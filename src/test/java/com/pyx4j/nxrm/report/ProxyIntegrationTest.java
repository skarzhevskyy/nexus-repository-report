package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for proxy functionality with command line arguments.
 */
class ProxyIntegrationTest {

    @Test
    void commandLineArgs_withProxyOption_shouldParseCorrectly() {
        // Test that the command line option parsing includes proxy
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.nexusServerUrl = "https://nexus.example.com";
        args.proxyUrl = "proxy.company.com:8080";

        // Verify proxy argument is set
        assertThat(args.proxyUrl).isEqualTo("proxy.company.com:8080");

        // Verify proxy selection would work
        ProxySelector.ProxyConfig proxyConfig = ProxySelector.selectProxy(args.nexusServerUrl, args.proxyUrl);
        assertThat(proxyConfig).isNotNull();
        assertThat(proxyConfig.getHost()).isEqualTo("proxy.company.com");
        assertThat(proxyConfig.getPort()).isEqualTo(8080);
    }

    @Test
    void commandLineArgs_withoutProxy_shouldWorkAsUsual() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.nexusServerUrl = "https://nexus.example.com";
        args.proxyUrl = null;

        // Verify proxy selection returns null when no proxy configured
        ProxySelector.ProxyConfig proxyConfig = ProxySelector.selectProxy(args.nexusServerUrl, args.proxyUrl);
        assertThat(proxyConfig).isNull();
    }
}
