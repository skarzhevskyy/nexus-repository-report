package com.pyx4j.nxrm.report;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.pyx4j.nxrm.report.model.ComponentsSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.ApiClient;
import org.sonatype.nexus.api.ComponentsApi;
import org.sonatype.nexus.api.RepositoryManagementApi;
import org.sonatype.nexus.model.AbstractApiRepository;
import org.sonatype.nexus.model.ComponentXO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public final class NxReport {

    private static final Logger log = LoggerFactory.getLogger(NxReport.class);

    private NxReport() {
        // Utility class should not be instantiated
    }

    private static ApiClient createApiClient(NxReportCommandArgs args) {
        Objects.requireNonNull(args, "Command arguments cannot be null");
        Objects.requireNonNull(args.nexusServerUrl, "Nexus server URL cannot be null");

        log.info("Initializing report generation for Nexus server: {}", args.nexusServerUrl);

        // There is no authentication configured in swagger, so apiClient.setUsername(args.nexusUsername) can't be used here
        String authorizationHeader;
        if (args.nexusToken != null && !args.nexusToken.isEmpty()) {
            authorizationHeader = "Bearer " + args.nexusToken;
        } else {
            authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((args.nexusUsername + ":" + args.nexusPassword).getBytes(StandardCharsets.UTF_8));
        }

        // Configure proxy settings
        ProxySelector.ProxyConfig proxyConfig = ProxySelector.selectProxy(args.nexusServerUrl, args.proxyUrl);

        WebClient webClient = ProxySelector.configureProxy(WebClient.builder(), proxyConfig)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        // Initialize API clients
        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(args.nexusServerUrl + "/service/rest");

        return apiClient;
    }

    public static int generateReport(NxReportCommandArgs args) {
        // Create our summary object to store results
        ComponentsSummary summary = new ComponentsSummary();

        // Use CountDownLatch to control flow in the main thread
        AtomicInteger resultCode = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        try {
            // Create component filter based on command line arguments
            var componentFilter = ComponentFilter.createFilter(args);

            ApiClient apiClient = createApiClient(args);
            RepositoryManagementApi repoApi = new RepositoryManagementApi(apiClient);

            // Build the reactive pipeline
            repoApi.getRepositories()
                    .doOnNext(repository -> log.debug("Found {} repository of type {}", repository.getName(), repository.getType()))
                    .filter(repository -> !repository.getType().equals(AbstractApiRepository.TypeEnum.GROUP)) // Exclude group repositories
                    .doOnNext(repository -> log.trace("Processing repository: {}", repository.getName()))
                    .flatMap(repository -> processRepositoryComponents(apiClient, repository, summary, componentFilter))
                    .collectList()
                    .doOnSuccess(allRepos -> {
                        // Output the summary with the specified sort option
                        NxReportConsole.printSummary(summary, args.sortBy);
                        resultCode.set(0);
                    })
                    .doOnError(ex -> {
                        log.error("Error generating report", ex);
                        resultCode.set(1);
                    })
                    .doFinally(signal -> latch.countDown())
                    .subscribe();

            // Wait for completion
            try {
                latch.await();
            } catch (InterruptedException e) {
                log.error("Report generation interrupted", e);
                Thread.currentThread().interrupt();
                return 1;
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid filter arguments: {}", e.getMessage());
            return 1;
        }

        return resultCode.get();
    }

    private static Mono<Void> processRepositoryComponents(ApiClient apiClient, AbstractApiRepository repository, ComponentsSummary summary, Predicate<ComponentXO> componentFilter) {
        ComponentsApi componentsApi = new ComponentsApi(apiClient);
        return processPaginatedComponents(componentsApi, repository, null, summary, componentFilter);
    }

    private static Mono<Void> processPaginatedComponents(ComponentsApi componentsApi, AbstractApiRepository repository, String continuationToken, ComponentsSummary summary, Predicate<ComponentXO> componentFilter) {
        final String repoName = Objects.requireNonNull(repository.getName(), "Repository name cannot be null");
        log.debug("Fetching components page for repository {} with token: {}", repoName, continuationToken);

        return componentsApi.getComponents(repoName, continuationToken)
                .flatMap(page -> {
                    if (page != null && page.getItems() != null) {
                        // Apply filter to components
                        List<ComponentXO> filteredComponents = page.getItems().stream()
                                .filter(componentFilter)
                                .toList();

                        long componentCount = filteredComponents.size();
                        long sizeBytes = calculateTotalSize(filteredComponents);

                        log.debug("Repository {} page has {} components (filtered from {}) with total size of {} bytes",
                                repoName, componentCount, page.getItems().size(), sizeBytes);

                        if (componentCount > 0) {
                            summary.addRepositoryStats(repoName, repository.getFormat(), componentCount, sizeBytes);
                        }

                        // If we have a continuation token, process next page
                        String nextContinuationToken = page.getContinuationToken();
                        if (nextContinuationToken != null && !nextContinuationToken.isEmpty()) {
                            return processPaginatedComponents(componentsApi, repository, nextContinuationToken, summary, componentFilter);
                        }
                    } else {
                        log.debug("Repository {} page has no components", repoName);
                    }

                    return Mono.empty();
                });
    }

    /**
     * Calculates the total size of all components in bytes.
     *
     * @param components List of components to calculate size for
     * @return Total size in bytes
     */
    private static long calculateTotalSize(List<ComponentXO> components) {
        if (components == null || components.isEmpty()) {
            return 0;
        }

        return components.stream()
                .mapToLong(component -> {
                    // If a component has a size property, use it - otherwise default to 0
                    if (component.getAssets() != null) {
                        return component.getAssets().stream()
                                .filter(asset -> asset.getFileSize() != null)
                                .mapToLong(asset -> asset.getFileSize() != null ? asset.getFileSize() : 0)
                                .sum();
                    }
                    return 0;
                })
                .sum();
    }

}
