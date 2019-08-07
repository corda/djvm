package net.corda.djvm.execution;

import net.corda.djvm.TaskExecutor;
import net.corda.djvm.TestBase;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.zone.ZoneRulesProvider;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JavaTimeTest extends TestBase {
    JavaTimeTest() {
        super(JAVA);
    }

    private Object run(TaskExecutor executor, Class<?> task, Object data) throws Throwable {
        Object toStringTask = executor.toSandboxClass(task).newInstance();
        return executor.execute(toStringTask, data);
    }

    @Test
    void testInstant() {
        Instant instant = Instant.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, instant);
                assertThat(toStringResult).isEqualTo(instant.toString());

                Object identityResult = run(executor, IdentityTransformation.class, instant);
                assertThat(identityResult).isEqualTo(instant);
                assertThat(identityResult).isNotSameAs(instant);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testDuration() {
        Duration duration = Duration.ofHours(2);
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, duration);
                assertThat(toStringResult).isEqualTo(duration.toString());

                Object identityResult = run(executor, IdentityTransformation.class, duration);
                assertThat(identityResult).isEqualTo(duration);
                assertThat(identityResult).isNotSameAs(duration);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testLocalDate() {
        LocalDate localDate = LocalDate.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, localDate);
                assertThat(toStringResult).isEqualTo(localDate.toString());

                Object identityResult = run(executor, IdentityTransformation.class, localDate);
                assertThat(identityResult).isEqualTo(localDate);
                assertThat(identityResult).isNotSameAs(localDate);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testLocalTime() {
        LocalTime localTime = LocalTime.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, localTime);
                assertThat(toStringResult).isEqualTo(localTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, localTime);
                assertThat(identityResult).isEqualTo(localTime);
                assertThat(identityResult).isNotSameAs(localTime);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, localDateTime);
                assertThat(toStringResult).isEqualTo(localDateTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, localDateTime);
                assertThat(identityResult).isEqualTo(localDateTime);
                assertThat(identityResult).isNotSameAs(localDateTime);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testMonthDay() {
        MonthDay monthDay = MonthDay.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, monthDay);
                assertThat(toStringResult).isEqualTo(monthDay.toString());

                Object identityResult = run(executor, IdentityTransformation.class, monthDay);
                assertThat(identityResult).isEqualTo(monthDay);
                assertThat(identityResult).isNotSameAs(monthDay);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testOffsetDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, offsetDateTime);
                assertThat(toStringResult).isEqualTo(offsetDateTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, offsetDateTime);
                assertThat(identityResult).isEqualTo(offsetDateTime);
                assertThat(identityResult).isNotSameAs(offsetDateTime);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testOffsetTime() {
        OffsetTime offsetTime = OffsetTime.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, offsetTime);
                assertThat(toStringResult).isEqualTo(offsetTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, offsetTime);
                assertThat(identityResult).isEqualTo(offsetTime);
                assertThat(identityResult).isNotSameAs(offsetTime);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testPeriod() {
        Period period = Period.of(1, 2, 3);
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, period);
                assertThat(toStringResult).isEqualTo(period.toString());

                Object identityResult = run(executor, IdentityTransformation.class, period);
                assertThat(identityResult).isEqualTo(period);
                assertThat(identityResult).isNotSameAs(period);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testYear() {
        Year year = Year.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, year);
                assertThat(toStringResult).isEqualTo(year.toString());

                Object identityResult = run(executor, IdentityTransformation.class, year);
                assertThat(identityResult).isEqualTo(year);
                assertThat(identityResult).isNotSameAs(year);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testYearMonth() {
        YearMonth yearMonth = YearMonth.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, yearMonth);
                assertThat(toStringResult).isEqualTo(yearMonth.toString());

                Object identityResult = run(executor, IdentityTransformation.class, yearMonth);
                assertThat(identityResult).isEqualTo(yearMonth);
                assertThat(identityResult).isNotSameAs(yearMonth);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testZonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, zonedDateTime);
                assertThat(toStringResult).isEqualTo(zonedDateTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, zonedDateTime);
                assertThat(identityResult).isEqualTo(zonedDateTime);
                assertThat(identityResult).isNotSameAs(zonedDateTime);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testZoneOffset() {
        ZoneOffset zoneOffset = ZoneOffset.ofHours(7);
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, zoneOffset);
                assertThat(toStringResult).isEqualTo(zoneOffset.toString());

                Object identityResult = run(executor, IdentityTransformation.class, zoneOffset);
                assertThat(identityResult).isEqualTo(zoneOffset);
                assertThat(identityResult).isSameAs(zoneOffset);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    @Test
    void testAllZoneIDs() {
        parentedSandbox(WARNING, true, ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                String[] zoneIDs = (String[]) run(executor, AllZoneIDs.class, null);
                assertThat(zoneIDs).hasSize(600);
            } catch (Throwable t) {
                fail(t);
            }
            return null;
        });
    }

    public static class TemporalToString implements Function<Object, String> {
        @Override
        public String apply(Object temporal) {
            return temporal.toString();
        }
    }

    public static class IdentityTransformation implements Function<Object, Object> {
        @Override
        public Object apply(Object temporal) {
            return temporal;
        }
    }

    public static class AllZoneIDs implements Function<Object, String[]> {
        @Override
        public String[] apply(Object o) {
            return ZoneRulesProvider.getAvailableZoneIds().toArray(new String[0]);
        }
    }
}
