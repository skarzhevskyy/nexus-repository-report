package com.pyx4j.nxrm.report;

/**
 * Enumeration of available sorting options for repository reports.
 */
public enum SortBy {
    NAME("name"),
    COMPONENTS("components"),
    SIZE("size");

    private final String value;

    SortBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse a sort option from string value.
     *
     * @param value The string value to parse
     * @return The corresponding SortBy enum value
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static SortBy fromString(String value) {
        if (value == null) {
            return COMPONENTS; // default
        }
        
        for (SortBy sortBy : values()) {
            if (sortBy.value.equalsIgnoreCase(value.trim())) {
                return sortBy;
            }
        }
        
        throw new IllegalArgumentException("Invalid sort option: " + value + 
                ". Valid options are: name, components, size");
    }
}