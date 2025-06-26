package com.pyx4j.nxrm.report;

import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(description = "nxrm-report", name = "nxrm-report.jar", sortOptions = false, mixinStandardHelpOptions = true)
public class NxReportCommandArgs implements Callable<Integer> {

    @CommandLine.Parameters(index = "0",
            description = "Report type: all, repositories-summary, top-groups, age-report",
            defaultValue = "all")
    public String report;

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
            names = {"--repo-sort"},
            description = "Sort repositories by: ${COMPLETION-CANDIDATES} (default: components)",
            converter = SortBy.CaseInsensitiveEnumConverter.class)
    public SortBy repositoriesSortBy = SortBy.COMPONENTS;

    @CommandLine.Option(
            names = {"--top-groups"},
            description = "Show only the top N groups (default: 10)")
    public int topGroups = 10;

    @CommandLine.Option(
            names = {"--group-sort"},
            description = "Sort groups by: ${COMPLETION-CANDIDATES} (default: components)",
            converter = SortBy.CaseInsensitiveEnumConverter.class)
    public SortBy groupSort = SortBy.COMPONENTS;

    @CommandLine.Option(
            names = {"--age-buckets"},
            description = "Age bucket ranges for age report (default: '0-7,8-30,31-90,91-365,>365')")
    public String ageBuckets = "0-7,8-30,31-90,91-365,>365";


    @CommandLine.Option(
            names = {"--created-before"},
            description = "Filter components created before this date (ISO-8601 format or 'Nd' for N days ago)")
    public String createdBefore;

    @CommandLine.Option(
            names = {"--created-after"},
            description = "Filter components created after this date (ISO-8601 format or 'Nd' for N days ago)")
    public String createdAfter;

    @CommandLine.Option(
            names = {"--updated-before"},
            description = "Filter components updated before this date (ISO-8601 format or 'Nd' for N days ago)")
    public String updatedBefore;

    @CommandLine.Option(
            names = {"--updated-after"},
            description = "Filter components updated after this date (ISO-8601 format or 'Nd' for N days ago)")
    public String updatedAfter;

    @CommandLine.Option(
            names = {"--downloaded-before"},
            description = "Filter components downloaded before this date (ISO-8601 format or 'Nd' for N days ago)")
    public String downloadedBefore;

    @CommandLine.Option(
            names = {"--downloaded-after"},
            description = "Filter components downloaded after this date (ISO-8601 format or 'Nd' for N days ago)")
    public String downloadedAfter;

    @CommandLine.Option(
            names = {"--never-downloaded"},
            description = "Only include components that have never been downloaded")
    public boolean neverDownloaded;

    @CommandLine.Option(
            names = {"--repository"},
            description = "Filter components by repository name (supports wildcards *, ?). Can be specified multiple times (OR logic)")
    public List<String> repositories;

    @CommandLine.Option(
            names = {"--group"},
            description = "Filter components by group (supports wildcards *, ?). Can be specified multiple times (OR logic)")
    public List<String> groups;

    @CommandLine.Option(
            names = {"--name"},
            description = "Filter components by name (supports wildcards *, ?). Can be specified multiple times (OR logic)")
    public List<String> names;


    public Integer call() throws Exception {
        int exitCode = 0;
        switch (report) {
            case "all":
            case "repositories-summary":
            case "top-groups":
            case "age-report":
                exitCode = NxReport.generateReport(this);
                break;
            default:
                CommandLine.usage(this, System.out);
                break;
        }
        return exitCode;
    }

}
