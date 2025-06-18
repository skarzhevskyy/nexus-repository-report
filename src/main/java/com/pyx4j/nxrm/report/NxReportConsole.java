package com.pyx4j.nxrm.report;

import com.pyx4j.nxrm.report.model.ComponentsSummary;
import com.pyx4j.nxrm.report.model.RepositoryStats;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class NxReportConsole {

    /**
     * Prints the component summary to the console.
     *
     * @param summary The summary to print
     * @param sortBy The sorting option to use
     */
    static void printSummary(ComponentsSummary summary, SortBy sortBy) {
        System.out.println("\nRepository Report Summary:");
        System.out.println("======================================================================");
        
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
        System.out.printf(headerFormat, "Repository", "Format", "Components", "Total Size");
        System.out.printf(separatorFormat, 
                "-".repeat(maxRepoNameLength), 
                "----------", 
                "------------", 
                "---------------");

        // Sort and print repository data
        getSortedRepositoryEntries(summary.getRepositoryStats(), sortBy)
                .forEach(entry -> {
                    String repoName = entry.getKey();
                    RepositoryStats stats = entry.getValue();
                    System.out.printf(dataFormat,
                            repoName,
                            stats.getFormat(),
                            stats.getComponentCount(),
                            formatSize(stats.getSizeBytes()));
                });

        // Print total
        System.out.printf("%n" + dataFormat,
                "TOTAL", "-", summary.getTotalComponents(), formatSize(summary.getTotalSizeBytes()));
    }

    /**
     * Gets repository entries sorted according to the specified criteria.
     *
     * @param repositoryStats The repository statistics map
     * @param sortBy The sorting criteria
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
