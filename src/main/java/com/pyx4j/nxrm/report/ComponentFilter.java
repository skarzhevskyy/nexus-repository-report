package com.pyx4j.nxrm.report;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Utility class for creating component filters based on date criteria.
 */
final class ComponentFilter {

    private ComponentFilter() {
        // Utility class should not be instantiated
    }

    /**
     * Creates a filter based on the provided date filtering arguments.
     *
     * @param args The command line arguments containing filter criteria
     * @return A predicate that tests whether a component matches the filter criteria
     */
    @NonNull
    static Predicate<ComponentXO> createFilter(@NonNull NxReportCommandArgs args) {
        Objects.requireNonNull(args, "Command arguments cannot be null");

        // Parse date filters
        OffsetDateTime createdBefore = DateFilterParser.parseDate(args.createdBefore);
        OffsetDateTime createdAfter = DateFilterParser.parseDate(args.createdAfter);
        OffsetDateTime updatedBefore = DateFilterParser.parseDate(args.updatedBefore);
        OffsetDateTime updatedAfter = DateFilterParser.parseDate(args.updatedAfter);
        OffsetDateTime downloadedBefore = DateFilterParser.parseDate(args.downloadedBefore);
        OffsetDateTime downloadedAfter = DateFilterParser.parseDate(args.downloadedAfter);

        // Validate date ranges
        DateFilterParser.validateDateRange(createdBefore, createdAfter, "created");
        DateFilterParser.validateDateRange(updatedBefore, updatedAfter, "updated");
        DateFilterParser.validateDateRange(downloadedBefore, downloadedAfter, "downloaded");

        // Validate conflicting filters
        if (args.neverDownloaded && (downloadedBefore != null || downloadedAfter != null)) {
            throw new IllegalArgumentException("Cannot combine --never-downloaded with --downloaded-before or --downloaded-after filters");
        }

        return component -> {
            if (component == null || component.getAssets() == null || component.getAssets().isEmpty()) {
                return false;
            }

            // A component matches if ANY of its assets match all the criteria
            return component.getAssets().stream().anyMatch(asset -> 
                matchesCreatedFilter(asset, createdBefore, createdAfter) &&
                matchesUpdatedFilter(asset, updatedBefore, updatedAfter) &&
                matchesDownloadedFilter(asset, downloadedBefore, downloadedAfter, args.neverDownloaded)
            );
        };
    }

    private static boolean matchesCreatedFilter(@NonNull AssetXO asset, 
                                              @Nullable OffsetDateTime createdBefore, 
                                              @Nullable OffsetDateTime createdAfter) {
        OffsetDateTime blobCreated = asset.getBlobCreated();
        
        if (createdBefore != null || createdAfter != null) {
            if (blobCreated == null) {
                return false; // Asset without creation date doesn't match time-based filters
            }
            
            if (createdBefore != null && !blobCreated.isBefore(createdBefore)) {
                return false;
            }
            
            if (createdAfter != null && !blobCreated.isAfter(createdAfter)) {
                return false;
            }
        }
        
        return true;
    }

    private static boolean matchesUpdatedFilter(@NonNull AssetXO asset, 
                                              @Nullable OffsetDateTime updatedBefore, 
                                              @Nullable OffsetDateTime updatedAfter) {
        OffsetDateTime lastModified = asset.getLastModified();
        
        if (updatedBefore != null || updatedAfter != null) {
            if (lastModified == null) {
                return false; // Asset without modified date doesn't match time-based filters
            }
            
            if (updatedBefore != null && !lastModified.isBefore(updatedBefore)) {
                return false;
            }
            
            if (updatedAfter != null && !lastModified.isAfter(updatedAfter)) {
                return false;
            }
        }
        
        return true;
    }

    private static boolean matchesDownloadedFilter(@NonNull AssetXO asset, 
                                                 @Nullable OffsetDateTime downloadedBefore, 
                                                 @Nullable OffsetDateTime downloadedAfter,
                                                 boolean neverDownloaded) {
        OffsetDateTime lastDownloaded = asset.getLastDownloaded();
        
        if (neverDownloaded) {
            return lastDownloaded == null;
        }
        
        if (downloadedBefore != null || downloadedAfter != null) {
            if (lastDownloaded == null) {
                return false; // Asset never downloaded doesn't match time-based download filters
            }
            
            if (downloadedBefore != null && !lastDownloaded.isBefore(downloadedBefore)) {
                return false;
            }
            
            if (downloadedAfter != null && !lastDownloaded.isAfter(downloadedAfter)) {
                return false;
            }
        }
        
        return true;
    }
}