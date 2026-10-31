package com.techtune.nexus.schedule;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CronEvaluatorTest {

    @Test
    void shouldValidateValidCronExpression() {
        assertDoesNotThrow(() -> CronEvaluator.validate("0 * * * *"));
        assertDoesNotThrow(() -> CronEvaluator.validate("*/5 * * * *"));
        assertDoesNotThrow(() -> CronEvaluator.validate("0 0 * * *"));
    }

    @Test
    void shouldRejectInvalidCronExpression() {
        assertThrows(IllegalArgumentException.class, () -> CronEvaluator.validate("invalid"));
        assertThrows(IllegalArgumentException.class, () -> CronEvaluator.validate("0 0 0 0 0 0")); // 6 fields not supported
    }

    @Test
    void shouldCalculateNextExecutionForHourly() {
        Instant from = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant();
        Optional<Instant> next = CronEvaluator.calculateNextExecution("0 * * * *", "UTC", from);
        
        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2024, 1, 1, 13, 0, 0, 0, ZoneId.of("UTC")).toInstant(), next.get());
    }

    @Test
    void shouldCalculateNextExecutionForDaily() {
        Instant from = ZonedDateTime.of(2024, 1, 1, 8, 30, 0, 0, ZoneId.of("UTC")).toInstant();
        Optional<Instant> next = CronEvaluator.calculateNextExecution("0 9 * * *", "UTC", from);
        
        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2024, 1, 1, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant(), next.get());
    }

    @Test
    void shouldRespectTimezone() {
        Instant from = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant();
        Optional<Instant> next = CronEvaluator.calculateNextExecution("0 9 * * *", "America/New_York", from);
        
        assertTrue(next.isPresent());
        // 9 AM EST is 2 PM UTC
        assertEquals(ZonedDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant(), next.get());
    }

    @Test
    void shouldHandleEvery5Minutes() {
        Instant from = ZonedDateTime.of(2024, 1, 1, 12, 2, 0, 0, ZoneId.of("UTC")).toInstant();
        Optional<Instant> next = CronEvaluator.calculateNextExecution("*/5 * * * *", "UTC", from);
        
        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2024, 1, 1, 12, 5, 0, 0, ZoneId.of("UTC")).toInstant(), next.get());
    }
}
