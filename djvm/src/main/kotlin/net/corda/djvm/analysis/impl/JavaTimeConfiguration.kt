@file:JvmName("JavaTimeConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.CONSTRUCTOR_NAME
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.FROM_DJVM
import net.corda.djvm.references.Member
import org.objectweb.asm.Opcodes.*

private const val OF = "of"

/**
 * Generate [Member] objects for the [sandbox.java.lang.Object.fromDJVM]
 * methods that will be stitched into the [sandbox.java.time] classes.
 */
fun generateJavaTimeMethods(): List<Member> = object : FromDJVMBuilder(
    className = sandboxed(java.time.Duration::class.java),
    bridgeDescriptor = "()Ljava/time/Duration;"
) {
    /**
     * Implements Duration.fromDJVM():
     *     return java.time.Duration.ofSeconds(seconds, nanos)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "seconds", "J")
        pushObject(0)
        pushField(className, "nanos", "I")
        instruction(I2L)
        invokeStatic("java/time/Duration", "ofSeconds", "(JJ)Ljava/time/Duration;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.Instant::class.java),
    bridgeDescriptor = "()Ljava/time/Instant;"
) {
    /**
     * Implements Instant.fromDJVM():
     *     return java.time.Instant.ofEpochSecond(seconds, nanos)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "seconds", "J")
        pushObject(0)
        pushField(className, "nanos", "I")
        instruction(I2L)
        invokeStatic("java/time/Instant", "ofEpochSecond", "(JJ)Ljava/time/Instant;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.LocalDate::class.java),
    bridgeDescriptor = "()Ljava/time/LocalDate;"
) {
    /**
     * Implements LocalDate.fromDJVM():
     *     return java.time.LocalDate.of(year, month, day)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "year", "I")
        pushObject(0)
        pushField(className, "month", "S")
        pushObject(0)
        pushField(className, "day", "S")
        invokeStatic("java/time/LocalDate", OF, "(III)Ljava/time/LocalDate;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.LocalDateTime::class.java),
    bridgeDescriptor = "()Ljava/time/LocalDateTime;"
) {
    /**
     * Implements LocalDateTime.fromDJVM():
     *     return java.time.LocalDateTime.of(date.fromDJVM(), time.fromDJVM())
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "date", "Lsandbox/java/time/LocalDate;")
        invokeVirtual("sandbox/java/time/LocalDate", FROM_DJVM, "()Ljava/time/LocalDate;")
        pushObject(0)
        pushField(className, "time", "Lsandbox/java/time/LocalTime;")
        invokeVirtual("sandbox/java/time/LocalTime", FROM_DJVM, "()Ljava/time/LocalTime;")
        invokeStatic("java/time/LocalDateTime", OF, "(Ljava/time/LocalDate;Ljava/time/LocalTime;)Ljava/time/LocalDateTime;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.LocalTime::class.java),
    bridgeDescriptor = "()Ljava/time/LocalTime;"
) {
    /**
     * Implements LocalTime.fromDJVM():
     *     return java.time.LocalTime.of(hour, minute, second, nano)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "hour", "B")
        pushObject(0)
        pushField(className, "minute", "B")
        pushObject(0)
        pushField(className, "second", "B")
        pushObject(0)
        pushField(className, "nano", "I")
        invokeStatic("java/time/LocalTime", OF, "(IIII)Ljava/time/LocalTime;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.MonthDay::class.java),
    bridgeDescriptor = "()Ljava/time/MonthDay;"
) {
    /**
     * Implements MonthDay.fromDJVM():
     *     return java.time.MonthDay.of(month, day)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "month", "I")
        pushObject(0)
        pushField(className, "day", "I")
        invokeStatic("java/time/MonthDay", OF, "(II)Ljava/time/MonthDay;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.OffsetDateTime::class.java),
    bridgeDescriptor = "()Ljava/time/OffsetDateTime;"
) {
    /**
     * Implements OffsetDateTime.fromDJVM():
     *     return java.time.OffsetDateTime.of(dateTime, offset)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "dateTime", "Lsandbox/java/time/LocalDateTime;")
        invokeVirtual("sandbox/java/time/LocalDateTime", FROM_DJVM, "()Ljava/time/LocalDateTime;")
        pushObject(0)
        pushField(className, "offset", "Lsandbox/java/time/ZoneOffset;")
        invokeVirtual("sandbox/java/time/ZoneOffset", FROM_DJVM, "()Ljava/time/ZoneId;")
        castObjectTo("java/time/ZoneOffset")
        invokeStatic("java/time/OffsetDateTime", OF, "(Ljava/time/LocalDateTime;Ljava/time/ZoneOffset;)Ljava/time/OffsetDateTime;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.OffsetTime::class.java),
    bridgeDescriptor = "()Ljava/time/OffsetTime;"
) {
    /**
     * Implements OffsetTime.fromDJVM():
     *     return java.time.OffsetTime.of(time, offset)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "time", "Lsandbox/java/time/LocalTime;")
        invokeVirtual("sandbox/java/time/LocalTime", FROM_DJVM, "()Ljava/time/LocalTime;")
        pushObject(0)
        pushField(className, "offset", "Lsandbox/java/time/ZoneOffset;")
        invokeVirtual("sandbox/java/time/ZoneOffset", FROM_DJVM, "()Ljava/time/ZoneId;")
        castObjectTo("java/time/ZoneOffset")
        invokeStatic("java/time/OffsetTime", OF, "(Ljava/time/LocalTime;Ljava/time/ZoneOffset;)Ljava/time/OffsetTime;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.Period::class.java),
    bridgeDescriptor = "()Ljava/time/Period;"
) {
    /**
     * Implements Period.fromDJVM():
     *     return java.time.Period.of(years, months, days)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "years", "I")
        pushObject(0)
        pushField(className, "months", "I")
        pushObject(0)
        pushField(className, "days", "I")
        invokeStatic("java/time/Period", OF, "(III)Ljava/time/Period;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.Year::class.java),
    bridgeDescriptor = "()Ljava/time/Year;"
) {
    /**
     * Implements Year.fromDJVM():
     *     return java.time.Year.of(year)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "year", "I")
        invokeStatic("java/time/Year", OF, "(I)Ljava/time/Year;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.YearMonth::class.java),
    bridgeDescriptor = "()Ljava/time/YearMonth;"
) {
    /**
     * Implements YearMonth.fromDJVM():
     *     return java.time.YearMonth.of(year, month)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "year", "I")
        pushObject(0)
        pushField(className, "month", "I")
        invokeStatic("java/time/YearMonth", OF, "(II)Ljava/time/YearMonth;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.ZonedDateTime::class.java),
    bridgeDescriptor = "()Ljava/time/ZonedDateTime;"
) {
    /**
     * Implements ZonedDateTime.fromDJVM():
     *     return sandbox.java.time.DJVM.zonedDateTime(dateTime, offset, zone)
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        pushField(className, "dateTime", "Lsandbox/java/time/LocalDateTime;")
        pushObject(0)
        pushField(className, "offset", "Lsandbox/java/time/ZoneOffset;")
        pushObject(0)
        pushField(className, "zone", "Lsandbox/java/time/ZoneId;")
        invokeStatic("sandbox/java/time/DJVM", "zonedDateTime", "(Lsandbox/java/time/LocalDateTime;Lsandbox/java/time/ZoneOffset;Lsandbox/java/time/ZoneId;)Ljava/time/ZonedDateTime;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.time.ZoneId::class.java),
    bridgeDescriptor = "()Ljava/time/ZoneId;"
) {
    /**
     * Implements ZoneId.fromDJVM():
     *     return java.time.ZoneId.of(sandbox.java.lang.String.fromDJVM(getId()))
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        invokeVirtual(className, "getId", "()Lsandbox/java/lang/String;")
        invokeStatic("sandbox/java/lang/String", FROM_DJVM, "(Lsandbox/java/lang/String;)Ljava/lang/String;")
        invokeStatic("java/time/ZoneId", OF, "(Ljava/lang/String;)Ljava/time/ZoneId;")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.util.Date::class.java),
    bridgeDescriptor = "()Ljava/util/Date;"
) {
    /**
     * Implements Date.fromDJVM():
     *     return java.time.Date(getTime())
     */
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        new("java/util/Date")
        duplicate()
        pushObject(0)
        invokeVirtual(className, "getTime", "()J")
        invokeSpecial("java/util/Date", CONSTRUCTOR_NAME, "(J)V")
        returnObject()
    }
}.build() + listOf(
    /**
     * Create an accessor for [sandbox.java.time.ZonedDateTime.ofLenient]:
     *     return ofLenient(localDateTime, offset, zone)
     */
    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(java.time.ZonedDateTime::class.java),
        memberName = "createDJVM",
        descriptor = "(Lsandbox/java/time/LocalDateTime;Lsandbox/java/time/ZoneOffset;Lsandbox/java/time/ZoneId;)Lsandbox/java/time/ZonedDateTime;"
    ) {
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            pushObject(0)
            pushObject(1)
            pushObject(2)
            invokeStatic(className, "ofLenient", descriptor)
            returnObject()
        }
    }.withBody()
      .build(),
    /**
     * Replace [java.time.zone.TzdbZoneRulesProvider] constructor with one that
     * accepts a [sandbox.java.io.DataInputStream] that will be managed elsewhere.
     */
    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STRICT,
        className = "sandbox/java/time/zone/TzdbZoneRulesProvider",
        memberName = CONSTRUCTOR_NAME,
        descriptor = "(Lsandbox/java/io/DataInputStream;)V",
        exceptions = setOf("java/lang/Exception")
    ) {
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            pushObject(0)
            invokeSpecial("sandbox/java/time/zone/ZoneRulesProvider", CONSTRUCTOR_NAME, "()V")

            // Initialise the regionToRules map field.
            pushObject(0)
            new("sandbox/java/util/concurrent/ConcurrentHashMap")
            duplicate()
            invokeSpecial("sandbox/java/util/concurrent/ConcurrentHashMap", CONSTRUCTOR_NAME, "()V")
            popField(className, "regionToRules", "Lsandbox/java/util/Map;")

            // Read the time-zone data from the input stream.
            pushObject(0)
            pushObject(1)
            invokeSpecial(className, "load", descriptor)
            returnVoid()
        }
    }.withBody()
     .build(),

    /**
     * Delete the original no-argument constructor.
     */
    Member(
        access = ACC_PUBLIC,
        className = "sandbox/java/time/zone/TzdbZoneRulesProvider",
        memberName = CONSTRUCTOR_NAME,
        descriptor = "()V",
        genericsDetails = ""
    ),

    /**
     * Rewrite the native methods that try to determine the system [java.util.TimeZone].
     * Returning null forces Java to use UTC here.
     */
    object : MethodBuilder(
        access = ACC_PRIVATE or ACC_STATIC,
        className = sandboxed(java.util.TimeZone::class.java),
        memberName = "getSystemTimeZoneID",
        descriptor = "(Lsandbox/java/lang/String;)Lsandbox/java/lang/String;"
    ) {
        /**
         * Replace [java.util.TimeZone.getSystemTimeZoneID]:
         *     return null
         */
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            pushNull()
            returnObject()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PRIVATE or ACC_STATIC,
        className = sandboxed(java.util.TimeZone::class.java),
        memberName = "getSystemGMTOffsetID",
        descriptor = "()Lsandbox/java/lang/String;"
    ) {
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            /**
             * Replace [java.util.TimeZone.getSystemGMTOffsetID]:
             *     return null
             */
            pushNull()
            returnObject()
        }
    }.withBody()
     .build()
)
