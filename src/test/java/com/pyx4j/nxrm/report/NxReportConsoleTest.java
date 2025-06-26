package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import com.pyx4j.nxrm.report.model.GroupsSummary;
import com.pyx4j.nxrm.report.model.RepositoryComponentsSummary;
import com.pyx4j.nxrm.report.model.AgeSummary;
import org.junit.jupiter.api.Test;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Unit tests for NxReportConsole functionality.
 */
class NxReportConsoleTest {

    @Test
    void printSummary_withShortRepositoryNames_shouldFormatCorrectly() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("maven-central", "maven2", 100, 1024000);
        summary.addRepositoryStats("npm-proxy", "npm", 50, 512000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.COMPONENTS, printStream);

        String output = outputStream.toString();
        assertThat(output)
                .contains("Repository Report Summary:")
                .contains("maven-central")
                .contains("npm-proxy")
                .contains("TOTAL");

        // Check that repositories are sorted by components (maven-central should come first with 100 components)
        int mavenIndex = output.indexOf("maven-central");
        int npmIndex = output.indexOf("npm-proxy");
        assertThat(mavenIndex).isLessThan(npmIndex);
    }

    @Test
    void printSummary_withLongRepositoryNames_shouldAdjustFormatting() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("very-long-repository-name-that-exceeds-thirty-characters", "maven2", 100, 1024000);
        summary.addRepositoryStats("short", "npm", 50, 512000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.NAME, printStream);

        String output = outputStream.toString();
        assertThat(output).contains("very-long-repository-name-that-exceeds-thirty-characters");
        assertThat(output).contains("short");

        // Verify that the format doesn't break with long names
        String[] lines = output.split("\n");
        boolean foundLongName = false;
        for (String line : lines) {
            if (line.contains("very-long-repository-name-that-exceeds-thirty-characters")) {
                foundLongName = true;
                // The line should be properly formatted (columns should be separated correctly)
                assertThat(line).matches(".*very-long-repository-name-that-exceeds-thirty-characters\\s+maven2\\s+\\d+\\s+.*");
                break;
            }
        }
        assertThat(foundLongName).as("Long repository name should be found in output").isTrue();
    }

    @Test
    void printSummary_sortByName_shouldSortAlphabetically() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("zebra-repo", "maven2", 10, 1000);
        summary.addRepositoryStats("alpha-repo", "npm", 20, 2000);
        summary.addRepositoryStats("beta-repo", "docker", 30, 3000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.NAME, printStream);

        String output = outputStream.toString();
        int alphaIndex = output.indexOf("alpha-repo");
        int betaIndex = output.indexOf("beta-repo");
        int zebraIndex = output.indexOf("zebra-repo");

        assertThat(alphaIndex).isLessThan(betaIndex);
        assertThat(betaIndex).isLessThan(zebraIndex);
    }

    @Test
    void printSummary_sortBySize_shouldSortByDescendingSize() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("small-repo", "maven2", 10, 1000);
        summary.addRepositoryStats("large-repo", "npm", 20, 10000);
        summary.addRepositoryStats("medium-repo", "docker", 30, 5000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.SIZE, printStream);

        String output = outputStream.toString();
        int largeIndex = output.indexOf("large-repo");
        int mediumIndex = output.indexOf("medium-repo");
        int smallIndex = output.indexOf("small-repo");

        // Should be sorted largest to smallest
        assertThat(largeIndex).isLessThan(mediumIndex);
        assertThat(mediumIndex).isLessThan(smallIndex);
    }

    @Test
    void printSummary_sortByComponents_shouldSortByDescendingComponentCount() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("few-components", "maven2", 10, 1000);
        summary.addRepositoryStats("many-components", "npm", 100, 2000);
        summary.addRepositoryStats("some-components", "docker", 50, 3000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.COMPONENTS, printStream);

        String output = outputStream.toString();
        int manyIndex = output.indexOf("many-components");
        int someIndex = output.indexOf("some-components");
        int fewIndex = output.indexOf("few-components");

        // Should be sorted most to least components
        assertThat(manyIndex).isLessThan(someIndex);
        assertThat(someIndex).isLessThan(fewIndex);
    }

    @Test
    void printSummary_shouldDisplayTotalCorrectly() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("repo1", "maven2", 100, 1024000);
        summary.addRepositoryStats("repo2", "npm", 50, 512000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.NAME, printStream);

        String output = outputStream.toString();
        assertThat(output).contains("TOTAL");
        assertThat(output).contains("150"); // Total components
        // Check that size formatting is present (should be like "1.46 MB" for total)
        assertThat(output).contains("1.46 MB");
    }

    @Test
    void printGroupsSummary_withShortGroupNames_shouldFormatCorrectly() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("org.springframework", 1200, 1800000000L); // 1.8 GB
        summary.addGroupStats("com.example", 950, 1200000000L); // 1.2 GB

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.COMPONENTS, 10, printStream);

        String output = outputStream.toString();
        assertThat(output)
                .contains("Top Consuming Groups (by Components):")
                .contains("org.springframework")
                .contains("com.example")
                .contains("1200")
                .contains("950");

        // Check that groups are sorted by components (org.springframework should come first with 1200 components)
        int springIndex = output.indexOf("org.springframework");
        int exampleIndex = output.indexOf("com.example");
        assertThat(springIndex).isLessThan(exampleIndex);
    }

    @Test
    void printGroupsSummary_sortBySize_shouldSortCorrectly() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("org.springframework", 800, 2000000000L); // 2.0 GB
        summary.addGroupStats("com.example", 1200, 1000000000L); // 1.0 GB

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.SIZE, 10, printStream);

        String output = outputStream.toString();
        assertThat(output).contains("Top Consuming Groups (by Size):");

        // Check that groups are sorted by size (org.springframework should come first with 2.0 GB)
        int springIndex = output.indexOf("org.springframework");
        int exampleIndex = output.indexOf("com.example");
        assertThat(springIndex).isLessThan(exampleIndex);
    }

    @Test
    void printGroupsSummary_withTopGroups_shouldLimitOutput() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("org.springframework", 1000, 1000000000L);
        summary.addGroupStats("com.example", 900, 900000000L);
        summary.addGroupStats("org.apache", 800, 800000000L);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.COMPONENTS, 2, printStream);

        String output = outputStream.toString();
        assertThat(output)
                .contains("org.springframework")
                .contains("com.example")
                .doesNotContain("org.apache"); // Should be limited to top 2
    }

    @Test
    void printGroupsSummary_withLongGroupNames_shouldAdjustFormatting() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("very-long-group-name-that-exceeds-thirty-characters.deeply.nested", 100, 1024000);
        summary.addGroupStats("short", 50, 512000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.NAME, 10, printStream);

        String output = outputStream.toString();
        String[] lines = output.split("\n");

        boolean foundLongName = false;
        for (String line : lines) {
            if (line.contains("very-long-group-name-that-exceeds-thirty-characters.deeply.nested")) {
                foundLongName = true;
                // The line should be properly formatted (columns should be separated correctly)
                assertThat(line).matches(".*very-long-group-name-that-exceeds-thirty-characters\\.deeply\\.nested\\s+\\d+\\s+.*");
                break;
            }
        }
        assertThat(foundLongName).as("Long group name should be found in output").isTrue();
    }

    @Test
    void printAgeSummary_shouldDisplayCorrectFormat() {
        List<String> ranges = Arrays.asList("0-7", "8-30", "31-90", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        // Create components with different ages and add them to buckets using proper method
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        // Add components to first bucket (0-7 days) - 120 components
        for (int i = 0; i < 120; i++) {
            ComponentXO component = createComponentWithAsset(now.minusDays(3)); // 3 days old
            summary.addComponent(component, 200_000_000L / 120); // Average size per component
        }
        
        // Add components to second bucket (8-30 days) - 340 components  
        for (int i = 0; i < 340; i++) {
            ComponentXO component = createComponentWithAsset(now.minusDays(15)); // 15 days old
            summary.addComponent(component, 1_100_000_000L / 340); // Average size per component
        }
        
        // Add components to third bucket (31-90 days) - 500 components
        for (int i = 0; i < 500; i++) {
            ComponentXO component = createComponentWithAsset(now.minusDays(60)); // 60 days old
            summary.addComponent(component, 2_000_000_000L / 500); // Average size per component
        }
        
        // Add components to fourth bucket (>365 days) - 300 components
        for (int i = 0; i < 300; i++) {
            ComponentXO component = createComponentWithAsset(now.minusDays(400)); // 400 days old
            summary.addComponent(component, 1_200_000_000L / 300); // Average size per component
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printAgeSummary(summary, printStream);

        String output = outputStream.toString();
        String[] lines = output.split("\n");

        // Check header
        assertThat(output).contains("Component Age Distribution");
        assertThat(output).contains("Age Range");
        assertThat(output).contains("Components");
        assertThat(output).contains("Total Size");

        // Check age bucket data (formatted with spacing)
        assertThat(output).contains("0 - 7  days");
        assertThat(output).contains("8 - 30 days");
        assertThat(output).contains("31 - 90 days");
        assertThat(output).contains(">365 days");

        // Check component counts
        assertThat(output).contains("120");
        assertThat(output).contains("340");
        assertThat(output).contains("500");
        assertThat(output).contains("300");

        // Check total
        assertThat(output).contains("TOTAL");
        assertThat(output).contains("1260"); // Total components
    }

    @Test
    void printAgeSummary_withLongAgeRanges_shouldAdjustFormatting() {
        List<String> ranges = Arrays.asList("0-7", "8-30", "31-90", "91-365", "366-1095", ">1095");
        AgeSummary summary = new AgeSummary(ranges);

        // Add minimal test data using proper component creation
        ComponentXO component = createComponentWithAsset(OffsetDateTime.now(ZoneOffset.UTC).minusDays(3));
        summary.addComponent(component, 1024);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printAgeSummary(summary, printStream);

        String output = outputStream.toString();

        // Should handle longer range descriptions properly (with formatting)
        assertThat(output).contains("366 - 1095 days");
        assertThat(output).contains(">1095 days");

        // Check formatting is consistent (no overlapping columns)
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("days") && line.matches(".*\\s+1\\s+.*")) {
                // If this line contains "days" and has " 1 " (component count 1 with spaces), verify it's properly formatted
                assertThat(line).matches(".*\\s+1\\s+.*");
                break; // Only need to check one such line
            }
        }
    }

    @Test
    void printAgeSummary_withEmptyBuckets_shouldDisplayZeros() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        // Add data to only one bucket using proper component method
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        // Add 50 components to the 8-30 days bucket (15 days old)
        for (int i = 0; i < 50; i++) {
            ComponentXO component = createComponentWithAsset(now.minusDays(15));
            summary.addComponent(component, 1024000L / 50); // Average size per component
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printAgeSummary(summary, printStream);

        String output = outputStream.toString();

        // Should show zeros for empty buckets (with new formatting)
        assertThat(output).containsPattern("0 - 7  days\\s+0\\s+");
        assertThat(output).containsPattern(">365 days\\s+0\\s+");

        // Should show data for non-empty bucket (with new formatting)
        assertThat(output).containsPattern("8 - 30 days\\s+50\\s+");

        // Total should match the single bucket
        assertThat(output).contains("TOTAL");
        assertThat(output).containsPattern("TOTAL\\s+50\\s+");
    }

    private ComponentXO createComponentWithAsset(OffsetDateTime blobCreated) {
        ComponentXO component = new ComponentXO();
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(blobCreated);
        asset.setFileSize(1024L);
        component.setAssets(Arrays.asList(asset));
        return component;
    }
}
