package net.corda.djvm.execution;

import net.corda.djvm.TaskExecutor;
import net.corda.djvm.TestBase;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.zone.ZoneRulesProvider;
import java.util.TimeZone;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JavaTimeTest extends TestBase {
    JavaTimeTest() {
        super(JAVA);
    }

    private Object run(TaskExecutor executor, Class<?> task, Object data) throws Exception {
        Object toStringTask = executor.toSandboxClass(task).newInstance();
        return executor.execute(toStringTask, data);
    }

    @Test
    void testInstant() {
        Instant instant = Instant.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, instant);
                assertThat(toStringResult).isEqualTo(instant.toString());

                Object identityResult = run(executor, IdentityTransformation.class, instant);
                assertThat(identityResult).isEqualTo(instant);
                assertThat(identityResult).isNotSameAs(instant);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testDuration() {
        Duration duration = Duration.ofHours(2);
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, duration);
                assertThat(toStringResult).isEqualTo(duration.toString());

                Object identityResult = run(executor, IdentityTransformation.class, duration);
                assertThat(identityResult).isEqualTo(duration);
                assertThat(identityResult).isNotSameAs(duration);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testLocalDate() {
        LocalDate localDate = LocalDate.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, localDate);
                assertThat(toStringResult).isEqualTo(localDate.toString());

                Object identityResult = run(executor, IdentityTransformation.class, localDate);
                assertThat(identityResult).isEqualTo(localDate);
                assertThat(identityResult).isNotSameAs(localDate);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testLocalTime() {
        LocalTime localTime = LocalTime.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, localTime);
                assertThat(toStringResult).isEqualTo(localTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, localTime);
                assertThat(identityResult).isEqualTo(localTime);
                assertThat(identityResult).isNotSameAs(localTime);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, localDateTime);
                assertThat(toStringResult).isEqualTo(localDateTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, localDateTime);
                assertThat(identityResult).isEqualTo(localDateTime);
                assertThat(identityResult).isNotSameAs(localDateTime);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testMonthDay() {
        MonthDay monthDay = MonthDay.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, monthDay);
                assertThat(toStringResult).isEqualTo(monthDay.toString());

                Object identityResult = run(executor, IdentityTransformation.class, monthDay);
                assertThat(identityResult).isEqualTo(monthDay);
                assertThat(identityResult).isNotSameAs(monthDay);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testOffsetDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, offsetDateTime);
                assertThat(toStringResult).isEqualTo(offsetDateTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, offsetDateTime);
                assertThat(identityResult).isEqualTo(offsetDateTime);
                assertThat(identityResult).isNotSameAs(offsetDateTime);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testOffsetTime() {
        OffsetTime offsetTime = OffsetTime.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, offsetTime);
                assertThat(toStringResult).isEqualTo(offsetTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, offsetTime);
                assertThat(identityResult).isEqualTo(offsetTime);
                assertThat(identityResult).isNotSameAs(offsetTime);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testPeriod() {
        Period period = Period.of(1, 2, 3);
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, period);
                assertThat(toStringResult).isEqualTo(period.toString());

                Object identityResult = run(executor, IdentityTransformation.class, period);
                assertThat(identityResult).isEqualTo(period);
                assertThat(identityResult).isNotSameAs(period);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testYear() {
        Year year = Year.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, year);
                assertThat(toStringResult).isEqualTo(year.toString());

                Object identityResult = run(executor, IdentityTransformation.class, year);
                assertThat(identityResult).isEqualTo(year);
                assertThat(identityResult).isNotSameAs(year);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testYearMonth() {
        YearMonth yearMonth = YearMonth.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, yearMonth);
                assertThat(toStringResult).isEqualTo(yearMonth.toString());

                Object identityResult = run(executor, IdentityTransformation.class, yearMonth);
                assertThat(identityResult).isEqualTo(yearMonth);
                assertThat(identityResult).isNotSameAs(yearMonth);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testZonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, zonedDateTime);
                assertThat(toStringResult).isEqualTo(zonedDateTime.toString());

                Object identityResult = run(executor, IdentityTransformation.class, zonedDateTime);
                assertThat(identityResult).isEqualTo(zonedDateTime);
                assertThat(identityResult).isNotSameAs(zonedDateTime);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testZoneOffset() {
        ZoneOffset zoneOffset = ZoneOffset.ofHours(7);
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                Object toStringResult = run(executor, TemporalToString.class, zoneOffset);
                assertThat(toStringResult).isEqualTo(zoneOffset.toString());

                Object identityResult = run(executor, IdentityTransformation.class, zoneOffset);
                assertThat(identityResult).isEqualTo(zoneOffset);
                assertThat(identityResult).isSameAs(zoneOffset);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testAllZoneIDs() {
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                String[] zoneIDs = (String[]) run(executor, AllZoneIDs.class, null);
                assertThat(zoneIDs).hasSize(600);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testDefaultZoneID() {
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                String defaultZoneID = (String) run(executor, DefaultZoneId.class, null);
                assertThat(defaultZoneID).isEqualTo("UTC");
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testDefaultTimeZone() {
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                String defaultTimeZone = (String) run(executor, DefaultTimeZone.class, null);
                assertThat(defaultTimeZone).isEqualTo("UTC");
            } catch (Exception e) {
                fail(e);
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

    public static class DefaultZoneId implements Function<Object, String> {
        @Override
        public String apply(Object o) {
            return ZoneId.systemDefault().getId();
        }
    }

    public static class DefaultTimeZone implements Function<Object, String> {
        @Override
        public String apply(Object o) {
            return TimeZone.getDefault().getID();
        }
    }
}
