package com.techtune.nexus.retry;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void shouldCalculateFixedBackoff() {
        RetryPolicy policy = new RetryPolicy(
                RetryPolicy.BackoffStrategy.FIXED,
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                5,
                0.0
        );

        assertEquals(Duration.ofSeconds(5), policy.calculateDelay(1));
        assertEquals(Duration.ofSeconds(5), policy.calculateDelay(2));
        assertEquals(Duration.ofSeconds(5), policy.calculateDelay(3));
    }

    @Test
    void shouldCalculateExponentialBackoff() {
        RetryPolicy policy = new RetryPolicy(
                RetryPolicy.BackoffStrategy.EXPONENTIAL,
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                5,
                0.0
        );

        assertEquals(Duration.ofSeconds(5), policy.calculateDelay(1));    // 5s
        assertEquals(Duration.ofSeconds(10), policy.calculateDelay(2));   // 10s
        assertEquals(Duration.ofSeconds(20), policy.calculateDelay(3));   // 20s
        assertEquals(Duration.ofSeconds(40), policy.calculateDelay(4));   // 40s
    }

    @Test
    void shouldCapDelayAtMax() {
        RetryPolicy policy = new RetryPolicy(
                RetryPolicy.BackoffStrategy.EXPONENTIAL,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15),
                5,
                0.0
        );

        assertEquals(Duration.ofSeconds(5), policy.calculateDelay(1));
        assertEquals(Duration.ofSeconds(10), policy.calculateDelay(2));
        assertEquals(Duration.ofSeconds(15), policy.calculateDelay(3));  // Capped at max
        assertEquals(Duration.ofSeconds(15), policy.calculateDelay(4));  // Capped at max
    }

    @Test
    void shouldApplyJitter() {
        RetryPolicy policy = new RetryPolicy(
                RetryPolicy.BackoffStrategy.FIXED,
                Duration.ofSeconds(10),
                Duration.ofMinutes(5),
                5,
                0.2  // 20% jitter
        );

        Duration delay = policy.calculateDelay(1);
        // With 20% jitter, delay should be between 8s and 12s
        assertTrue(delay.toMillis() >= 8000);
        assertTrue(delay.toMillis() <= 12000);
    }

    @Test
    void shouldCheckRetryEligibility() {
        RetryPolicy policy = new RetryPolicy(
                RetryPolicy.BackoffStrategy.EXPONENTIAL,
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                3,
                0.0
        );

        assertTrue(policy.canRetry(0));
        assertTrue(policy.canRetry(1));
        assertTrue(policy.canRetry(2));
        assertFalse(policy.canRetry(3));
    }

    @Test
    void shouldCalculateNextExecutionTime() {
        RetryPolicy policy = new RetryPolicy(
                RetryPolicy.BackoffStrategy.FIXED,
                Duration.ofSeconds(10),
                Duration.ofMinutes(5),
                5,
                0.0
        );

        Instant now = Instant.now();
        Instant nextExecution = policy.calculateNextExecutionTime(1);
        
        assertTrue(nextExecution.isAfter(now));
        assertTrue(nextExecution.isBefore(now.plusSeconds(11)));
    }

    @Test
    void shouldRejectInvalidMaxAttempts() {
        assertThrows(IllegalArgumentException.class, () ->
                new RetryPolicy(
                        RetryPolicy.BackoffStrategy.EXPONENTIAL,
                        Duration.ofSeconds(5),
                        Duration.ofMinutes(5),
                        0,
                        0.0
                )
        );
    }

    @Test
    void shouldRejectNegativeInitialDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                new RetryPolicy(
                        RetryPolicy.BackoffStrategy.EXPONENTIAL,
                        Duration.ofSeconds(-1),
                        Duration.ofMinutes(5),
                        5,
                        0.0
                )
        );
    }

    @Test
    void shouldRejectMaxDelayLessThanInitial() {
        assertThrows(IllegalArgumentException.class, () ->
                new RetryPolicy(
                        RetryPolicy.BackoffStrategy.EXPONENTIAL,
                        Duration.ofMinutes(5),
                        Duration.ofSeconds(5),
                        5,
                        0.0
                )
        );
    }

    @Test
    void shouldRejectInvalidJitterFactor() {
        assertThrows(IllegalArgumentException.class, () ->
                new RetryPolicy(
                        RetryPolicy.BackoffStrategy.EXPONENTIAL,
                        Duration.ofSeconds(5),
                        Duration.ofMinutes(5),
                        5,
                        1.5
                )
        );
    }

    @Test
    void shouldCreateDefaultPolicy() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        
        assertEquals(RetryPolicy.BackoffStrategy.EXPONENTIAL, policy.backoffStrategy());
        assertEquals(Duration.ofSeconds(5), policy.initialDelay());
        assertEquals(Duration.ofMinutes(5), policy.maxDelay());
        assertEquals(5, policy.maxAttempts());
        assertEquals(0.1, policy.jitterFactor());
    }
}
