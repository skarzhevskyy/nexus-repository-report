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
        cmd.parseArgs("--url", "https://nexus.example.com", "--username", "user", "--password", "pass", "--sort", "SIZE");
        
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
}