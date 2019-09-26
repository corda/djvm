package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.zone.ZoneRulesProvider;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JavaTimeTest extends TestBase {
    private static final long OFFSET_SECONDS = 5000L;

    JavaTimeTest() {
        super(JAVA);
    }

    @Test
    void testInstant() {
        Instant instant = Instant.now();
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super Instant, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(instant);
                assertThat(toStringResult).isEqualTo(instant.toString());

                Function<? super Instant, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(instant);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super Duration, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(duration);
                assertThat(toStringResult).isEqualTo(duration.toString());

                Function<? super Duration, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(duration);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super LocalDate, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(localDate);
                assertThat(toStringResult).isEqualTo(localDate.toString());

                Function<? super LocalDate, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(localDate);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super LocalTime, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(localTime);
                assertThat(toStringResult).isEqualTo(localTime.toString());

                Function<? super LocalTime, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(localTime);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super LocalDateTime, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(localDateTime);
                assertThat(toStringResult).isEqualTo(localDateTime.toString());

                Function<? super LocalDateTime, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(localDateTime);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super MonthDay, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(monthDay);
                assertThat(toStringResult).isEqualTo(monthDay.toString());

                Function<? super MonthDay, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(monthDay);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super OffsetDateTime, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(offsetDateTime);
                assertThat(toStringResult).isEqualTo(offsetDateTime.toString());

                Function<? super OffsetDateTime, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(offsetDateTime);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super OffsetTime, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(offsetTime);
                assertThat(toStringResult).isEqualTo(offsetTime.toString());

                Function<? super OffsetTime, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(offsetTime);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super Period, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(period);
                assertThat(toStringResult).isEqualTo(period.toString());

                Function<? super Period, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(period);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super Year, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(year);
                assertThat(toStringResult).isEqualTo(year.toString());

                Function<? super Year, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(year);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super YearMonth, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(yearMonth);
                assertThat(toStringResult).isEqualTo(yearMonth.toString());

                Function<? super YearMonth, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(yearMonth);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super ZonedDateTime, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(zonedDateTime);
                assertThat(toStringResult).isEqualTo(zonedDateTime.toString());

                Function<? super ZonedDateTime, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(zonedDateTime);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super ZoneOffset, String> stringTask = typedTaskFor(ctx.getClassLoader(), taskFactory, TemporalToString.class);
                String toStringResult = stringTask.apply(zoneOffset);
                assertThat(toStringResult).isEqualTo(zoneOffset.toString());

                Function<? super ZoneOffset, ?> identityTask = typedTaskFor(ctx.getClassLoader(), taskFactory, IdentityTransformation.class);
                Object identityResult = identityTask.apply(zoneOffset);
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
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<?, String[]> allZoneIDs = typedTaskFor(ctx.getClassLoader(), taskFactory, AllZoneIDs.class);
                String[] zoneIDs = allZoneIDs.apply(null);
                assertThat(zoneIDs).hasSize(600);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testDefaultZoneID() {
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<?, String> defaultZoneIdTask = typedTaskFor(ctx.getClassLoader(), taskFactory, DefaultZoneId.class);
                String defaultZoneID = defaultZoneIdTask.apply(null);
                assertThat(defaultZoneID).isEqualTo("UTC");
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testDefaultTimeZone() {
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<?, String> defaultTimeZoneTask = typedTaskFor(ctx.getClassLoader(), taskFactory, DefaultTimeZone.class);
                String defaultTimeZone = defaultTimeZoneTask.apply(null);
                assertThat(defaultTimeZone).isEqualTo("Coordinated Universal Time");
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testDate() {
        Date now = new Date();

        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<Date, String> showDate = typedTaskFor(ctx.getClassLoader(), taskFactory, ShowDate.class);
                String result = showDate.apply(now);
                assertThat(result).isEqualTo(now.toString());
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testReturningDate() {
        Date now = new Date();
        Date later = new AddToDate().apply(now);

        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<Date, Date> addToDate = typedTaskFor(ctx.getClassLoader(), taskFactory, AddToDate.class);
                Date result = addToDate.apply(now);
                assertNotSame(later, result);
                assertEquals(later, result);
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
            return TimeZone.getDefault().getDisplayName();
        }
    }

    public static class ShowDate implements Function<Date, String> {
        @Override
        public String apply(Date date) {
            return date.toString();
        }
    }

    public static class AddToDate implements Function<Date, Date> {
        @Override
        public Date apply(Date date) {
            return new Date(date.getTime() + OFFSET_SECONDS);
        }
    }
}
