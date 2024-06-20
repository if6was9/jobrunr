package org.jobrunr.scheduling;

import org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin;
import org.jobrunr.scheduling.cron.Cron;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ScheduleTest {

    @Test
    void createScheduleWithoutCarbonAwareSchedule() {
        Schedule schedule = new MyTestSchedule(Cron.daily());

        assertThat(schedule.getExpression()).isEqualTo(Cron.daily());
        assertThat(schedule.isCarbonAware()).isFalse();
    }

    @Test
    void createScheduleWithCarbonAwareSchedule() {
        Schedule schedule = new MyTestSchedule("0 0 * * * [PT2H/PT0S]");
        CarbonAwareScheduleMargin carbonAwareScheduleMargin = schedule.getCarbonAwareScheduleMargin();

        assertThat(schedule.getExpression()).isEqualTo("0 0 * * *");
        assertThat(schedule.isCarbonAware()).isTrue();
        assertThat(carbonAwareScheduleMargin.getMarginBefore()).isEqualTo(Duration.ofHours(2));
        assertThat(carbonAwareScheduleMargin.getMarginAfter()).isZero();
    }

    @Test
    void createScheduleWithEmptyOrNullExpression() {
        assertThatCode(() -> new MyTestSchedule(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected scheduleWithOptionalCarbonAwareScheduleMargin to be non-null and non-empty.");

        assertThatCode(() -> new MyTestSchedule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected scheduleWithOptionalCarbonAwareScheduleMargin to be non-null and non-empty.");
    }

    @Test
    void canSetCarbonAwareScheduleMargin() {
        Schedule schedule = new MyTestSchedule("0 0 * * *");

        assertThat(schedule.isCarbonAware()).isFalse();

        schedule.setCarbonAwareScheduleMargin(CarbonAwareScheduleMargin.after(Duration.ofHours(5)));

        assertThat(schedule.isCarbonAware()).isTrue();
        assertThat(schedule.getCarbonAwareScheduleMargin().getMarginBefore()).isZero();
        assertThat(schedule.getCarbonAwareScheduleMargin().getMarginAfter()).isEqualTo(Duration.ofHours(5));
    }

    static class MyTestSchedule extends Schedule {
        public MyTestSchedule(String scheduleWithOptionalCarbonAwareScheduleMargin) {
            super(scheduleWithOptionalCarbonAwareScheduleMargin);
        }

        @Override
        public Instant next(Instant createdAtInstant, Instant currentInstant, ZoneId zoneId) {
            return Instant.now();
        }
    }
}