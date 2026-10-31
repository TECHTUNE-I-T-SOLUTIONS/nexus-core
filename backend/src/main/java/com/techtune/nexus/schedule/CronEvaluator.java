package com.techtune.nexus.schedule;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Evaluates cron expressions and calculates next execution times.
 * Uses standard Unix cron syntax (5 fields: minute, hour, day of month, month, day of week).
 */
public class CronEvaluator {

    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);

    private static final CronParser PARSER = new CronParser(CRON_DEFINITION);

    /**
     * Validate a cron expression.
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static void validate(String cronExpression) {
        try {
            PARSER.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }

    /**
     * Calculate the next execution time for a cron expression.
     * @param cronExpression The cron expression
     * @param timezone The timezone ID (e.g., "UTC", "America/New_York")
     * @param from The starting point for calculation
     * @return The next execution time, or empty if the expression never matches
     */
    public static Optional<Instant> calculateNextExecution(String cronExpression, String timezone, Instant from) {
        try {
            Cron cron = PARSER.parse(cronExpression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime fromZoned = from.atZone(zoneId);
            Optional<ZonedDateTime> nextZoned = executionTime.nextExecution(fromZoned);
            return nextZoned.map(ZonedDateTime::toInstant);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to calculate next execution for cron: " + cronExpression, e);
        }
    }

    /**
     * Calculate the next execution time using UTC timezone.
     */
    public static Optional<Instant> calculateNextExecution(String cronExpression, Instant from) {
        return calculateNextExecution(cronExpression, "UTC", from);
    }
}
