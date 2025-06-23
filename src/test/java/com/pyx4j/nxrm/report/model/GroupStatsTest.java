package com.pyx4j.nxrm.report.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for GroupStats functionality.
 */
class GroupStatsTest {

    @Test
    void groupStats_initialState_shouldHaveZeroValues() {
        GroupStats stats = new GroupStats();

        assertThat(stats.getComponentCount()).isZero();
        assertThat(stats.getSizeBytes()).isZero();
    }

    @Test
    void addComponents_withValidInput_shouldUpdateCounts() {
        GroupStats stats = new GroupStats();

        stats.addComponents(10, 1024);

        assertThat(stats.getComponentCount()).isEqualTo(10);
        assertThat(stats.getSizeBytes()).isEqualTo(1024);
    }

    @Test
    void addComponents_multipleAdds_shouldAccumulate() {
        GroupStats stats = new GroupStats();

        stats.addComponents(5, 512);
        stats.addComponents(3, 256);

        assertThat(stats.getComponentCount()).isEqualTo(8);
        assertThat(stats.getSizeBytes()).isEqualTo(768);
    }
}
