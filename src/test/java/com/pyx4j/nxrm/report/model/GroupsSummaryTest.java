package com.pyx4j.nxrm.report.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for GroupsSummary functionality.
 */
class GroupsSummaryTest {

    @Test
    void groupsSummary_initialState_shouldHaveZeroValues() {
        GroupsSummary summary = new GroupsSummary();

        assertThat(summary.getGroupStats()).isEmpty();
        assertThat(summary.getTotalComponents()).isZero();
        assertThat(summary.getTotalSizeBytes()).isZero();
    }

    @Test
    void addGroupStats_withValidInput_shouldCreateGroupAndUpdateTotals() {
        GroupsSummary summary = new GroupsSummary();

        summary.addGroupStats("org.springframework", 100, 1024000);

        assertThat(summary.getGroupStats()).hasSize(1);
        assertThat(summary.getGroupStats().get("org.springframework").getComponentCount()).isEqualTo(100);
        assertThat(summary.getGroupStats().get("org.springframework").getSizeBytes()).isEqualTo(1024000);
        assertThat(summary.getTotalComponents()).isEqualTo(100);
        assertThat(summary.getTotalSizeBytes()).isEqualTo(1024000);
    }

    @Test
    void addGroupStats_multipleGroups_shouldTrackSeparately() {
        GroupsSummary summary = new GroupsSummary();

        summary.addGroupStats("org.springframework", 100, 1024000);
        summary.addGroupStats("com.example", 50, 512000);

        assertThat(summary.getGroupStats()).hasSize(2);
        assertThat(summary.getGroupStats().get("org.springframework").getComponentCount()).isEqualTo(100);
        assertThat(summary.getGroupStats().get("com.example").getComponentCount()).isEqualTo(50);
        assertThat(summary.getTotalComponents()).isEqualTo(150);
        assertThat(summary.getTotalSizeBytes()).isEqualTo(1536000);
    }

    @Test
    void addGroupStats_sameGroupMultipleTimes_shouldAccumulate() {
        GroupsSummary summary = new GroupsSummary();

        summary.addGroupStats("org.springframework", 50, 512000);
        summary.addGroupStats("org.springframework", 30, 256000);

        assertThat(summary.getGroupStats()).hasSize(1);
        assertThat(summary.getGroupStats().get("org.springframework").getComponentCount()).isEqualTo(80);
        assertThat(summary.getGroupStats().get("org.springframework").getSizeBytes()).isEqualTo(768000);
        assertThat(summary.getTotalComponents()).isEqualTo(80);
        assertThat(summary.getTotalSizeBytes()).isEqualTo(768000);
    }

    @Test
    void addGroupStats_withNullGroupName_shouldThrowException() {
        GroupsSummary summary = new GroupsSummary();

        assertThatThrownBy(() -> summary.addGroupStats(null, 10, 1024))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Group name cannot be null");
    }

    @Test
    void getGroupStats_shouldReturnUnmodifiableMap() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("org.springframework", 100, 1024000);

        var groupStats = summary.getGroupStats();

        assertThatThrownBy(() -> groupStats.put("test", new GroupStats()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
