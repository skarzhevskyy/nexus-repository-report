package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.pyx4j.nxrm.report.model.ComponentsSummary;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for NxReportConsole functionality.
 */
class NxReportConsoleTest {

    @Test
    void printSummary_withShortRepositoryNames_shouldFormatCorrectly() {
        ComponentsSummary summary = new ComponentsSummary();
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
        ComponentsSummary summary = new ComponentsSummary();
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
        ComponentsSummary summary = new ComponentsSummary();
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
        ComponentsSummary summary = new ComponentsSummary();
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
        ComponentsSummary summary = new ComponentsSummary();
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
        ComponentsSummary summary = new ComponentsSummary();
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
}
