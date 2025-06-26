package com.pyx4j.nxrm.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.pyx4j.nxrm.report.model.AgeSummary;
import com.pyx4j.nxrm.report.model.GroupsSummary;
import com.pyx4j.nxrm.report.model.RepositoryComponentsSummary;
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
        // Create component filter based on command line arguments
        var componentFilter = ComponentFilter.createFilter(args);

        // Create our summary objects based on report type
        RepositoryComponentsSummary repositoryComponentsSummary = new RepositoryComponentsSummary();
        GroupsSummary groupsSummary = new GroupsSummary();

        // Parse age buckets and create AgeSummary
        List<String> ageBucketRanges = Arrays.asList(args.ageBuckets.split(","));
        AgeSummary ageSummary = new AgeSummary(ageBucketRanges);

        repositoryComponentsSummary.setEnabled("all".equals(args.report) || "repositories-summary".equals(args.report));
        groupsSummary.setEnabled("all".equals(args.report) || "top-groups".equals(args.report));
        ageSummary.setEnabled("all".equals(args.report) || "age-report".equals(args.report));

        // Use CountDownLatch to control flow in the main thread
        AtomicInteger resultCode = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<ComponentXO>> filteredComponents = new AtomicReference<>(new ArrayList<>());

        ApiClient apiClient = createApiClient(args);
        RepositoryManagementApi repoApi = new RepositoryManagementApi(apiClient);

        // Build the reactive pipeline
        repoApi.getRepositories()
                .doOnNext(repository -> log.debug("Found {} repository of type {}", repository.getName(), repository.getType()))
                .filter(repository -> !repository.getType().equals(AbstractApiRepository.TypeEnum.GROUP)) // Exclude group repositories
                .filter(repository -> ComponentFilter.matchesRepositoryFilter(repository.getName(), args.repositories)) // Filter repositories early
                .doOnNext(repository -> log.trace("Processing repository: {}", repository.getName()))
                .flatMap(repository -> processRepositoryComponents(apiClient, repository, repositoryComponentsSummary, groupsSummary, ageSummary, componentFilter, filteredComponents.get()))
                .collectList()
                .doOnSuccess(allRepos -> {
                    try (ReportWriter reportWriter = ReportWriterFactory.create(args.outputFile);
                         ReportWriter componentWriter = ReportWriterFactory.create(args.outputComponentFile)) {

                        if (reportWriter != null) {
                            if (repositoryComponentsSummary.isEnabled()) {
                                reportWriter.writeRepositoryComponentsSummary(repositoryComponentsSummary, args.repositoriesSortBy);
                            }
                            if (groupsSummary.isEnabled()) {
                                reportWriter.writeGroupsSummary(groupsSummary, args.groupSort, args.topGroups);
                            }
                            if (ageSummary.isEnabled()) {
                                reportWriter.writeAgeSummary(ageSummary);
                            }
                        } else {
                            boolean hasPreviousOutput = false;
                            if (repositoryComponentsSummary.isEnabled()) {
                                NxReportConsole.printSummary(repositoryComponentsSummary, args.repositoriesSortBy);
                                hasPreviousOutput = true;
                            }
                            if (groupsSummary.isEnabled()) {
                                if (hasPreviousOutput) {
                                    System.out.println(); // Add blank line between reports
                                }
                                NxReportConsole.printGroupsSummary(groupsSummary, args.groupSort, args.topGroups);
                                hasPreviousOutput = true;
                            }
                            if (ageSummary.isEnabled()) {
                                if (hasPreviousOutput) {
                                    System.out.println(); // Add blank line between reports
                                }
                                NxReportConsole.printAgeSummary(ageSummary);
                            }
                        }

                        if (componentWriter != null) {
                            componentWriter.writeComponents(filteredComponents.get());
                        }

                        resultCode.set(0);
                    } catch (IOException e) {
                        log.error("Error writing report file", e);
                        resultCode.set(1);
                    }
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

        return resultCode.get();
    }


    private static Mono<Void> processRepositoryComponents(ApiClient apiClient, AbstractApiRepository repository, RepositoryComponentsSummary repositoryComponentsSummary, GroupsSummary groupsSummary, AgeSummary ageSummary, Predicate<ComponentXO> componentFilter, List<ComponentXO> filteredComponents) {
        ComponentsApi componentsApi = new ComponentsApi(apiClient);
        return processPaginatedComponents(componentsApi, repository, null, repositoryComponentsSummary, groupsSummary, ageSummary, componentFilter, filteredComponents);
    }


    private static Mono<Void> processPaginatedComponents(ComponentsApi componentsApi, AbstractApiRepository repository, String continuationToken, RepositoryComponentsSummary repositoryComponentsSummary, GroupsSummary groupsSummary, AgeSummary ageSummary, Predicate<ComponentXO> componentFilter, List<ComponentXO> allFilteredComponents) {
        final String repoName = Objects.requireNonNull(repository.getName(), "Repository name cannot be null");
        log.debug("Fetching components page for repository {} with token: {}", repoName, continuationToken);

        return componentsApi.getComponents(repoName, continuationToken)
                .flatMap(page -> {
                    if (page != null && page.getItems() != null) {
                        // Apply filter to components
                        List<ComponentXO> filteredComponents = page.getItems().stream()
                                .filter(componentFilter)
                                .toList();

                        allFilteredComponents.addAll(filteredComponents);

                        long componentCount = filteredComponents.size();
                        long sizeBytes = calculateTotalSize(filteredComponents);

                        log.debug("Repository {} page has {} components (filtered from {}) with total size of {} bytes",
                                repoName, componentCount, page.getItems().size(), sizeBytes);

                        if (componentCount > 0) {
                            // Update repository summary if provided
                            if (repositoryComponentsSummary.isEnabled()) {
                                repositoryComponentsSummary.addRepositoryStats(repoName, repository.getFormat(), componentCount, sizeBytes);
                            }

                            // Update groups summary if provided
                            if (groupsSummary.isEnabled()) {
                                filteredComponents.stream()
                                        .filter(component -> component.getGroup() != null) // Only include components with a group
                                        .forEach(component -> {
                                            String groupName = component.getGroup();
                                            long componentSize = calculateComponentSize(component);
                                            groupsSummary.addGroupStats(groupName, 1, componentSize);
                                        });
                            }

                            // Update age summary if provided
                            if (ageSummary.isEnabled()) {
                                filteredComponents.forEach(component -> {
                                    long componentSize = calculateComponentSize(component);
                                    ageSummary.addComponent(component, componentSize);
                                });
                            }
                        }

                        // If we have a continuation token, process next page
                        String nextContinuationToken = page.getContinuationToken();
                        if (nextContinuationToken != null && !nextContinuationToken.isEmpty()) {
                            return processPaginatedComponents(componentsApi, repository, nextContinuationToken, repositoryComponentsSummary, groupsSummary, ageSummary, componentFilter, allFilteredComponents);
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
                .mapToLong(NxReport::calculateComponentSize)
                .sum();
    }

    /**
     * Calculates the total size of a single component in bytes.
     *
     * @param component Component to calculate size for
     * @return Total size in bytes
     */
    private static long calculateComponentSize(ComponentXO component) {
        if (component == null || component.getAssets() == null) {
            return 0;
        }

        return component.getAssets().stream()
                .filter(asset -> asset.getFileSize() != null)
                .mapToLong(asset -> asset.getFileSize() != null ? asset.getFileSize() : 0)
                .sum();
    }

}
