package com.pyx4j.nxrm.report;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import com.google.common.base.Strings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

/**
 * Utility class for selecting and configuring proxy settings based on multiple sources.
 * Supports command line arguments, Java system properties, and environment variables.
 */
public final class ProxySelector {

    private static final Logger log = LoggerFactory.getLogger(ProxySelector.class);

    private ProxySelector() {
        // Utility class should not be instantiated
    }

    /**
     * Selects proxy configuration for the given URL based on priority order:
     * 1. Command line argument
     * 2. Java System Properties
     * 3. Capitalized environment variables
     * 4. Lowercase environment variables
     *
     * @param url           The target URL to determine proxy for
     * @param proxyArgument Command line proxy argument (nullable)
     * @return ProxyConfig object or null if no proxy should be used
     */
    @Nullable
    public static ProxyConfig selectProxy(String url, @Nullable String proxyArgument) {
        Objects.requireNonNull(url, "URL cannot be null");

        log.trace("Selecting proxy for URL: {}", url);

        // 1. Command line argument has highest priority
        if (!Strings.isNullOrEmpty(proxyArgument)) {
            log.debug("Using proxy from command line argument: {}", proxyArgument);
            return parseProxyUrl(proxyArgument);
        }

        // 2. Java System Properties
        ProxyConfig systemProxyConfig = getSystemProxyConfig(url);
        if (systemProxyConfig != null) {
            log.debug("Using proxy from Java system properties: {}:{}", systemProxyConfig.getHost(), systemProxyConfig.getPort());
            return systemProxyConfig;
        }

        // 3. Capitalized environment variables
        ProxyConfig envProxyConfig = getEnvironmentProxyConfig(url, true);
        if (envProxyConfig != null) {
            log.debug("Using proxy from capitalized environment variables: {}:{}", envProxyConfig.getHost(), envProxyConfig.getPort());
            return envProxyConfig;
        }

        // 4. Lowercase environment variables
        envProxyConfig = getEnvironmentProxyConfig(url, false);
        if (envProxyConfig != null) {
            log.debug("Using proxy from lowercase environment variables: {}:{}", envProxyConfig.getHost(), envProxyConfig.getPort());
            return envProxyConfig;
        }

        log.trace("No proxy configuration found for URL: {}", url);
        return null;
    }

    /**
     * Configures WebClient builder with proxy settings if available.
     *
     * @param webClientBuilder WebClient builder to configure
     * @param proxyConfig      Proxy configuration (nullable)
     * @return The configured WebClient builder
     */
    public static WebClient.Builder configureProxy(WebClient.Builder webClientBuilder, @Nullable ProxyConfig proxyConfig) {
        Objects.requireNonNull(webClientBuilder, "WebClient builder cannot be null");

        if (proxyConfig == null) {
            log.trace("No proxy configuration provided, using direct connection");
            return webClientBuilder;
        }

        log.info("Configuring HTTP client with proxy: {}:{}", proxyConfig.getHost(), proxyConfig.getPort());

        HttpClient httpClient = HttpClient.create()
                .proxy(proxy -> {
                    ProxyProvider.Builder proxyBuilder = proxy.type(ProxyProvider.Proxy.HTTP)
                            .host(proxyConfig.getHost())
                            .port(proxyConfig.getPort());

                    if (proxyConfig.getUsername() != null && proxyConfig.getPassword() != null) {
                        proxyBuilder.username(proxyConfig.getUsername())
                                .password(unused -> proxyConfig.getPassword());
                    }
                });

        return webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Nullable
    private static ProxyConfig parseProxyUrl(String proxyUrl) {
        try {
            // Handle URLs without protocol
            String normalizedUrl = proxyUrl;
            if (!proxyUrl.startsWith("http://") && !proxyUrl.startsWith("https://")) {
                // For host:port format, validate that it contains a colon or is a valid hostname
                if (!proxyUrl.contains(":") && !isValidHostname(proxyUrl)) {
                    log.warn("Invalid proxy URL format: {}", proxyUrl);
                    return null;
                }
                normalizedUrl = "http://" + proxyUrl;
            }

            URI uri = new URI(normalizedUrl);
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null) {
                log.warn("Invalid proxy URL format: {}", proxyUrl);
                return null;
            }

            // Use default ports if not specified
            if (port == -1) {
                port = "https".equals(uri.getScheme()) ? 443 : 8080;
            }

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] credentials = userInfo.split(":");
                username = credentials[0];
                if (credentials.length > 1) {
                    password = credentials[1];
                }
            }

            return new ProxyConfig(host, port, username, password);

        } catch (URISyntaxException e) {
            log.warn("Failed to parse proxy URL: {}", proxyUrl, e);
            return null;
        }
    }

    private static boolean isValidHostname(String hostname) {
        // Basic hostname validation - contains at least one dot for domain names
        // or could be 'localhost' or similar
        return hostname.contains(".") || "localhost".equals(hostname);
    }

    @Nullable
    private static ProxyConfig getSystemProxyConfig(String url) {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPortStr = System.getProperty("http.proxyPort");
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");

        if (Strings.isNullOrEmpty(proxyHost)) {
            return null;
        }

        // Check if URL should bypass proxy
        if (!Strings.isNullOrEmpty(nonProxyHosts) && shouldBypassProxy(url, nonProxyHosts)) {
            log.trace("URL {} matches non-proxy hosts pattern: {}", url, nonProxyHosts);
            return null;
        }

        int proxyPort = 8080; // Default port
        if (!Strings.isNullOrEmpty(proxyPortStr)) {
            try {
                proxyPort = Integer.parseInt(proxyPortStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid proxy port in system property: {}", proxyPortStr);
            }
        }

        return new ProxyConfig(proxyHost, proxyPort, null, null);
    }

    @Nullable
    private static ProxyConfig getEnvironmentProxyConfig(String url, boolean capitalized) {
        String httpProxy = capitalized ? System.getenv("HTTP_PROXY") : System.getenv("http_proxy");
        String httpsProxy = capitalized ? System.getenv("HTTPS_PROXY") : System.getenv("https_proxy");

        String proxyEnvVar = null;
        if (url.startsWith("https://") && !Strings.isNullOrEmpty(httpsProxy)) {
            proxyEnvVar = httpsProxy;
        } else if (!Strings.isNullOrEmpty(httpProxy)) {
            proxyEnvVar = httpProxy;
        }

        if (Strings.isNullOrEmpty(proxyEnvVar)) {
            return null;
        }

        return parseProxyUrl(proxyEnvVar);
    }

    private static boolean shouldBypassProxy(String url, String nonProxyHosts) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }

            String[] patterns = nonProxyHosts.split("\\|");
            for (String pattern : patterns) {
                pattern = pattern.trim();
                // Convert wildcard pattern to regex
                String regex = pattern.replace(".", "\\.").replace("*", ".*");
                if (host.matches(regex)) {
                    return true;
                }
            }
        } catch (URISyntaxException e) {
            log.warn("Failed to parse URL for proxy bypass check: {}", url, e);
        }

        return false;
    }

    /**
     * Represents proxy configuration with host, port, and optional credentials.
     */
    public static class ProxyConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;

        public ProxyConfig(String host, int port, @Nullable String username, @Nullable String password) {
            this.host = Objects.requireNonNull(host, "Proxy host cannot be null");
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Nullable
        public String getUsername() {
            return username;
        }

        @Nullable
        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            return "ProxyConfig{host='" + host + "', port=" + port + ", hasCredentials=" + (username != null) + "}";
        }
    }
}
