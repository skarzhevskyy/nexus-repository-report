package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for SortBy enum functionality.
 */
class SortByTest {

    @Test
    void fromString_withValidValues_shouldReturnCorrectEnum() {
        assertThat(SortBy.fromString("name")).isEqualTo(SortBy.NAME);
        assertThat(SortBy.fromString("components")).isEqualTo(SortBy.COMPONENTS);
        assertThat(SortBy.fromString("size")).isEqualTo(SortBy.SIZE);
    }

    @Test
    void fromString_withCaseInsensitiveValues_shouldReturnCorrectEnum() {
        assertThat(SortBy.fromString("NAME")).isEqualTo(SortBy.NAME);
        assertThat(SortBy.fromString("Components")).isEqualTo(SortBy.COMPONENTS);
        assertThat(SortBy.fromString("SIZE")).isEqualTo(SortBy.SIZE);
    }

    @Test
    void fromString_withWhitespace_shouldReturnCorrectEnum() {
        assertThat(SortBy.fromString("  name  ")).isEqualTo(SortBy.NAME);
        assertThat(SortBy.fromString(" components ")).isEqualTo(SortBy.COMPONENTS);
    }

    @Test
    void fromString_withNull_shouldReturnDefaultComponents() {
        assertThat(SortBy.fromString(null)).isEqualTo(SortBy.COMPONENTS);
    }

    @Test
    void fromString_withInvalidValue_shouldThrowException() {
        assertThatThrownBy(() -> SortBy.fromString("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort option: invalid");
    }

    @Test
    void getValue_shouldReturnCorrectStringValue() {
        assertThat(SortBy.NAME.getValue()).isEqualTo("name");
        assertThat(SortBy.COMPONENTS.getValue()).isEqualTo("components");
        assertThat(SortBy.SIZE.getValue()).isEqualTo("size");
    }
}