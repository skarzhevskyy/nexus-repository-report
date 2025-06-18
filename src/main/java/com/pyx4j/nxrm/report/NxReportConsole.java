package com.pyx4j.nxrm.report;

import com.pyx4j.nxrm.report.model.ComponentsSummary;

class NxReportConsole {

    /**
     * Prints the component summary to the console.
     *
     * @param summary The summary to print
     */
    static void printSummary(ComponentsSummary summary) {
        System.out.println("\nRepository Report Summary:");
        System.out.println("======================================================================");
        System.out.printf("%-30s %-10s %-12s %-15s%n", "Repository", "Format", "Components", "Total Size");
        System.out.printf("%-30s %-10s %-12s %-15s%n", "------------------------------", "----------", "------------", "---------------");

        summary.getRepositoryStats().forEach((repoName, stats) -> {
            System.out.printf("%-30s %-10s %12d %15s%n",
                    repoName,
                    stats.getFormat(),
                    stats.getComponentCount(),
                    formatSize(stats.getSizeBytes()));
        });

        System.out.printf("%n%-30s %-10s %12d %15s%n",
                "TOTAL", "-", summary.getTotalComponents(), formatSize(summary.getTotalSizeBytes()));
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
