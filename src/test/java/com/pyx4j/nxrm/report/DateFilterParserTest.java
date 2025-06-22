package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class DateFilterParserTest {

    @Test
    void parseDate_withNullString_shouldReturnNull() {
        assertThat(DateFilterParser.parseDate(null)).isNull();
    }

    @Test
    void parseDate_withEmptyString_shouldReturnNull() {
        assertThat(DateFilterParser.parseDate("")).isNull();
        assertThat(DateFilterParser.parseDate("   ")).isNull();
    }

    @Test
    void parseDate_withIso8601DateTime_shouldParseCorrectly() {
        OffsetDateTime result = DateFilterParser.parseDate("2024-06-01T00:00:00Z");

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(6);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
        assertThat(result.getHour()).isEqualTo(0);
        assertThat(result.getMinute()).isEqualTo(0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void parseDate_withIso8601Date_shouldParseToStartOfDayUtc() {
        OffsetDateTime result = DateFilterParser.parseDate("2024-06-01");

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(6);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
        assertThat(result.getHour()).isEqualTo(0);
        assertThat(result.getMinute()).isEqualTo(0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void parseDate_withDaysAgoPattern_shouldCalculateCorrectly() {
        OffsetDateTime beforeCall = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime result = DateFilterParser.parseDate("7d");
        OffsetDateTime afterCall = OffsetDateTime.now(ZoneOffset.UTC);

        assertThat(result).isNotNull();
        // Should be approximately 7 days ago (allowing for small timing differences)
        OffsetDateTime expectedEarliest = beforeCall.minusDays(7).minusSeconds(1);
        OffsetDateTime expectedLatest = afterCall.minusDays(7).plusSeconds(1);
        assertThat(result).isBetween(expectedEarliest, expectedLatest);
    }

    @Test
    void parseDate_withSingleDayAgo_shouldWork() {
        OffsetDateTime beforeCall = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime result = DateFilterParser.parseDate("1d");
        OffsetDateTime afterCall = OffsetDateTime.now(ZoneOffset.UTC);

        assertThat(result).isNotNull();
        OffsetDateTime expectedEarliest = beforeCall.minusDays(1).minusSeconds(1);
        OffsetDateTime expectedLatest = afterCall.minusDays(1).plusSeconds(1);
        assertThat(result).isBetween(expectedEarliest, expectedLatest);
    }

    @Test
    void parseDate_withZeroDaysAgo_shouldReturnToday() {
        OffsetDateTime beforeCall = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime result = DateFilterParser.parseDate("0d");
        OffsetDateTime afterCall = OffsetDateTime.now(ZoneOffset.UTC);

        assertThat(result).isNotNull();
        // Should be between before and after the call (allowing for execution time)
        assertThat(result).isBetween(beforeCall.minusSeconds(1), afterCall.plusSeconds(1));
    }

    @Test
    void parseDate_withInvalidFormat_shouldThrowException() {
        assertThatThrownBy(() -> DateFilterParser.parseDate("invalid-date"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format: 'invalid-date'");
    }

    @Test
    void parseDate_withInvalidDaysAgoFormat_shouldThrowException() {
        assertThatThrownBy(() -> DateFilterParser.parseDate("7days"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format: '7days'");
    }

    @Test
    void parseDate_withNegativeDaysAgo_shouldThrowException() {
        assertThatThrownBy(() -> DateFilterParser.parseDate("-5d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format: '-5d'");
    }

    @Test
    void parseDate_shouldTrimWhitespace() {
        OffsetDateTime result = DateFilterParser.parseDate("  2024-06-01  ");

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(6);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void validateDateRange_withValidRange_shouldNotThrow() {
        OffsetDateTime after = OffsetDateTime.parse("2024-06-01T00:00:00Z");
        OffsetDateTime before = OffsetDateTime.parse("2024-06-10T00:00:00Z");

        // Should not throw
        DateFilterParser.validateDateRange(before, after, "test");
    }

    @Test
    void validateDateRange_withSameDates_shouldNotThrow() {
        OffsetDateTime same = OffsetDateTime.parse("2024-06-01T00:00:00Z");

        // Should not throw
        DateFilterParser.validateDateRange(same, same, "test");
    }

    @Test
    void validateDateRange_withNullDates_shouldNotThrow() {
        // Should not throw
        DateFilterParser.validateDateRange(null, null, "test");
        DateFilterParser.validateDateRange(OffsetDateTime.now(), null, "test");
        DateFilterParser.validateDateRange(null, OffsetDateTime.now(), "test");
    }

    @Test
    void validateDateRange_withInvalidRange_shouldThrowException() {
        OffsetDateTime after = OffsetDateTime.parse("2024-06-10T00:00:00Z");
        OffsetDateTime before = OffsetDateTime.parse("2024-06-01T00:00:00Z");

        assertThatThrownBy(() -> DateFilterParser.validateDateRange(before, after, "created"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid created filter")
                .hasMessageContaining("'before' date")
                .hasMessageContaining("cannot be earlier than 'after' date");
    }

    @Test
    void validateDateRange_withNullFilterType_shouldThrowException() {
        OffsetDateTime date = OffsetDateTime.now();

        assertThatThrownBy(() -> DateFilterParser.validateDateRange(date, date, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Filter type cannot be null");
    }
}
