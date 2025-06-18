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
        assertThat(args.sortBy).isEqualTo("size");
    }

    @Test
    void commandLineArgs_withoutSortOption_shouldUseDefault() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        CommandLine cmd = new CommandLine(args);
        
        // Test parsing without sort option (should use default)
        cmd.parseArgs("--url", "https://nexus.example.com", "--username", "user", "--password", "pass");
        
        assertThat(args.sortBy).isEqualTo("components"); // Default value
    }

    @Test
    void sortBy_parsing_shouldWorkWithValidValues() {
        // Test that SortBy can parse the command line values correctly
        assertThat(SortBy.fromString("components")).isEqualTo(SortBy.COMPONENTS);
        assertThat(SortBy.fromString("name")).isEqualTo(SortBy.NAME);
        assertThat(SortBy.fromString("size")).isEqualTo(SortBy.SIZE);
    }
}