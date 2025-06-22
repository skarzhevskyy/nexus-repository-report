package com.pyx4j.nxrm.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Utility class for parsing date filter arguments.
 * Supports ISO-8601 format dates and "days ago" patterns.
 */
final class DateFilterParser {

    private static final Pattern DAYS_AGO_PATTERN = Pattern.compile("^(\\d+)d$");

    private DateFilterParser() {
        // Utility class should not be instantiated
    }

    /**
     * Parses a date string that can be either ISO-8601 format or "Nd" format (N days ago).
     *
     * @param dateString The date string to parse
     * @return OffsetDateTime representation, or null if the string is null/empty
     * @throws IllegalArgumentException if the date string format is invalid
     */
    @Nullable
    static OffsetDateTime parseDate(@Nullable String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        String trimmed = dateString.trim();

        // Check if it's a "days ago" pattern
        Matcher daysAgoMatcher = DAYS_AGO_PATTERN.matcher(trimmed);
        if (daysAgoMatcher.matches()) {
            int daysAgo = Integer.parseInt(daysAgoMatcher.group(1));
            return OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysAgo);
        }

        // Try to parse as ISO-8601 date
        try {
            // First try to parse as full ISO-8601 with time
            return OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // Try to parse as just date (YYYY-MM-DD) and set time to start of day UTC
                LocalDate localDate = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
                return localDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format: '" + trimmed +
                        "'. Expected ISO-8601 format (e.g., '2024-06-01' or '2024-06-01T00:00:00Z') or 'Nd' format (e.g., '30d')", e2);
            }
        }
    }

    /**
     * Validates that date ranges are logical (before dates should be after after dates).
     *
     * @param before     The "before" date filter
     * @param after      The "after" date filter
     * @param filterType The type of filter for error messages
     * @throws IllegalArgumentException if the date range is invalid
     */
    static void validateDateRange(@Nullable OffsetDateTime before, @Nullable OffsetDateTime after, @NonNull String filterType) {
        Objects.requireNonNull(filterType, "Filter type cannot be null");

        if (before != null && after != null && before.isBefore(after)) {
            throw new IllegalArgumentException("Invalid " + filterType + " filter: 'before' date (" +
                    before + ") cannot be earlier than 'after' date (" + after + ")");
        }
    }
}
