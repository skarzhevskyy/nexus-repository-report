package com.pyx4j.nxrm.report.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for AgeBucket functionality.
 */
class AgeBucketTest {

    @Test
    void ageBucket_withValidRangeFormat_shouldParseCorrectly() {
        AgeBucket bucket = new AgeBucket("0-7");

        assertThat(bucket.getMinDays()).isEqualTo(0);
        assertThat(bucket.getMaxDays()).isEqualTo(7);
        assertThat(bucket.getRangeDescription()).isEqualTo("0-7 days");
        assertThat(bucket.getOriginalRange()).isEqualTo("0-7");
    }

    @Test
    void ageBucket_withGreaterThanFormat_shouldParseCorrectly() {
        AgeBucket bucket = new AgeBucket(">365");

        assertThat(bucket.getMinDays()).isEqualTo(366);
        assertThat(bucket.getMaxDays()).isNull();
        assertThat(bucket.getRangeDescription()).isEqualTo(">365 days");
        assertThat(bucket.getOriginalRange()).isEqualTo(">365");
    }

    @Test
    void ageBucket_withWhitespace_shouldTrimCorrectly() {
        AgeBucket bucket = new AgeBucket("  8-30  ");

        assertThat(bucket.getMinDays()).isEqualTo(8);
        assertThat(bucket.getMaxDays()).isEqualTo(30);
        assertThat(bucket.getOriginalRange()).isEqualTo("8-30");
    }

    @Test
    void ageBucket_withInvalidRangeOrder_shouldThrowException() {
        assertThatThrownBy(() -> new AgeBucket("30-8"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("min days cannot be greater than max days");
    }

    @Test
    void ageBucket_withInvalidFormat_shouldThrowException() {
        assertThatThrownBy(() -> new AgeBucket("invalid-format"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid age bucket format");
    }

    @Test
    void ageBucket_withNullRange_shouldThrowException() {
        assertThatThrownBy(() -> new AgeBucket(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Range description cannot be null");
    }

    @Test
    void contains_withRangeBucket_shouldTestCorrectly() {
        AgeBucket bucket = new AgeBucket("8-30");

        assertThat(bucket.contains(7)).isFalse();
        assertThat(bucket.contains(8)).isTrue();
        assertThat(bucket.contains(15)).isTrue();
        assertThat(bucket.contains(30)).isTrue();
        assertThat(bucket.contains(31)).isFalse();
    }

    @Test
    void contains_withGreaterThanBucket_shouldTestCorrectly() {
        AgeBucket bucket = new AgeBucket(">365");

        assertThat(bucket.contains(365)).isFalse();
        assertThat(bucket.contains(366)).isTrue();
        assertThat(bucket.contains(1000)).isTrue();
    }

    @Test
    void addComponents_shouldAccumulateCorrectly() {
        AgeBucket bucket = new AgeBucket("0-7");

        assertThat(bucket.getComponentCount()).isZero();
        assertThat(bucket.getSizeBytes()).isZero();

        bucket.addComponents(5, 1024);
        assertThat(bucket.getComponentCount()).isEqualTo(5);
        assertThat(bucket.getSizeBytes()).isEqualTo(1024);

        bucket.addComponents(3, 512);
        assertThat(bucket.getComponentCount()).isEqualTo(8);
        assertThat(bucket.getSizeBytes()).isEqualTo(1536);
    }

    @Test
    void equals_shouldCompareByRangeDescription() {
        AgeBucket bucket1 = new AgeBucket("0-7");
        AgeBucket bucket2 = new AgeBucket("0-7");
        AgeBucket bucket3 = new AgeBucket("8-30");

        assertThat(bucket1).isEqualTo(bucket2);
        assertThat(bucket1).isNotEqualTo(bucket3);
        assertThat(bucket1.hashCode()).isEqualTo(bucket2.hashCode());
    }

    @Test
    void toString_shouldContainRelevantInfo() {
        AgeBucket bucket = new AgeBucket("0-7");
        bucket.addComponents(5, 1024);

        String toString = bucket.toString();
        assertThat(toString).contains("0-7");
        assertThat(toString).contains("5");
        assertThat(toString).contains("1024");
    }
}