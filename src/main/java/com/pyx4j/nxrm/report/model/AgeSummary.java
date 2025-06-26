package com.pyx4j.nxrm.report.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Stores summary information about components grouped by their age.
 */
public class AgeSummary extends ReportSection {

    private final List<AgeBucket> ageBuckets;
    private long totalComponents;
    private long totalSizeBytes;

    /**
     * Creates an AgeSummary with the specified age buckets.
     *
     * @param ageBucketRanges List of age bucket range descriptions (e.g., ["0-7", "8-30", ">365"])
     */
    public AgeSummary(@NonNull List<String> ageBucketRanges) {
        Objects.requireNonNull(ageBucketRanges, "Age bucket ranges cannot be null");

        if (ageBucketRanges.isEmpty()) {
            throw new IllegalArgumentException("Age bucket ranges cannot be empty");
        }

        this.ageBuckets = new ArrayList<>();
        this.totalComponents = 0;
        this.totalSizeBytes = 0;

        // Create age buckets from range descriptions
        for (String range : ageBucketRanges) {
            ageBuckets.add(new AgeBucket(range));
        }
    }

    /**
     * Adds a component to the appropriate age bucket based on its creation date.
     *
     * @param component The component to categorize by age
     * @param sizeBytes The size of the component in bytes
     */
    public void addComponent(@NonNull ComponentXO component, long sizeBytes) {
        Objects.requireNonNull(component, "Component cannot be null");

        // Find the earliest creation date among all assets
        OffsetDateTime earliestCreationDate = getEarliestCreationDate(component);

        if (earliestCreationDate == null) {
            // Skip components without creation dates
            return;
        }

        // Calculate age in days
        long ageInDays = ChronoUnit.DAYS.between(earliestCreationDate, OffsetDateTime.now(ZoneOffset.UTC));

        // Find the appropriate age bucket
        for (AgeBucket bucket : ageBuckets) {
            if (bucket.contains(ageInDays)) {
                bucket.addComponents(1, sizeBytes);
                totalComponents++;
                totalSizeBytes += sizeBytes;
                return;
            }
        }

        // If no bucket matched, this indicates a configuration issue
        // For robustness, we could add it to a catch-all bucket, but for now we'll skip it
    }

    /**
     * Gets the earliest creation date among all assets of a component.
     *
     * @param component The component to check
     * @return The earliest creation date, or null if no assets have creation dates
     */
    @Nullable
    private OffsetDateTime getEarliestCreationDate(@NonNull ComponentXO component) {
        if (component.getAssets() == null || component.getAssets().isEmpty()) {
            return null;
        }

        OffsetDateTime earliest = null;
        for (AssetXO asset : component.getAssets()) {
            OffsetDateTime blobCreated = asset.getBlobCreated();
            if (blobCreated != null) {
                if (earliest == null || blobCreated.isBefore(earliest)) {
                    earliest = blobCreated;
                }
            }
        }
        return earliest;
    }

    /**
     * Gets an unmodifiable view of the age buckets.
     *
     * @return List of age buckets
     */
    @NonNull
    public List<AgeBucket> getAgeBuckets() {
        return Collections.unmodifiableList(ageBuckets);
    }

    /**
     * Gets the total number of components across all age buckets.
     *
     * @return Total component count
     */
    public long getTotalComponents() {
        return totalComponents;
    }

    /**
     * Gets the total size in bytes across all age buckets.
     *
     * @return Total size in bytes
     */
    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }
}