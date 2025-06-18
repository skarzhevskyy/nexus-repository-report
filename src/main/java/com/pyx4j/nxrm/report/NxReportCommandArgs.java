package com.pyx4j.nxrm.report;

import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(description = "nxrm-report", name = "nxrm-report.jar", sortOptions = false, mixinStandardHelpOptions = true)
public class NxReportCommandArgs implements Callable<Integer> {

    @CommandLine.Parameters(index = "0",
            description = "repositories-summary",
            defaultValue = "repositories-summary")
    public String action;

    @CommandLine.Option(
            names = {"--url"},
            description = "Nexus Repository Manager URL", required = true,
            defaultValue = "${NEXUS_URL}")
    public String nexusServerUrl;

    @CommandLine.Option(
            names = {"--username"},
            description = "Nexus Repository Manager username",
            defaultValue = "${NEXUS_USERNAME}")
    public String nexusUsername;

    @CommandLine.Option(
            names = {"--password"},
            description = "Nexus Repository Manager password",
            defaultValue = "${NEXUS_PASSWORD}")
    public String nexusPassword;

    @CommandLine.Option(
            names = {"--token"},
            description = "Nexus Repository Manager Token",
            defaultValue = "${NEXUS_TOKEN}")
    public String nexusToken;

    @CommandLine.Option(
            names = {"--proxy"},
            description = "Proxy server URL (e.g., proxy.example.com:8081 or http://proxy.example.com:8081)")
    public String proxyUrl;

    @CommandLine.Option(
            names = {"--sort"},
            description = "Sort repositories by: ${COMPLETION-CANDIDATES} (default: components)")
    public SortBy sortBy = SortBy.COMPONENTS;

    public Integer call() throws Exception {
        int exitCode = 0;
        switch (action) {
            case "repositories-summary":
                exitCode = NxReport.generateReport(this);
                break;
            default:
                CommandLine.usage(this, System.out);
                break;
        }
        return exitCode;
    }

}
