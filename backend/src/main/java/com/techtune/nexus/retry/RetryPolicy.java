package com.techtune.nexus.retry;

import java.time.Duration;
import java.time.Instant;

/**
 * Retry policy configuration for job execution.
 * Supports fixed and exponential backoff with optional jitter.
 */
public record RetryPolicy(
        BackoffStrategy backoffStrategy,
        Duration initialDelay,
        Duration maxDelay,
        int maxAttempts,
        double jitterFactor
) {
    public enum BackoffStrategy {
        FIXED,
        EXPONENTIAL
    }

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (initialDelay.isNegative() || initialDelay.isZero()) {
            throw new IllegalArgumentException("initialDelay must be positive");
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be >= initialDelay");
        }
        if (jitterFactor < 0 || jitterFactor > 1) {
            throw new IllegalArgumentException("jitterFactor must be between 0 and 1");
        }
    }

    /**
     * Calculate the delay for a given attempt number.
     * Attempts are 1-indexed (first retry is attempt 1).
     */
    public Duration calculateDelay(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }

        Duration baseDelay = switch (backoffStrategy) {
            case FIXED -> initialDelay;
            case EXPONENTIAL -> calculateExponentialDelay(attempt);
        };

        // Apply jitter if configured
        if (jitterFactor > 0) {
            baseDelay = applyJitter(baseDelay);
        }

        // Cap at max delay
        return Duration.ofMillis(Math.min(baseDelay.toMillis(), maxDelay.toMillis()));
    }

    private Duration calculateExponentialDelay(int attempt) {
        long delayMillis = initialDelay.toMillis() * (1L << (attempt - 1));
        return Duration.ofMillis(delayMillis);
    }

    private Duration applyJitter(Duration delay) {
        long delayMillis = delay.toMillis();
        long jitterMillis = (long) (delayMillis * jitterFactor * (Math.random() * 2 - 1));
        return Duration.ofMillis(delayMillis + jitterMillis);
    }

    /**
     * Create a default retry policy.
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(
                BackoffStrategy.EXPONENTIAL,
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                5,
                0.1
        );
    }

    /**
     * Check if another retry is allowed for the given attempt count.
     */
    public boolean canRetry(int attemptCount) {
        return attemptCount < maxAttempts;
    }

    /**
     * Calculate the next execution time for a retry.
     */
    public Instant calculateNextExecutionTime(int attemptCount) {
        Duration delay = calculateDelay(attemptCount);
        return Instant.now().plus(delay);
    }
}
