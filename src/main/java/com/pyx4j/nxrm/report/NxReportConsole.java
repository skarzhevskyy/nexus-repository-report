package com.pyx4j.nxrm.report;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pyx4j.nxrm.report.model.ComponentsSummary;
import com.pyx4j.nxrm.report.model.RepositoryStats;
import com.pyx4j.nxrm.report.model.GroupsSummary;
import com.pyx4j.nxrm.report.model.GroupStats;

class NxReportConsole {

    /**
     * Prints the component summary to the console.
     *
     * @param summary The summary to print
     * @param sortBy  The sorting option to use
     */
    static void printSummary(ComponentsSummary summary, SortBy sortBy) {
        printSummary(summary, sortBy, System.out);
    }

    /**
     * Prints the component summary to the specified PrintStream.
     *
     * @param summary The summary to print
     * @param sortBy  The sorting option to use
     * @param out     The PrintStream to write to
     */
    static void printSummary(ComponentsSummary summary, SortBy sortBy, PrintStream out) {
        out.println("\nRepository Report Summary:");
        out.println("======================================================================");

        // Calculate the maximum repository name length for dynamic formatting
        int maxRepoNameLength = Math.max(30, // minimum width
                summary.getRepositoryStats().keySet().stream()
                        .mapToInt(String::length)
                        .max()
                        .orElse(30) + 2); // add some padding

        // Create format strings based on calculated width
        String headerFormat = "%-" + maxRepoNameLength + "s %-10s %-12s %-15s%n";
        String separatorFormat = "%-" + maxRepoNameLength + "s %-10s %-12s %-15s%n";
        String dataFormat = "%-" + maxRepoNameLength + "s %-10s %12d %15s%n";

        // Print header
        out.printf(headerFormat, "Repository", "Format", "Components", "Total Size");
        out.printf(separatorFormat,
                "-".repeat(maxRepoNameLength),
                "----------",
                "------------",
                "---------------");

        // Sort and print repository data
        getSortedRepositoryEntries(summary.getRepositoryStats(), sortBy)
                .forEach(entry -> {
                    String repoName = entry.getKey();
                    RepositoryStats stats = entry.getValue();
                    out.printf(dataFormat,
                            repoName,
                            stats.getFormat(),
                            stats.getComponentCount(),
                            formatSize(stats.getSizeBytes()));
                });

        // Print total
        out.printf("%n" + dataFormat,
                "TOTAL", "-", summary.getTotalComponents(), formatSize(summary.getTotalSizeBytes()));
    }

    /**
     * Prints the groups summary to the console.
     *
     * @param summary   The groups summary to print
     * @param sortBy    The sorting option to use
     * @param topGroups The number of top groups to show
     */
    static void printGroupsSummary(GroupsSummary summary, SortBy sortBy, int topGroups) {
        printGroupsSummary(summary, sortBy, topGroups, System.out);
    }

    /**
     * Prints the groups summary to the specified PrintStream.
     *
     * @param summary   The groups summary to print
     * @param sortBy    The sorting option to use
     * @param topGroups The number of top groups to show
     * @param out       The PrintStream to write to
     */
    static void printGroupsSummary(GroupsSummary summary, SortBy sortBy, int topGroups, PrintStream out) {
        String sortDescription = sortBy == SortBy.SIZE ? "Size" : "Components";
        out.println("\nTop Consuming Groups (by " + sortDescription + "):");
        out.println("======================================================================");

        // Calculate the maximum group name length for dynamic formatting
        int maxGroupNameLength = Math.max(30, // minimum width
                summary.getGroupStats().keySet().stream()
                        .mapToInt(String::length)
                        .max()
                        .orElse(30) + 2); // add some padding

        // Create format strings based on calculated width
        String headerFormat = "%-" + maxGroupNameLength + "s %-12s %-15s%n";
        String separatorFormat = "%-" + maxGroupNameLength + "s %-12s %-15s%n";
        String dataFormat = "%-" + maxGroupNameLength + "s %12d %15s%n";

        // Print header
        out.printf(headerFormat, "Group", "Components", "Total Size");
        out.printf(separatorFormat,
                "-".repeat(maxGroupNameLength),
                "------------",
                "---------------");

        // Sort and print group data (limited to topGroups)
        getSortedGroupEntries(summary.getGroupStats(), sortBy).stream()
                .limit(topGroups)
                .forEach(entry -> {
                    String groupName = entry.getKey();
                    GroupStats stats = entry.getValue();
                    out.printf(dataFormat,
                            groupName,
                            stats.getComponentCount(),
                            formatSize(stats.getSizeBytes()));
                });
    }

    /**
     * Gets repository entries sorted according to the specified criteria.
     *
     * @param repositoryStats The repository statistics map
     * @param sortBy          The sorting criteria
     * @return Sorted list of map entries
     */
    private static List<Map.Entry<String, RepositoryStats>> getSortedRepositoryEntries(
            Map<String, RepositoryStats> repositoryStats, SortBy sortBy) {

        Comparator<Map.Entry<String, RepositoryStats>> comparator;

        switch (sortBy) {
            case NAME:
                comparator = Map.Entry.comparingByKey();
                break;
            case SIZE:
                comparator = Map.Entry.<String, RepositoryStats>comparingByValue(
                                Comparator.comparingLong(RepositoryStats::getSizeBytes))
                        .reversed(); // Largest first
                break;
            case COMPONENTS:
            default:
                comparator = Map.Entry.<String, RepositoryStats>comparingByValue(
                                Comparator.comparingLong(RepositoryStats::getComponentCount))
                        .reversed(); // Most components first
                break;
        }

        return repositoryStats.entrySet().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Gets group entries sorted according to the specified criteria.
     *
     * @param groupStats The group statistics map
     * @param sortBy     The sorting criteria
     * @return Sorted list of map entries
     */
    private static List<Map.Entry<String, GroupStats>> getSortedGroupEntries(
            Map<String, GroupStats> groupStats, SortBy sortBy) {

        Comparator<Map.Entry<String, GroupStats>> comparator;

        switch (sortBy) {
            case NAME:
                comparator = Map.Entry.comparingByKey();
                break;
            case SIZE:
                comparator = Map.Entry.<String, GroupStats>comparingByValue(
                                Comparator.comparingLong(GroupStats::getSizeBytes))
                        .reversed(); // Largest first
                break;
            case COMPONENTS:
            default:
                comparator = Map.Entry.<String, GroupStats>comparingByValue(
                                Comparator.comparingLong(GroupStats::getComponentCount))
                        .reversed(); // Most components first
                break;
        }

        return groupStats.entrySet().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Formats a size in bytes to a human-readable string (e.g. "2.1 GB").
     *
     * @param bytes The size in bytes
     * @return Human-readable size string
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double value = bytes;

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;

        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", value, units[unitIndex]);
    }
}
