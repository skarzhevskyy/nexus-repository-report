package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ProxySelector functionality.
 */
class ProxySelectorTest {

    private static final String TEST_URL = "https://nexus.example.com";

    @BeforeEach
    void setUp() {
        // Clear any existing system properties and environment to start fresh
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties after each test
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
    }

    @Test
    void selectProxy_withCommandLineArgument_shouldReturnProxyConfig() {
        String proxyArg = "proxy.example.com:8080";

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, proxyArg);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("proxy.example.com");
        assertThat(result.getPort()).isEqualTo(8080);
        assertThat(result.getUsername()).isNull();
        assertThat(result.getPassword()).isNull();
    }

    @Test
    void selectProxy_withHttpUrl_shouldReturnProxyConfig() {
        String proxyArg = "http://proxy.example.com:8080";

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, proxyArg);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("proxy.example.com");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    void selectProxy_withCredentials_shouldReturnProxyConfigWithAuth() {
        String proxyArg = "http://user:pass@proxy.example.com:8080";

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, proxyArg);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("proxy.example.com");
        assertThat(result.getPort()).isEqualTo(8080);
        assertThat(result.getUsername()).isEqualTo("user");
        assertThat(result.getPassword()).isEqualTo("pass");
    }

    @Test
    void selectProxy_withDefaultPort_shouldUse8080() {
        String proxyArg = "proxy.example.com";

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, proxyArg);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("proxy.example.com");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    void selectProxy_withSystemProperties_shouldReturnProxyConfig() {
        System.setProperty("http.proxyHost", "system.proxy.com");
        System.setProperty("http.proxyPort", "3128");

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, null);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("system.proxy.com");
        assertThat(result.getPort()).isEqualTo(3128);
    }

    @Test
    void selectProxy_withSystemPropertiesDefaultPort_shouldUse8080() {
        System.setProperty("http.proxyHost", "system.proxy.com");
        // No port specified

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, null);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("system.proxy.com");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    void selectProxy_commandLineArgTakesPrecedenceOverSystemProperties() {
        // Set system properties
        System.setProperty("http.proxyHost", "system.proxy.com");
        System.setProperty("http.proxyPort", "3128");

        // Command line should take precedence
        String proxyArg = "cli.proxy.com:8080";

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, proxyArg);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("cli.proxy.com");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    void selectProxy_withLocalhost_shouldReturnProxyConfig() {
        String proxyArg = "localhost:8080";

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, proxyArg);

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    void selectProxy_withInvalidProxyUrl_shouldReturnNull() {
        String invalidProxyArg = "invalid-url-format";

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, invalidProxyArg);

        assertThat(result).isNull();
    }

    @Test
    void selectProxy_withNullUrl_shouldThrowException() {
        assertThatThrownBy(() -> ProxySelector.selectProxy(null, "proxy.com:8080"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("URL cannot be null");
    }

    @Test
    void selectProxy_withEmptyProxyArg_shouldCheckSystemProperties() {
        System.setProperty("http.proxyHost", "system.proxy.com");
        System.setProperty("http.proxyPort", "3128");

        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, "");

        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("system.proxy.com");
        assertThat(result.getPort()).isEqualTo(3128);
    }

    @Test
    void selectProxy_withNoConfiguration_shouldReturnNull() {
        ProxySelector.ProxyConfig result = ProxySelector.selectProxy(TEST_URL, null);

        assertThat(result).isNull();
    }

    @Test
    void configureProxy_withNullWebClientBuilder_shouldThrowException() {
        ProxySelector.ProxyConfig proxyConfig = new ProxySelector.ProxyConfig("proxy.com", 8080, null, null);

        assertThatThrownBy(() -> ProxySelector.configureProxy(null, proxyConfig))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("WebClient builder cannot be null");
    }

    @Test
    void proxyConfig_toString_shouldContainHostAndPort() {
        ProxySelector.ProxyConfig config = new ProxySelector.ProxyConfig("proxy.com", 8080, "user", "pass");

        String result = config.toString();

        assertThat(result)
                .contains("proxy.com")
                .contains("8080")
                .contains("hasCredentials=true");
    }

    @Test
    void proxyConfig_toString_withoutCredentials_shouldIndicateNoCredentials() {
        ProxySelector.ProxyConfig config = new ProxySelector.ProxyConfig("proxy.com", 8080, null, null);

        String result = config.toString();

        assertThat(result)
                .contains("proxy.com")
                .contains("8080")
                .contains("hasCredentials=false");
    }
}
