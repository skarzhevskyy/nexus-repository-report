package com.pyx4j.nxrm.report;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.base.Strings;
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

            // Apply component-level filters (repository, group, name)
            if (!matchesComponentFilters(component, args.repositories, args.groups, args.names)) {
                return false;
            }

            // neverDownloaded: no asset was ever downloaded
            if (args.neverDownloaded && component.getAssets().stream().anyMatch(asset -> asset.getLastDownloaded() != null)) {
                return false;
            }

            // A component matches if ANY of its assets match all the date criteria
            return component.getAssets().stream().anyMatch(asset ->
                    matchesCreatedFilter(asset, createdBefore, createdAfter) &&
                            matchesUpdatedFilter(asset, updatedBefore, updatedAfter) &&
                            matchesDownloadedFilter(asset, downloadedBefore, downloadedAfter)
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

            return createdAfter == null || blobCreated.isAfter(createdAfter);
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

            return updatedAfter == null || lastModified.isAfter(updatedAfter);
        }

        return true;
    }

    private static boolean matchesDownloadedFilter(@NonNull AssetXO asset,
                                                   @Nullable OffsetDateTime downloadedBefore,
                                                   @Nullable OffsetDateTime downloadedAfter) {
        OffsetDateTime lastDownloaded = asset.getLastDownloaded();

        if (downloadedBefore != null || downloadedAfter != null) {
            if (lastDownloaded == null) {
                return false; // Asset never downloaded doesn't match time-based download filters
            }

            if (downloadedBefore != null && !lastDownloaded.isBefore(downloadedBefore)) {
                return false;
            }

            return downloadedAfter == null || lastDownloaded.isAfter(downloadedAfter);
        }

        return true;
    }

    /**
     * Checks if a component matches the provided component-level filters.
     *
     * @param component    The component to test
     * @param repositories List of repository patterns (OR logic)
     * @param groups       List of group patterns (OR logic)
     * @param names        List of name patterns (OR logic)
     * @return true if the component matches all provided filters (AND logic between filter types)
     */
    private static boolean matchesComponentFilters(@NonNull ComponentXO component,
                                                   @Nullable List<String> repositories,
                                                   @Nullable List<String> groups,
                                                   @Nullable List<String> names) {
        // Repository filter
        if (repositories != null && !repositories.isEmpty()) {
            if (!matchesAnyPattern(component.getRepository(), repositories)) {
                return false;
            }
        }

        // Group filter
        if (groups != null && !groups.isEmpty()) {
            if (!matchesAnyPattern(component.getGroup(), groups)) {
                return false;
            }
        }

        // Name filter
        if (names != null && !names.isEmpty()) {
            return matchesAnyPattern(component.getName(), names);
        }

        return true;
    }

    /**
     * Checks if a value matches any of the provided wildcard patterns.
     *
     * @param value    The value to test (can be null)
     * @param patterns List of wildcard patterns
     * @return true if the value matches any pattern, false if value is null or no patterns match
     */
    private static boolean matchesAnyPattern(@Nullable String value, @NonNull List<String> patterns) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        return patterns.stream().anyMatch(pattern -> matchesWildcardPattern(value, pattern));
    }

    /**
     * Tests if a value matches a wildcard pattern.
     * Supports '*' (any characters) and '?' (single character) wildcards.
     *
     * @param value   The value to test
     * @param pattern The wildcard pattern
     * @return true if the value matches the pattern
     */
    private static boolean matchesWildcardPattern(@NonNull String value, @NonNull String pattern) {
        if (Strings.isNullOrEmpty(pattern)) {
            return Strings.isNullOrEmpty(value);
        }

        // Convert wildcard pattern to regex
        // Escape special regex characters except * and ?
        String regex = pattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("|", "\\|")
                // Now handle wildcards
                .replace("*", ".*")
                .replace("?", ".");

        return value.matches(regex);
    }
}
