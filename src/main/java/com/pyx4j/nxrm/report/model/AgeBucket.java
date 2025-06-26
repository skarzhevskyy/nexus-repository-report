package com.pyx4j.nxrm.report.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an age bucket for categorizing components by their age.
 * Supports ranges like "0-7", "8-30", "91-365", and open-ended ranges like ">365".
 */
public class AgeBucket {

    private static final Pattern RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
    private static final Pattern GREATER_THAN_PATTERN = Pattern.compile(">(\\d+)");

    private final String rangeDescription;
    private final Integer minDays;
    private final Integer maxDays;
    private long componentCount;
    private long sizeBytes;

    /**
     * Creates an age bucket from a range description.
     *
     * @param rangeDescription The range description (e.g., "0-7", "8-30", ">365")
     */
    public AgeBucket(String rangeDescription) {
        Objects.requireNonNull(rangeDescription, "Range description cannot be null");
        this.rangeDescription = rangeDescription.trim();
        this.componentCount = 0;
        this.sizeBytes = 0;

        // Parse the range description
        Matcher rangeMatcher = RANGE_PATTERN.matcher(this.rangeDescription);
        Matcher greaterThanMatcher = GREATER_THAN_PATTERN.matcher(this.rangeDescription);

        if (rangeMatcher.matches()) {
            // Range format: "0-7", "8-30", etc.
            this.minDays = Integer.parseInt(rangeMatcher.group(1));
            this.maxDays = Integer.parseInt(rangeMatcher.group(2));

            if (this.minDays > this.maxDays) {
                throw new IllegalArgumentException("Invalid age bucket range: " + rangeDescription +
                        " (min days cannot be greater than max days)");
            }
        } else if (greaterThanMatcher.matches()) {
            // Greater than format: ">365"
            this.minDays = Integer.parseInt(greaterThanMatcher.group(1)) + 1;
            this.maxDays = null; // Open-ended
        } else {
            throw new IllegalArgumentException("Invalid age bucket format: " + rangeDescription +
                    ". Expected formats: '0-7', '8-30', or '>365'");
        }
    }

    /**
     * Checks if the given age in days falls within this bucket.
     *
     * @param days The age in days
     * @return true if the age falls within this bucket's range
     */
    public boolean contains(long days) {
        if (days < minDays) {
            return false;
        }
        return maxDays == null || days <= maxDays;
    }

    /**
     * Adds components to this age bucket.
     *
     * @param componentCount The number of components to add
     * @param sizeBytes      The total size in bytes to add
     */
    public void addComponents(long componentCount, long sizeBytes) {
        this.componentCount += componentCount;
        this.sizeBytes += sizeBytes;
    }

    /**
     * Gets the range description for this bucket.
     *
     * @return The range description (e.g., "0-7 days", ">365 days")
     */
    public String getRangeDescription() {
        return rangeDescription + " days";
    }

    /**
     * Gets the original range string.
     *
     * @return The original range string
     */
    public String getOriginalRange() {
        return rangeDescription;
    }

    /**
     * Gets the minimum days for this bucket.
     *
     * @return Minimum days (inclusive)
     */
    public Integer getMinDays() {
        return minDays;
    }

    /**
     * Gets the maximum days for this bucket.
     *
     * @return Maximum days (inclusive), or null for open-ended buckets
     */
    public Integer getMaxDays() {
        return maxDays;
    }

    /**
     * Gets the number of components in this bucket.
     *
     * @return Component count
     */
    public long getComponentCount() {
        return componentCount;
    }

    /**
     * Gets the total size in bytes for this bucket.
     *
     * @return Size in bytes
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgeBucket ageBucket = (AgeBucket) o;
        return Objects.equals(rangeDescription, ageBucket.rangeDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rangeDescription);
    }

    @Override
    public String toString() {
        return "AgeBucket{" +
                "range='" + rangeDescription + '\'' +
                ", components=" + componentCount +
                ", sizeBytes=" + sizeBytes +
                '}';
    }
}