package com.pyx4j.nxrm.report.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores summary information about components across repositories.
 */
public class RepositoryComponentsSummary extends ReportSection {

    private final Map<String, RepositoryStats> repositoryStats;

    private long totalComponents;

    private long totalSizeBytes;

    public RepositoryComponentsSummary() {
        this.repositoryStats = new HashMap<>();
        this.totalComponents = 0;
        this.totalSizeBytes = 0;
    }

    /**
     * Adds statistics for a repository.
     *
     * @param repositoryName The name of the repository
     * @param format         The format of the repository (e.g., maven, npm)
     * @param componentCount The number of components in the repository
     * @param sizeBytes      The total size in bytes of all components in the repository
     */
    public void addRepositoryStats(String repositoryName, String format, long componentCount, long sizeBytes) {
        Objects.requireNonNull(repositoryName, "Repository name cannot be null");
        Objects.requireNonNull(format, "Format cannot be null");

        RepositoryStats stats = repositoryStats.computeIfAbsent(repositoryName, k -> new RepositoryStats(format));
        stats.addComponents(componentCount, sizeBytes);

        // Update totals
        totalComponents += componentCount;
        totalSizeBytes += sizeBytes;
    }

    /**
     * Gets an unmodifiable view of the repository statistics.
     *
     * @return Map of repository names to their statistics
     */
    public Map<String, RepositoryStats> getRepositoryStats() {
        return Collections.unmodifiableMap(repositoryStats);
    }

    /**
     * Gets the total number of components across all repositories.
     *
     * @return Total component count
     */
    public long getTotalComponents() {
        return totalComponents;
    }

    /**
     * Gets the total size in bytes across all repositories.
     *
     * @return Total size in bytes
     */
    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

}
