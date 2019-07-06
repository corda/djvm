package net.corda.djvm.execution

import foo.bar.sandbox.MyObject
import foo.bar.sandbox.testClock
import foo.bar.sandbox.toNumber
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.Utilities.*
import net.corda.djvm.analysis.Whitelist.Companion.MINIMAL
import net.corda.djvm.assertions.AssertionExtensions.withProblem
import net.corda.djvm.costing.ThresholdViolationError
import net.corda.djvm.rewiring.SandboxClassLoadingException
import net.corda.djvm.rules.RuleViolationError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function
import java.util.stream.Collectors.*

class SandboxExecutorTest : TestBase(KOTLIN) {

    @Test
    fun `can load and execute runnable`() = sandbox(MINIMAL) {
        val contractExecutor = DeterministicSandboxExecutor<Int, String>(configuration)
        val summary = contractExecutor.run<TestSandboxedRunnable>(1)
        val result = summary.result
        assertThat(result).isEqualTo("sandbox")
    }

    class TestSandboxedRunnable : Function<Int, String> {
        override fun apply(input: Int): String {
            return "sandbox"
        }
    }

    @Test
    fun `can load and execute contract`() = sandbox(DEFAULT, pinnedClasses = setOf(Transaction::class.java)) {
        val contractExecutor = DeterministicSandboxExecutor<Transaction, Unit>(configuration)
        //TODO: Transaction should not be a pinned class! It needs to be marshalled into and out of the sandbox.
        val tx = Transaction(1)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<ContractWrapper>(tx) }
                .withCauseInstanceOf(IllegalArgumentException::class.java)
                .withMessageContaining("Contract constraint violated: txId=${tx.id}")
    }

    interface Contract {
        fun verify(tx: Transaction)
    }

    class ContractImplementation : Contract {
        override fun verify(tx: Transaction) {
            throw IllegalArgumentException("Contract constraint violated: txId=${tx.id}")
        }
    }

    class ContractWrapper : Function<Transaction, Unit> {
        override fun apply(input: Transaction) {
            ContractImplementation().verify(input)
        }
    }

    data class Transaction(val id: Int)

    @Test
    fun `can load and execute code that overrides object hash code`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        val summary = contractExecutor.run<TestObjectHashCode>(0)
        val result = summary.result
        assertThat(result).isEqualTo(0xfed_c0de + 2)
    }

    class TestObjectHashCode : Function<Int, Int> {
        override fun apply(input: Int): Int {
            val obj = Object()
            val hash1 = obj.hashCode()
            val hash2 = obj.hashCode()
            require(hash1 == hash2)
            return Object().hashCode()
        }
    }

    @Test
    fun `can load and execute code that overrides object hash code when derived`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        val summary = contractExecutor.run<TestObjectHashCodeWithHierarchy>(0)
        val result = summary.result
        assertThat(result).isEqualTo(0xfed_c0de + 1)
    }

    class TestObjectHashCodeWithHierarchy : Function<Int, Int> {
        override fun apply(input: Int): Int {
            val obj = MyObject()
            return obj.hashCode()
        }
    }

    @Test
    fun `can detect breached threshold`() = sandbox(DEFAULT, ExecutionProfile.DEFAULT) {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<TestThresholdBreach>(0) }
                .withMessageContaining("terminated due to excessive use of looping")
    }

    class TestThresholdBreach : Function<Int, Int> {
        private var x = 0
        override fun apply(input: Int): Int {
            for (i in 0..1_000_000) {
                x += 1
            }
            return x
        }
    }

    @Test
    fun `can detect stack overflow`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<TestStackOverflow>(0) }
                .withCauseInstanceOf(StackOverflowError::class.java)
    }

    class TestStackOverflow : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return a()
        }

        private fun a(): Int = b()
        private fun b(): Int = a()
    }


    @Test
    fun `can detect illegal references in Kotlin meta-classes`() = sandbox(DEFAULT, ExecutionProfile.DEFAULT) {
        val contractExecutor = DeterministicSandboxExecutor<Int, Long>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<TestKotlinMetaClasses>(0) }
                .withCauseInstanceOf(NoSuchMethodError::class.java)
                .withProblem("sandbox.java.lang.System.nanoTime()J")
    }

    class TestKotlinMetaClasses : Function<Int, Long> {
        override fun apply(input: Int): Long {
            val someNumber = testClock()
            return "12345".toNumber() * someNumber
        }
    }

    @Test
    fun `cannot execute runnable that references non-deterministic code`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Long>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<TestNonDeterministicCode>(0) }
                .withCauseInstanceOf(NoSuchMethodError::class.java)
                .withProblem("sandbox.java.lang.System.currentTimeMillis()J")
    }

    class TestNonDeterministicCode : Function<Int, Long> {
        override fun apply(input: Int): Long {
            return System.currentTimeMillis()
        }
    }

    @Test
    fun `cannot execute runnable that catches ThreadDeath`() = parentedSandbox {
        assertThat(TestCatchThreadDeath().apply(0))
            .isEqualTo(1)

        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
            .isThrownBy { contractExecutor.run<TestCatchThreadDeath>(0) }
            .withCauseExactlyInstanceOf(ThreadDeath::class.java)
    }

    class TestCatchThreadDeath : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return try {
                throw ThreadDeath()
            } catch (exception: ThreadDeath) {
                1
            }
        }
    }

    @Test
    fun `cannot execute runnable that catches ThresholdViolationError`() = parentedSandbox {
        assertThat(TestCatchThresholdViolationError().apply(0))
            .isEqualTo(1)

        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
            .isThrownBy { contractExecutor.run<TestCatchThresholdViolationError>(0) }
            .withCauseExactlyInstanceOf(ThresholdViolationError::class.java)
            .withMessageContaining("Can't catch this!")
    }

    class TestCatchThresholdViolationError : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return try {
                throwThresholdViolationError()
                Int.MIN_VALUE // Should not reach here
            } catch (exception: Error) {
                1
            }
        }
    }

    @Test
    fun `cannot execute runnable that catches RuleViolationError`() = parentedSandbox {
        assertThat(TestCatchRuleViolationError().apply(0))
            .isEqualTo(1)

        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
            .isThrownBy { contractExecutor.run<TestCatchRuleViolationError>(0) }
            .withCauseExactlyInstanceOf(RuleViolationError::class.java)
            .withMessageContaining("Can't catch this!")
    }

    class TestCatchRuleViolationError : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return try {
                throwRuleViolationError()
                Int.MIN_VALUE // Should not reach here
            } catch (exception: ThreadDeath) {
                1
            }
        }
    }

    @Test
    fun `can catch Throwable`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        contractExecutor.run<TestCatchThrowableAndError>(1).apply {
            assertThat(result).isEqualTo(1)
        }
    }

    @Test
    fun `can catch Error`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        contractExecutor.run<TestCatchThrowableAndError>(2).apply {
            assertThat(result).isEqualTo(2)
        }
    }

    @Test
    fun `cannot catch ThreadDeath`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<TestCatchThrowableErrorsAndThreadDeath>(3) }
                .withCauseInstanceOf(ThreadDeath::class.java)
    }

    class TestCatchThrowableAndError : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return try {
                when (input) {
                    1 -> throw Throwable()
                    2 -> throw Error()
                    else -> 0
                }
            } catch (exception: Error) {
                2
            } catch (exception: Throwable) {
                1
            }
        }
    }

    class TestCatchThrowableErrorsAndThreadDeath : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return try {
                when (input) {
                    1 -> throw Throwable()
                    2 -> throw Error()
                    3 -> try {
                        throw ThreadDeath()
                    } catch (ex: ThreadDeath) {
                        3
                    }
                    4 -> try {
                        throw StackOverflowError("FAKE OVERFLOW!")
                    } catch (ex: StackOverflowError) {
                        4
                    }
                    5 -> try {
                        throw OutOfMemoryError("FAKE OOM!")
                    } catch (ex: OutOfMemoryError) {
                        5
                    }
                    else -> 0
                }
            } catch (exception: Error) {
                2
            } catch (exception: Throwable) {
                1
            }
        }
    }

    @Test
    fun `cannot catch stack-overflow error`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<TestCatchThrowableErrorsAndThreadDeath>(4) }
                .withCauseInstanceOf(StackOverflowError::class.java)
                .withMessageContaining("FAKE OVERFLOW!")
    }

    @Test
    fun `cannot catch out-of-memory error`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<TestCatchThrowableErrorsAndThreadDeath>(5) }
                .withCauseInstanceOf(OutOfMemoryError::class.java)
                .withMessageContaining("FAKE OOM!")
    }

    @Test
    fun `cannot persist state across sessions`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        val result1 = contractExecutor.run<TestStatePersistence>(0)
        val result2 = contractExecutor.run<TestStatePersistence>(0)
        assertThat(result1.result)
                .isEqualTo(result2.result)
                .isEqualTo(1)
    }

    class TestStatePersistence : Function<Int, Int> {
        override fun apply(input: Int): Int {
            ReferencedClass.value += 1
            return ReferencedClass.value
        }
    }

    object ReferencedClass {
        @JvmField
        var value = 0
    }

    @Test
    fun `can load and execute code that uses IO`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
            .isThrownBy { contractExecutor.run<TestIO>("test.dat") }
            .withCauseInstanceOf(SandboxClassLoadingException::class.java)
            .withMessageContaining("Class file not found; java/nio/file/Paths.class")
    }

    class TestIO : Function<String, Int> {
        override fun apply(input: String): Int {
            val file = Paths.get(input)
            Files.newBufferedWriter(file).use {
                it.write("Hello world!")
            }
            return 0
        }
    }

    @Test
    fun `can load and execute code that uses reflection`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        val ex = assertThrows<SandboxException>{ contractExecutor.run<TestReflection>(0) }
        assertThat(ex)
            .isExactlyInstanceOf(SandboxException::class.java)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Class.getMethods()")
        assertThat(ex.cause).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    class TestReflection : Function<Int, Int> {
        override fun apply(input: Int): Int {
            val clazz = Object::class.java
            val obj = clazz.newInstance()
            val result = clazz.methods.first().invoke(obj)
            return obj.hashCode() + result.hashCode()
        }
    }

    @Test
    fun `can load and execute code that uses notify()`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String?>(configuration)
        val ex = assertThrows<SandboxException>{ contractExecutor.run<TestMonitors>(1) }
        assertThat(ex)
            .isExactlyInstanceOf(SandboxException::class.java)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.notify()")
        assertThat(ex.cause).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses notifyAll()`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String?>(configuration)
        val ex = assertThrows<SandboxException>{ contractExecutor.run<TestMonitors>(2) }
        assertThat(ex)
            .isExactlyInstanceOf(SandboxException::class.java)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.notifyAll()")
        assertThat(ex.cause).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses wait()`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String?>(configuration)
        val ex = assertThrows<SandboxException>{ contractExecutor.run<TestMonitors>(3) }
        assertThat(ex)
            .isExactlyInstanceOf(SandboxException::class.java)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.wait()")
        assertThat(ex.cause).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses wait(long)`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String?>(configuration)
        val ex = assertThrows<SandboxException>{ contractExecutor.run<TestMonitors>(4) }
        assertThat(ex)
            .isExactlyInstanceOf(SandboxException::class.java)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.wait(Long)")
        assertThat(ex.cause).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses wait(long,int)`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String?>(configuration)
        val ex = assertThrows<SandboxException>{ contractExecutor.run<TestMonitors>(5) }
        assertThat(ex)
            .isExactlyInstanceOf(SandboxException::class.java)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.wait(Long, Integer)")
        assertThat(ex.cause).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `code after forbidden APIs is intact`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String?>(configuration)
        assertThat(contractExecutor.run<TestMonitors>(0).result)
                .isEqualTo("unknown")
    }

    class TestMonitors : Function<Int, String?> {
        override fun apply(input: Int): String? {
            return synchronized(this) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                val javaObject = this as Object
                when(input) {
                    1 -> {
                        javaObject.notify()
                        "notify"
                    }
                    2 -> {
                        javaObject.notifyAll()
                        "notifyAll"
                    }
                    3 -> {
                        javaObject.wait()
                        "wait"
                    }
                    4 -> {
                        javaObject.wait(100)
                        "wait(100)"
                    }
                    5 -> {
                        javaObject.wait(100, 10)
                        "wait(100, 10)"
                    }
                    else -> "unknown"
                }
            }
        }
    }

    @Test
    fun `can load and execute code that has a native method`() = parentedSandbox {
        assertThatExceptionOfType(UnsatisfiedLinkError::class.java)
            .isThrownBy { TestNativeMethod().apply(0) }
            .withMessageContaining("TestNativeMethod.evilDeeds()I")

        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
            .isThrownBy { contractExecutor.run<TestNativeMethod>(0) }
            .withCauseInstanceOf(RuleViolationError::class.java)
            .withMessageContaining("Native method has been deleted")
    }

    class TestNativeMethod : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return evilDeeds()
        }

        private external fun evilDeeds(): Int
    }

    @Test
    fun `check arrays still work`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Array<Int>>(configuration)
        contractExecutor.run<TestArray>(100).apply {
            assertThat(result).isEqualTo(arrayOf(100))
        }
    }

    class TestArray : Function<Int, Array<Int>> {
        override fun apply(input: Int): Array<Int> {
            return listOf(input).toTypedArray()
        }
    }

    @Test
    fun `check building a string`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String?, String?>(configuration)
        contractExecutor.run<TestStringBuilding>("Hello Sandbox!").apply {
            assertThat(result)
                .isEqualTo("SANDBOX: Boolean=true, Char='X', Integer=1234, Long=99999, Short=3200, Byte=101, String='Hello Sandbox!', Float=123.456, Double=987.6543")
        }
    }

    class TestStringBuilding : Function<String?, String?> {
        override fun apply(input: String?): String? {
             return StringBuilder("SANDBOX")
                    .append(": Boolean=").append(true)
                    .append(", Char='").append('X')
                    .append("', Integer=").append(1234)
                    .append(", Long=").append(99999L)
                    .append(", Short=").append(3200.toShort())
                    .append(", Byte=").append(101.toByte())
                    .append(", String='").append(input)
                    .append("', Float=").append(123.456f)
                    .append(", Double=").append(987.6543)
                    .toString()
        }
    }

    @Test
    fun `check System-arraycopy still works with Objects`() = parentedSandbox {
        val source = arrayOf("one", "two", "three")
        assertThat(TestArrayCopy().apply(source))
            .isEqualTo(source)
            .isNotSameAs(source)

        val contractExecutor = DeterministicSandboxExecutor<Array<String>, Array<String>>(configuration)
        contractExecutor.run<TestArrayCopy>(source).apply {
            assertThat(result)
                .isEqualTo(source)
                .isNotSameAs(source)
        }
    }

    class TestArrayCopy : Function<Array<String>, Array<String>> {
        override fun apply(input: Array<String>): Array<String> {
            val newArray = Array(input.size) { "" }
            System.arraycopy(input, 0, newArray, 0, newArray.size)
            return newArray
        }
    }

    @Test
    fun `test System-arraycopy still works with CharArray`() = parentedSandbox {
        val source = CharArray(10) { '?' }
        val contractExecutor = DeterministicSandboxExecutor<CharArray, CharArray>(configuration)
        contractExecutor.run<TestCharArrayCopy>(source).apply {
            assertThat(result)
                .isEqualTo(source)
                .isNotSameAs(source)
        }
    }

    class TestCharArrayCopy : Function<CharArray, CharArray> {
        override fun apply(input: CharArray): CharArray {
            val newArray = CharArray(input.size) { 'X' }
            System.arraycopy(input, 0, newArray, 0, newArray.size)
            return newArray
        }
    }

    @Test
    fun `can load and execute class that has finalize`() = parentedSandbox {
        assertThatExceptionOfType(UnsupportedOperationException::class.java)
            .isThrownBy { TestFinalizeMethod().apply(100) }
            .withMessageContaining("Very Bad Thing")

        val contractExecutor = DeterministicSandboxExecutor<Int, Int>(configuration)
        contractExecutor.run<TestFinalizeMethod>(100).apply {
            assertThat(result).isEqualTo(100)
        }
    }

    class TestFinalizeMethod : Function<Int, Int> {
        override fun apply(input: Int): Int {
            finalize()
            return input
        }

        private fun finalize() {
            throw UnsupportedOperationException("Very Bad Thing")
        }
    }

    @Test
    fun `can execute parallel stream`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, String>(configuration)
        contractExecutor.run<TestParallelStream>("Pebble").apply {
            assertThat(result).isEqualTo("Five,Four,One,Pebble,Three,Two")
        }
    }

    class TestParallelStream : Function<String, String> {
        override fun apply(input: String): String {
            return listOf(input, "One", input, "Two", input, "Three", input, "Four", input, "Five")
                    .stream()
                    .distinct()
                    .sorted()
                    .collect(joining(","))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "java.io.DJVMInputStream",
        "java.lang.DJVM",
        "java.lang.DJVMException",
        "java.lang.DJVMNoResource",
        "java.lang.DJVMResourceKey",
        "java.lang.DJVMThrowableWrapper",
        "java.util.concurrent.atomic.DJVM",
        "RuntimeCostAccounter",
        "TaskTypes",
        "Task"
    ])
    fun `users cannot load our sandboxed classes`(className: String) = parentedSandbox {
        // Show the class exists to be found.
        assertThat(Class.forName("sandbox.$className")).isNotNull

        // Show the class cannot be loaded from the sandbox.
        val contractExecutor = DeterministicSandboxExecutor<String, Class<*>>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
            .isThrownBy { contractExecutor.run<TestClassForName>(className) }
            .withCauseInstanceOf(ClassNotFoundException::class.java)
            .withMessageContaining(className)
    }

    @Test
    fun `users can load sandboxed classes`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Class<*>>(configuration)
        contractExecutor.run<TestClassForName>("java.util.List").apply {
            assertThat(result?.name).isEqualTo("sandbox.java.util.List")
        }
    }

    class TestClassForName : Function<String, Class<*>> {
        override fun apply(input: String): Class<*> {
            return Class.forName(input)
        }
    }

    @Test
    fun `test case-insensitive string sorting`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Array<String>, Array<String>>(configuration)
        contractExecutor.run<CaseInsensitiveSort>(arrayOf("Zelda", "angela", "BOB", "betsy", "ALBERT")).apply {
            assertThat(result).isEqualTo(arrayOf("ALBERT", "angela", "betsy", "BOB", "Zelda"))
        }
    }

    class CaseInsensitiveSort : Function<Array<String>, Array<String>> {
        override fun apply(input: Array<String>): Array<String> {
            return listOf(*input).sortedWith(String.CASE_INSENSITIVE_ORDER).toTypedArray()
        }
    }

    @Test
    fun `test unicode characters`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String>(configuration)
        contractExecutor.run<ExamineUnicodeBlock>(0x01f600).apply {
            assertThat(result).isEqualTo("EMOTICONS")
        }
    }

    class ExamineUnicodeBlock : Function<Int, String> {
        override fun apply(codePoint: Int): String {
            return Character.UnicodeBlock.of(codePoint).toString()
        }
    }

    @Test
    fun `test unicode scripts`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Character.UnicodeScript?>(configuration)
        contractExecutor.run<ExamineUnicodeScript>("COMMON").apply {
            assertThat(result).isEqualTo(Character.UnicodeScript.COMMON)
        }
    }

    class ExamineUnicodeScript : Function<String, Character.UnicodeScript?> {
        override fun apply(scriptName: String): Character.UnicodeScript? {
            val script = Character.UnicodeScript.valueOf(scriptName)
            return if (script::class.java.isEnum) script else null
        }
    }

    @Test
    fun `test users cannot define new classes`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Class<*>>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<DefineNewClass>("sandbox.java.lang.DJVM") }
                .withCauseInstanceOf(RuleViolationError::class.java)
                .withMessageContaining("Disallowed reference to API;")
                .withMessageContaining("java.lang.ClassLoader.defineClass")
    }

    class DefineNewClass : Function<String, Class<*>> {
        override fun apply(input: String): Class<*> {
            val data = ByteArray(0)
            val cl = object : ClassLoader() {
                fun define(): Class<*> {
                    return super.defineClass(input, data, 0, data.size)
                }
            }
            return cl.define()
        }
    }

    @Test
    fun `test users cannot load new classes`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Class<*>>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<LoadNewClass>("sandbox.java.lang.DJVM") }
                .withCauseInstanceOf(RuleViolationError::class.java)
                .withMessageContaining("Disallowed reference to API;")
                .withMessageContaining("java.lang.ClassLoader.loadClass")
    }

    class LoadNewClass : Function<String, Class<*>> {
        override fun apply(input: String): Class<*> {
            val cl = object : ClassLoader() {
                fun load(): Class<*> {
                    return super.loadClass(input)
                }
            }
            return cl.load()
        }
    }

    @Test
    fun `test users cannot lookup classes`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Class<*>>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
                .isThrownBy { contractExecutor.run<FindClass>("sandbox.java.lang.DJVM") }
                .withCauseInstanceOf(RuleViolationError::class.java)
                .withMessageContaining("Disallowed reference to API;")
                .withMessageContaining("java.lang.ClassLoader.findClass")
    }

    class FindClass : Function<String, Class<*>> {
        override fun apply(input: String): Class<*> {
            val cl = object : ClassLoader() {
                fun find(): Class<*> {
                    return super.findClass(input)
                }
            }
            return cl.find()
        }
    }

    @Test
    fun `test users cannot load system resources`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Boolean>(configuration)
        contractExecutor.run<GetSystemResources>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isFalse()
        }
    }

    class GetSystemResources : Function<String, Boolean> {
        override fun apply(resourceName: String): Boolean {
            return ClassLoader.getSystemResources(resourceName).hasMoreElements()
        }
    }

    @Test
    fun `test users cannot load system resource URL`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, String?>(configuration)
        contractExecutor.run<GetSystemResourceURL>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetSystemResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemResource(resourceName)?.path
        }
    }

    @Test
    fun `test users cannot load system resource stream`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Int?>(configuration)
        contractExecutor.run<GetSystemResourceStream>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetSystemResourceStream : Function<String, Int?> {
        override fun apply(resourceName: String): Int? {
            return ClassLoader.getSystemResourceAsStream(resourceName)?.available()
        }
    }

    @Test
    fun `test users cannot load resources`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Boolean>(configuration)
        contractExecutor.run<GetResources>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isFalse()
        }
    }

    class GetResources : Function<String, Boolean> {
        override fun apply(resourceName: String): Boolean {
            return ClassLoader.getSystemClassLoader().getResources(resourceName).hasMoreElements()
        }
    }

    @Test
    fun `test users cannot load resource URL`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, String?>(configuration)
        contractExecutor.run<GetResourceURL>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemClassLoader().getResource(resourceName)?.path
        }
    }

    @Test
    fun `test users cannot load resource stream`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, Int?>(configuration)
        contractExecutor.run<GetResourceStream>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetResourceStream : Function<String, Int?> {
        override fun apply(resourceName: String): Int? {
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)?.available()
        }
    }

    @Test
    fun `test creating arrays of arrays`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Any, Array<Any>>(configuration)
        contractExecutor.run<ArraysOfArrays>("THINGY").apply {
            assertThat(result).isEqualTo(arrayOf(arrayOf("THINGY")))
        }
    }

    class ArraysOfArrays : Function<Any, Array<Any>> {
        override fun apply(input: Any): Array<Any> {
            return arrayOf(arrayOf(input))
        }
    }

    @Test
    fun `test creating arrays of int arrays`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, Array<IntArray>>(configuration)
        contractExecutor.run<ArrayOfIntArrays>(0).apply {
            assertThat(result).isEqualTo(arrayOf(intArrayOf(0)))
        }
    }

    class ArrayOfIntArrays : Function<Int, Array<IntArray>> {
        override fun apply(input: Int): Array<IntArray> {
            return arrayOf(intArrayOf(input))
        }
    }

    @Test
    fun `test class with protection domain`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Int, String>(configuration)
        assertThatExceptionOfType(SandboxException::class.java)
            .isThrownBy { contractExecutor.run<AccessClassProtectionDomain>(0) }
            .withCauseInstanceOf(RuleViolationError::class.java)
            .withMessageContaining("Disallowed reference to API;")
            .withMessageContaining("java.lang.Class.getProtectionDomain")
    }

    class AccessClassProtectionDomain : Function<Int, String> {
        override fun apply(input: Int): String {
            return input::class.java.protectionDomain.codeSource.toString()
        }
    }
}
