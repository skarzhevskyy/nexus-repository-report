package com.pyx4j.nxrm.report;

import java.util.Locale;

import picocli.CommandLine;

/**
 * Enumeration of available sorting options for repository reports.
 */
public enum SortBy {

    NAME,

    COMPONENTS,

    SIZE;

    public static class CaseInsensitiveEnumConverter implements CommandLine.ITypeConverter<SortBy> {
        @Override
        public SortBy convert(String value) {
            return SortBy.valueOf(value.toUpperCase(Locale.CANADA));
        }
    }
}
