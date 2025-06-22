package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Integration tests for command line arguments functionality.
 */
class CommandLineIntegrationTest {

    @Test
    void commandLineArgs_withSortOption_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        // Test parsing with sort option
        cmd.parseArgs("--url", "https://nexus.example.com", "--username", "user", "--password", "pass", "--sort", "size");

        assertThat(args.nexusServerUrl).isEqualTo("https://nexus.example.com");
        assertThat(args.nexusUsername).isEqualTo("user");
        assertThat(args.nexusPassword).isEqualTo("pass");
        assertThat(args.sortBy).isEqualTo(SortBy.SIZE);
    }

    @Test
    void commandLineArgs_withoutSortOption_shouldUseDefault() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        // Test parsing without sort option (should use default)
        cmd.parseArgs("--url", "https://nexus.example.com", "--username", "user", "--password", "pass");

        assertThat(args.sortBy).isEqualTo(SortBy.COMPONENTS); // Default value
    }

    @Test
    void sortBy_parsing_shouldWorkWithValidValues() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        // Test that picocli can parse enum values correctly (case sensitive)
        cmd.parseArgs("--url", "https://nexus.example.com", "--sort", "COMPONENTS");
        assertThat(args.sortBy).isEqualTo(SortBy.COMPONENTS);

        cmd.parseArgs("--url", "https://nexus.example.com", "--sort", "NAME");
        assertThat(args.sortBy).isEqualTo(SortBy.NAME);

        cmd.parseArgs("--url", "https://nexus.example.com", "--sort", "SIZE");
        assertThat(args.sortBy).isEqualTo(SortBy.SIZE);
    }

    @Test
    void commandLineArgs_withCreatedFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--created-before", "2024-06-01T00:00:00Z",
                "--created-after", "30d");

        assertThat(args.createdBefore).isEqualTo("2024-06-01T00:00:00Z");
        assertThat(args.createdAfter).isEqualTo("30d");
    }

    @Test
    void commandLineArgs_withUpdatedFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--updated-before", "2024-06-01",
                "--updated-after", "7d");

        assertThat(args.updatedBefore).isEqualTo("2024-06-01");
        assertThat(args.updatedAfter).isEqualTo("7d");
    }

    @Test
    void commandLineArgs_withDownloadedFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--downloaded-before", "2024-12-01T12:00:00Z",
                "--downloaded-after", "1d");

        assertThat(args.downloadedBefore).isEqualTo("2024-12-01T12:00:00Z");
        assertThat(args.downloadedAfter).isEqualTo("1d");
    }

    @Test
    void commandLineArgs_withNeverDownloadedFlag_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com", "--never-downloaded");

        assertThat(args.neverDownloaded).isTrue();
    }

    @Test
    void commandLineArgs_withAllFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--created-before", "2024-06-01T00:00:00Z",
                "--created-after", "30d",
                "--updated-before", "2024-05-01",
                "--updated-after", "7d",
                "--downloaded-before", "2024-04-01T12:00:00Z",
                "--downloaded-after", "1d");

        assertThat(args.createdBefore).isEqualTo("2024-06-01T00:00:00Z");
        assertThat(args.createdAfter).isEqualTo("30d");
        assertThat(args.updatedBefore).isEqualTo("2024-05-01");
        assertThat(args.updatedAfter).isEqualTo("7d");
        assertThat(args.downloadedBefore).isEqualTo("2024-04-01T12:00:00Z");
        assertThat(args.downloadedAfter).isEqualTo("1d");
        assertThat(args.neverDownloaded).isFalse();
    }

    @Test
    void commandLineArgs_withoutDateFilters_shouldHaveNullValues() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com");

        assertThat(args.createdBefore).isNull();
        assertThat(args.createdAfter).isNull();
        assertThat(args.updatedBefore).isNull();
        assertThat(args.updatedAfter).isNull();
        assertThat(args.downloadedBefore).isNull();
        assertThat(args.downloadedAfter).isNull();
        assertThat(args.neverDownloaded).isFalse();
        assertThat(args.repositories).isNull();
        assertThat(args.groups).isNull();
        assertThat(args.names).isNull();
    }

    @Test
    void commandLineArgs_withRepositoryFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--repository", "my-repo",
                "--repository", "other-repo");

        assertThat(args.repositories).containsExactly("my-repo", "other-repo");
    }

    @Test
    void commandLineArgs_withGroupFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--group", "com.example",
                "--group", "org.springframework.*");

        assertThat(args.groups).containsExactly("com.example", "org.springframework.*");
    }

    @Test
    void commandLineArgs_withNameFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--name", "spring-*",
                "--name", "junit");

        assertThat(args.names).containsExactly("spring-*", "junit");
    }

    @Test
    void commandLineArgs_withAllComponentFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--repository", "my-repo",
                "--repository", "test-repo",
                "--group", "com.example.*",
                "--name", "spring-*",
                "--name", "junit?");

        assertThat(args.repositories).containsExactly("my-repo", "test-repo");
        assertThat(args.groups).containsExactly("com.example.*");
        assertThat(args.names).containsExactly("spring-*", "junit?");
    }

    @Test
    void commandLineArgs_withMixedFilters_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com",
                "--repository", "my-repo",
                "--created-after", "30d",
                "--group", "com.example",
                "--never-downloaded",
                "--name", "spring-*");

        assertThat(args.repositories).containsExactly("my-repo");
        assertThat(args.groups).containsExactly("com.example");
        assertThat(args.names).containsExactly("spring-*");
        assertThat(args.createdAfter).isEqualTo("30d");
        assertThat(args.neverDownloaded).isTrue();
    }

    @Test
    void commandLineArgs_withTopGroupsOptions_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("top-groups", "--url", "https://nexus.example.com", "--username", "user", "--password", "pass",
                "--top-groups", "5", "--group-sort", "size");

        assertThat(args.report).isEqualTo("top-groups");
        assertThat(args.nexusServerUrl).isEqualTo("https://nexus.example.com");
        assertThat(args.topGroups).isEqualTo(5);
        assertThat(args.groupSort).isEqualTo(SortBy.SIZE);
    }

    @Test
    void commandLineArgs_withDefaultTopGroupsOptions_shouldUseDefaults() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("top-groups", "--url", "https://nexus.example.com", "--username", "user", "--password", "pass");

        assertThat(args.report).isEqualTo("top-groups");
        assertThat(args.topGroups).isEqualTo(10); // Default value
        assertThat(args.groupSort).isEqualTo(SortBy.COMPONENTS); // Default value
    }

    @Test
    void commandLineArgs_withGroupSortCaseInsensitive_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("top-groups", "--url", "https://nexus.example.com", "--username", "user", "--password", "pass",
                "--group-sort", "Components");

        assertThat(args.groupSort).isEqualTo(SortBy.COMPONENTS);
    }

    @Test
    void commandLineArgs_withAllReport_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("all", "--url", "https://nexus.example.com", "--username", "user", "--password", "pass");

        assertThat(args.report).isEqualTo("all");
    }

    @Test
    void commandLineArgs_withRepositoriesSummaryReport_shouldParseCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("repositories-summary", "--url", "https://nexus.example.com", "--username", "user", "--password", "pass");

        assertThat(args.report).isEqualTo("repositories-summary");
    }

    @Test
    void commandLineArgs_withDefaultReport_shouldUseAll() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);

        cmd.parseArgs("--url", "https://nexus.example.com", "--username", "user", "--password", "pass");

        assertThat(args.report).isEqualTo("all"); // Default value
    }
}
