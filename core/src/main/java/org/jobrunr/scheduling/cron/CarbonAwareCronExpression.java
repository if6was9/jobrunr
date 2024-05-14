package org.jobrunr.scheduling.cron;

import org.jobrunr.scheduling.Schedule;
import org.jobrunr.scheduling.TemporalWrapper;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.carbonaware.CarbonAwarePeriod;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

@Beta(note = "WIP")
public class CarbonAwareCronExpression extends Schedule {

    private final CronExpression cronExpression;
    private final Duration allowedDurationBefore; //format: PnDTnHnMnS
    private final Duration allowedDurationAfter;

    private CarbonAwareCronExpression(CronExpression cronExpression, Duration allowedDurationBefore, Duration allowedDurationAfter) {
        this.cronExpression = cronExpression;
        this.allowedDurationBefore = allowedDurationBefore;
        this.allowedDurationAfter = allowedDurationAfter;
    }

    public static CarbonAwareCronExpression create(String cronExpression, Duration allowedDurationBefore, Duration allowedDurationAfter) {
        return create(cronExpression + " " + allowedDurationBefore + " " + allowedDurationAfter);
    }

    public static CarbonAwareCronExpression create(CronExpression cronExpression, Duration allowedDurationBefore, Duration allowedDurationAfter) {
        return create(cronExpression.toString() + " " + allowedDurationBefore + " " + allowedDurationAfter);
    }

    public static CarbonAwareCronExpression create(String expression) {
        String[] fields = expression.trim().toLowerCase().split("\\s+");
        if (expression.isEmpty()) {
            throw new InvalidCarbonAwareCronExpressionException("Empty carbon aware cron expression");
        }
        int count = fields.length;
        if (count > 8 || count < 7) {
            throw new InvalidCarbonAwareCronExpressionException(
                    "Carbon aware crontab expression should have 8 fields (seconds resolution) or 7 fields (minutes resolution).\n" +
                            "Also, last 2 fields has to be parseable as java.time.Duration objects (ISO-8601 PnDTnHnMn.nS format)\n" +
                            "Example:\n " +
                            "`0 0 * * * PT2H PT6H`: run every day at midnight. Job can be scheduled 2 hours before and 6 hours after midnight, in the time when carbon emissions are the lowest.");
        }
        Duration allowedBefore;
        Duration allowedAfter;
        try {
            allowedBefore = Duration.parse(fields[count - 2]);
            allowedAfter = Duration.parse(fields[count - 1]);
        } catch (Exception e) {
            throw new InvalidCarbonAwareCronExpressionException("Last 2 fields has to be parseable as java.time.Duration objects (ISO-8601 PnDTnHnMn.nS format)" +
                    "Provided: " + fields[count - 2] + " and " + fields[count - 1] + "\n" +
                    "Valid Duration Examples:\n " +
                    "- PT20.345S represents 20.345 seconds, " +
                    "- PT15M represents 15 minutes, " +
                    "- PT10H represents 10 hours, " +
                    "- P2D represents 2 days, " +
                    "- P2DT3H4M represents 2 days, 3 hours and 4 minutes");
        }

        if (allowedBefore.plus(allowedAfter).toHours() < 3) {
            throw new InvalidCarbonAwareCronExpressionException("Allowed duration before and after must be at least 3 hours, in order to schedule the job at a time when carbon emission from electricity are low. " +
                    "If you need an interval of less than 3 hours, you can use normal recurring job scheduling.\n" +
                    "Provided: " + allowedBefore + " and " + allowedAfter);
        }

        StringBuilder cronExpressionStrBuilder = new StringBuilder();
        for (int i = 0; i < fields.length - 2; i++) {
            cronExpressionStrBuilder.append(fields[i]).append(" ");
        }
        String cronExpressionStr = cronExpressionStrBuilder.toString().trim();
        CronExpression cronExpression = CronExpression.create(cronExpressionStr);
        Instant baseTime = Instant.now();
        Instant nextTime = cronExpression.next(baseTime, baseTime, ZoneId.systemDefault()).getInstant();
        Instant nextNextTime = cronExpression.next(baseTime, nextTime, ZoneId.systemDefault()).getInstant();
        if (Duration.between(nextTime, nextNextTime).compareTo(allowedBefore.plus(allowedAfter)) < 0) {
            throw new InvalidCarbonAwareCronExpressionException("Duration between 2 runs should be equal or greater than (allowedDurationBefore + allowedDurationAfter)\n" +
                    String.format("Provided: %s %s %s\n", cronExpressionStr, allowedBefore, allowedAfter) +
                    "Duration between 2 runs: " + Duration.between(nextTime, nextNextTime));

        }
        return new CarbonAwareCronExpression(cronExpression, allowedBefore, allowedAfter);
    }

    /**
     * Calculates the next occurrence based on provided base time.
     *
     * @param createdAtInstant Instant object based on which calculating the next occurrence.
     * @return {@link TemporalWrapper} object containing the next CarbonAwarePeriod.
     */
    @Override
    public TemporalWrapper next(Instant createdAtInstant, Instant currentInstant, ZoneId zoneId) {
        Instant nextPossibleTime = cronExpression.next(createdAtInstant, currentInstant, zoneId).getInstant();
        Instant earliestStart = nextPossibleTime.minus(allowedDurationBefore);
        Instant latestStart = nextPossibleTime.plus(allowedDurationAfter);
        return TemporalWrapper.ofCarbonAwarePeriod(CarbonAwarePeriod.between(earliestStart, latestStart));
    }

    public Duration getAllowedDurationBefore() {
        return allowedDurationBefore;
    }

    public CronExpression getCronExpression() {
        return cronExpression;
    }

    public Duration getAllowedDurationAfter() {
        return allowedDurationAfter;
    }

    @Override
    public String toString() {
        return cronExpression.toString() + " " + allowedDurationBefore + " " + allowedDurationAfter;
    }

    @Override
    public int hashCode() {
        return cronExpression.hashCode() + allowedDurationBefore.hashCode() + allowedDurationAfter.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CarbonAwareCronExpression))
            return false;
        if (this == obj)
            return true;

        CarbonAwareCronExpression carbonAwareCronExpression = (CarbonAwareCronExpression) obj;

        return this.cronExpression.equals(carbonAwareCronExpression.cronExpression) &&
                this.allowedDurationBefore.equals(carbonAwareCronExpression.allowedDurationBefore) &&
                this.allowedDurationAfter.equals(carbonAwareCronExpression.allowedDurationAfter);
    }

    // TESTS:
    // (allowedDurationBefore + allowedDurationAfter) must be at least 3 hours
    // CarbonAwareCronExpression time between 2 runs must be equal or greater than (allowedDurationBefore + allowedDurationAfter)
}