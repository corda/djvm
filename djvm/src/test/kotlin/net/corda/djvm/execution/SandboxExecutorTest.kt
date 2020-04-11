package net.corda.djvm.execution

import foo.bar.sandbox.MyObject
import foo.bar.sandbox.testClock
import foo.bar.sandbox.toNumber
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.Utilities.throwRuleViolationError
import net.corda.djvm.Utilities.throwThresholdViolationError
import net.corda.djvm.analysis.Whitelist.Companion.MINIMAL
import net.corda.djvm.costing.ThresholdViolationError
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
import java.util.stream.Collectors.joining

class SandboxExecutorTest : TestBase(KOTLIN) {
    companion object {
        const val TX_ID = 1
    }

    @Test
    fun `can load and execute runnable`() = customSandbox(MINIMAL) {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestSandboxedRunnable::class.java).apply(1)
        assertThat(result).isEqualTo("sandbox")
    }

    class TestSandboxedRunnable : Function<Int, String> {
        override fun apply(input: Int): String {
            return "sandbox"
        }
    }

    @Test
    fun `can load and execute contract`() = sandbox {
        val taskFactory = classLoader.createRawTaskFactory()
        val verifyTask = taskFactory.compose(classLoader.createSandboxFunction()).apply(ContractWrapper::class.java)
        val sandboxClass = classLoader.toSandboxClass(Transaction::class.java)
        val sandboxTx = sandboxClass.getDeclaredConstructor(Integer.TYPE).newInstance(TX_ID)
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { verifyTask.apply(sandboxTx) }
            .withMessageContaining("Contract constraint violated: txId=$TX_ID")
            .withNoCause()
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
    fun `can load and execute code that overrides object hash code`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestObjectHashCode::class.java).apply(0)
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
    fun `can load and execute code that overrides object hash code when derived`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestObjectHashCodeWithHierarchy::class.java).apply(0)
        assertThat(result).isEqualTo(0xfed_c0de + 1)
    }

    class TestObjectHashCodeWithHierarchy : Function<Int, Int> {
        override fun apply(input: Int): Int {
            val obj = MyObject()
            return obj.hashCode()
        }
    }

    @Test
    fun `can detect breached threshold`() = customSandbox(DEFAULT, ExecutionProfile.DEFAULT) {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(ThresholdViolationError::class.java)
            .isThrownBy { taskFactory.create(TestThresholdBreach::class.java).apply(0) }
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
    fun `can detect stack overflow`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(StackOverflowError::class.java)
            .isThrownBy { taskFactory.create(TestStackOverflow::class.java).apply(0) }
    }

    class TestStackOverflow : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return a()
        }

        private fun a(): Int = b()
        private fun b(): Int = a()
    }


    @Test
    fun `can detect illegal references in Kotlin meta-classes`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(NoSuchMethodError::class.java)
            .isThrownBy { taskFactory.create(TestKotlinMetaClasses::class.java).apply(0) }
            .withMessageContaining("sandbox.java.lang.System.nanoTime()")
            .withMessageMatching(".*(long sandbox\\.|\\.nanoTime\\(\\)J)+.*")
    }

    class TestKotlinMetaClasses : Function<Int, Long> {
        override fun apply(input: Int): Long {
            val someNumber = testClock()
            return "12345".toNumber() * someNumber
        }
    }

    @Test
    fun `cannot execute runnable that references non-deterministic code`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(NoSuchMethodError::class.java)
            .isThrownBy { taskFactory.create(TestNonDeterministicCode::class.java).apply(0) }
            .withMessageContaining("sandbox.java.lang.System.currentTimeMillis()")
            .withMessageMatching(".*(long sandbox\\.|\\.currentTimeMillis\\(\\)J)+.*")
    }

    class TestNonDeterministicCode : Function<Int, Long> {
        override fun apply(input: Int): Long {
            return System.currentTimeMillis()
        }
    }

    @Test
    fun `cannot execute runnable that catches ThreadDeath`() = sandbox {
        assertThat(TestCatchThreadDeath().apply(0))
            .isEqualTo(1)

        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(ThreadDeath::class.java)
            .isThrownBy { taskFactory.create(TestCatchThreadDeath::class.java).apply(0) }
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
    fun `cannot execute runnable that catches ThresholdViolationError`() = sandbox {
        assertThat(TestCatchThresholdViolationError().apply(0))
            .isEqualTo(1)

        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(ThresholdViolationError::class.java)
            .isThrownBy { taskFactory.create(TestCatchThresholdViolationError::class.java).apply(0) }
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
    fun `cannot execute runnable that catches RuleViolationError`() = sandbox {
        assertThat(TestCatchRuleViolationError().apply(0))
            .isEqualTo(1)

        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(RuleViolationError::class.java)
            .isThrownBy { taskFactory.create(TestCatchRuleViolationError::class.java).apply(0) }
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
    fun `can catch Throwable`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestCatchThrowableAndError::class.java).apply(1)
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `can catch Error`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestCatchThrowableAndError::class.java).apply(2)
        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `cannot catch ThreadDeath`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(ThreadDeath::class.java)
            .isThrownBy { taskFactory.create(TestCatchThrowableErrorsAndThreadDeath::class.java).apply(3) }
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
    fun `cannot catch stack-overflow error`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(StackOverflowError::class.java)
            .isThrownBy { taskFactory.create(TestCatchThrowableErrorsAndThreadDeath::class.java).apply(4) }
            .withMessageContaining("FAKE OVERFLOW!")
    }

    @Test
    fun `cannot catch out-of-memory error`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(OutOfMemoryError::class.java)
            .isThrownBy { taskFactory.create(TestCatchThrowableErrorsAndThreadDeath::class.java).apply(5) }
            .withMessageContaining("FAKE OOM!")
    }

    @Test
    fun `cannot persist state across sessions`() = sandbox {
        /*
         * Compare the result from this sandbox with the results
         * from two supposedly isolated tasks.
         */
        val taskFactory = classLoader.createTypedTaskFactory()
        val result1 = taskFactory.create(TestStatePersistence::class.java).apply(0)

        val executor = SandboxExecutor<Int, Int>(configuration, validating = false)
        val result2 = executor.run<Int, Int, TestStatePersistence>(0)
        val result3 = executor.run<Int, Int, TestStatePersistence>(0)
        assertThat(result1)
            .isEqualTo(result2.result)
            .isEqualTo(result3.result)
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
    fun `can load and execute code that uses IO`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<NoClassDefFoundError>{ taskFactory.create(TestIO::class.java).apply("test.dat") }
        assertThat(ex)
            .hasCauseExactlyInstanceOf(ClassNotFoundException::class.java)
        assertThat(ex.cause)
            .hasMessageContaining("Class file not found: java/nio/file/Paths.class")
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
    fun `can load and execute code that uses reflection`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<RuleViolationError>{ taskFactory.create(TestReflection::class.java).apply(0) }
        assertThat(ex)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Class.getDeclaredConstructor(Class[])")
        assertThat(ex).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    class TestReflection : Function<Int, Int> {
        override fun apply(input: Int): Int {
            val clazz = Object::class.java
            val obj = clazz.getDeclaredConstructor().newInstance()
            return obj.hashCode()
        }
    }

    @Test
    fun `can load and execute code that uses notify()`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<RuleViolationError>{ taskFactory.create(TestMonitors::class.java).apply(1) }
        assertThat(ex)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.notify()")
        assertThat(ex).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses notifyAll()`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<RuleViolationError>{ taskFactory.create(TestMonitors::class.java).apply(2) }
        assertThat(ex)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.notifyAll()")
        assertThat(ex).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses wait()`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<RuleViolationError>{ taskFactory.create(TestMonitors::class.java).apply(3) }
        assertThat(ex)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.wait()")
        assertThat(ex).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses wait(long)`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<RuleViolationError>{ taskFactory.create(TestMonitors::class.java).apply(4) }
        assertThat(ex)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.wait(Long)")
        assertThat(ex).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `can load and execute code that uses wait(long,int)`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<RuleViolationError>{ taskFactory.create(TestMonitors::class.java).apply(5) }
        assertThat(ex)
            .hasMessageContaining("Disallowed reference to API;")
            .hasMessageContaining("java.lang.Object.wait(Long, Integer)")
        assertThat(ex).extracting { it.stackTrace.toList() }.asList().hasSize(2)
    }

    @Test
    fun `code after forbidden APIs is intact`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThat(taskFactory.create(TestMonitors::class.java).apply(0))
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
    fun `can load and execute code that has a native method`() = sandbox {
        assertThatExceptionOfType(UnsatisfiedLinkError::class.java)
            .isThrownBy { TestNativeMethod().apply(0) }
            .withMessageContaining("${TestNativeMethod::class.java.name}.evilDeeds()")
            .withMessageMatching(".*(int \\Q${TestNativeMethod::class.java.name}\\E\\.|\\.evilDeeds\\(\\)I)+.*")

        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(RuleViolationError::class.java)
            .isThrownBy { taskFactory.create(TestNativeMethod::class.java).apply(0) }
            .withMessageContaining("Native method has been deleted")
    }

    class TestNativeMethod : Function<Int, Int> {
        override fun apply(input: Int): Int {
            return evilDeeds()
        }

        private external fun evilDeeds(): Int
    }

    @Test
    fun `check arrays still work`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestArray::class.java).apply(100)
        assertThat(result).isEqualTo(arrayOf(100))
    }

    class TestArray : Function<Int, Array<Int>> {
        override fun apply(input: Int): Array<Int> {
            return listOf(input).toTypedArray()
        }
    }

    @Test
    fun `check building a string`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestStringBuilding::class.java).apply("Hello Sandbox!")
        assertThat(result)
            .isEqualTo("SANDBOX: Boolean=true, Char='X', Integer=1234, Long=99999, Short=3200, Byte=101, String='Hello Sandbox!', Float=123.456, Double=987.6543")
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
    fun `check System-arraycopy still works with Objects`() = sandbox {
        val source = arrayOf("one", "two", "three")
        assertThat(TestArrayCopy().apply(source))
            .isEqualTo(source)
            .isNotSameAs(source)

        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestArrayCopy::class.java).apply(source)
        assertThat(result)
            .isEqualTo(source)
            .isNotSameAs(source)
    }

    class TestArrayCopy : Function<Array<String>, Array<String>> {
        override fun apply(input: Array<String>): Array<String> {
            val newArray = Array(input.size) { "" }
            System.arraycopy(input, 0, newArray, 0, newArray.size)
            return newArray
        }
    }

    @Test
    fun `test System-arraycopy still works with CharArray`() = sandbox {
        val source = CharArray(10) { '?' }
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestCharArrayCopy::class.java).apply(source)
        assertThat(result)
            .isEqualTo(source)
            .isNotSameAs(source)
    }

    class TestCharArrayCopy : Function<CharArray, CharArray> {
        override fun apply(input: CharArray): CharArray {
            val newArray = CharArray(input.size) { 'X' }
            System.arraycopy(input, 0, newArray, 0, newArray.size)
            return newArray
        }
    }

    @Test
    fun `can load and execute class that has finalize`() = sandbox {
        assertThatExceptionOfType(UnsupportedOperationException::class.java)
            .isThrownBy { TestFinalizeMethod().apply(100) }
            .withMessageContaining("Very Bad Thing")

        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestFinalizeMethod::class.java).apply(100)
        assertThat(result).isEqualTo(100)
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
    fun `can execute parallel stream`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestParallelStream::class.java).apply("Pebble")
        assertThat(result).isEqualTo("Five,Four,One,Pebble,Three,Two")
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
        "BasicInput",
        "BasicOutput",
        "ImportTask",
        "java.io.DJVMInputStream",
        "java.lang.DJVM",
        "java.lang.DJVMAnnotationAction",
        "java.lang.DJVMAnnotationHandler",
        "java.lang.DJVMAnnotationHandler\$MethodValue",
        "java.lang.DJVMBootstrapClassAction",
        "java.lang.DJVMConstructorAction",
        "java.lang.DJVMEnumAction",
        "java.lang.DJVMException",
        "java.lang.DJVMNoResource",
        "java.lang.DJVMResourceKey",
        "java.lang.DJVMSystemResourceAction",
        "java.lang.DJVMThrowableWrapper",
        "java.lang.String\$InitAction",
        "java.time.DJVM",
        "java.time.DJVM\$InitAction",
        "java.security.DJVM",
        "java.security.DJVM\$PrivilegedTask",
        "java.security.DJVM\$PrivilegedExceptionTask",
        "java.util.concurrent.atomic.DJVM",
        "java.util.concurrent.locks.DJVMConditionObject",
        "javax.security.auth.x500.DJVM",
        "PredicateTask",
        "RawTask",
        "RuntimeCostAccounter",
        "TaskTypes",
        "Task"
    ])
    fun `users cannot load our sandboxed classes`(className: String) = sandbox {
        // Show the class exists to be found.
        assertThat(Class.forName("sandbox.$className")).isNotNull

        // Show the class cannot be loaded from the sandbox.
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(ClassNotFoundException::class.java)
            .isThrownBy { taskFactory.create(TestClassForName::class.java).apply(className) }
            .withMessageContaining(className)
    }

    @Test
    fun `users can load sandboxed classes`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TestClassForName::class.java).apply("java.util.List")
        assertThat(result.name).isEqualTo("sandbox.java.util.List")
    }

    class TestClassForName : Function<String, Class<*>> {
        override fun apply(input: String): Class<*> {
            return Class.forName(input)
        }
    }

    @Test
    fun `test case-insensitive string sorting`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(CaseInsensitiveSort::class.java).apply(arrayOf("Zelda", "angela", "BOB", "betsy", "ALBERT"))
        assertThat(result).isEqualTo(arrayOf("ALBERT", "angela", "betsy", "BOB", "Zelda"))
    }

    class CaseInsensitiveSort : Function<Array<String>, Array<String>> {
        override fun apply(input: Array<String>): Array<String> {
            return listOf(*input).sortedWith(String.CASE_INSENSITIVE_ORDER).toTypedArray()
        }
    }

    @Test
    fun `test unicode characters`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ExamineUnicodeBlock::class.java).apply(0x01f600)
        assertThat(result).isEqualTo("EMOTICONS")
    }

    class ExamineUnicodeBlock : Function<Int, String> {
        override fun apply(codePoint: Int): String {
            return Character.UnicodeBlock.of(codePoint).toString()
        }
    }

    @Test
    fun `test unicode scripts`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ExamineUnicodeScript::class.java).apply("COMMON")
        assertThat(result).isEqualTo(Character.UnicodeScript.COMMON)
    }

    class ExamineUnicodeScript : Function<String, Character.UnicodeScript?> {
        override fun apply(scriptName: String): Character.UnicodeScript? {
            val script = Character.UnicodeScript.valueOf(scriptName)
            return if (script::class.java.isEnum) script else null
        }
    }

    @Test
    fun `test users cannot define new classes`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(RuleViolationError::class.java)
            .isThrownBy { taskFactory.create(DefineNewClass::class.java).apply("sandbox.java.lang.DJVM") }
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
    fun `test users cannot load new classes`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(RuleViolationError::class.java)
            .isThrownBy { taskFactory.create(LoadNewClass::class.java).apply("sandbox.java.lang.DJVM") }
            .withMessageContaining("Disallowed reference to API;")
            .withMessageContaining("java.lang.ClassLoader.loadClass")
    }

    class LoadNewClass : Function<String, Class<*>> {
        override fun apply(input: String): Class<*> {
            val cl = object : ClassLoader() {
                fun load(): Class<*> {
                    return super.loadClass(input, true)
                }
            }
            return cl.load()
        }
    }

    @Test
    fun `test users cannot lookup classes`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(RuleViolationError::class.java)
            .isThrownBy { taskFactory.create(FindClass::class.java).apply("sandbox.java.lang.DJVM") }
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
    fun `test users cannot load system resources`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetSystemResources::class.java).apply("META-INF/MANIFEST.MF")
        assertThat(result).isFalse()
    }

    class GetSystemResources : Function<String, Boolean> {
        override fun apply(resourceName: String): Boolean {
            return ClassLoader.getSystemResources(resourceName).hasMoreElements()
        }
    }

    @Test
    fun `test users cannot load system resource URL`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetSystemResourceURL::class.java).apply("META-INF/MANIFEST.MF")
        assertThat(result).isNull()
    }

    class GetSystemResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemResource(resourceName)?.path
        }
    }

    @Test
    fun `test users cannot load system resource stream`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetSystemResourceStream::class.java).apply("META-INF/MANIFEST.MF")
        assertThat(result).isNull()
    }

    class GetSystemResourceStream : Function<String, Int?> {
        override fun apply(resourceName: String): Int? {
            return ClassLoader.getSystemResourceAsStream(resourceName)?.available()
        }
    }

    @Test
    fun `test users cannot load resources`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetResources::class.java).apply("META-INF/MANIFEST.MF")
        assertThat(result).isFalse()
    }

    class GetResources : Function<String, Boolean> {
        override fun apply(resourceName: String): Boolean {
            return ClassLoader.getSystemClassLoader().getResources(resourceName).hasMoreElements()
        }
    }

    @Test
    fun `test users cannot load resource URL`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetResourceURL::class.java).apply("META-INF/MANIFEST.MF")
        assertThat(result).isNull()
    }

    class GetResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemClassLoader().getResource(resourceName)?.path
        }
    }

    @Test
    fun `test users cannot load resource stream`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetResourceStream::class.java).apply("META-INF/MANIFEST.MF")
        assertThat(result).isNull()
    }

    class GetResourceStream : Function<String, Int?> {
        override fun apply(resourceName: String): Int? {
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)?.available()
        }
    }

    @Test
    fun `test users cannot read package`() = sandbox {
        assertThat(GetClassPackage().apply(null))
            .isEqualTo(GetClassPackage::class.java.getPackage()?.toString())

        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetClassPackage::class.java).apply(null)
        assertThat(result).isNull()
    }

    class GetClassPackage : Function<String?, String?> {
        override fun apply(input: String?): String? {
            return javaClass.getPackage()?.toString()
        }
    }

    @Test
    fun `test creating arrays of arrays`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ArraysOfArrays::class.java).apply("THINGY")
        assertThat(result).isEqualTo(arrayOf(arrayOf("THINGY")))
    }

    class ArraysOfArrays : Function<Any, Array<Any>> {
        override fun apply(input: Any): Array<Any> {
            return arrayOf(arrayOf(input))
        }
    }

    @Test
    fun `test creating arrays of int arrays`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ArrayOfIntArrays::class.java).apply(0)
        assertThat(result).isEqualTo(arrayOf(intArrayOf(0)))
    }

    class ArrayOfIntArrays : Function<Int, Array<IntArray>> {
        override fun apply(input: Int): Array<IntArray> {
            return arrayOf(intArrayOf(input))
        }
    }

    @Test
    fun `test class with protection domain`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        assertThatExceptionOfType(RuleViolationError::class.java)
            .isThrownBy { taskFactory.create(AccessClassProtectionDomain::class.java).apply(0) }
            .withMessageContaining("Disallowed reference to API;")
            .withMessageContaining("java.lang.Class.getProtectionDomain")
    }

    class AccessClassProtectionDomain : Function<Int, String> {
        override fun apply(input: Int): String {
            return input::class.java.protectionDomain.codeSource.toString()
        }
    }

    @Test
    fun `test kotlin class access`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val accessTask = taskFactory.create(AccessKotlinClass::class.java)
        assertThat(accessTask.apply("Message")).isEqualTo(classLoader.toSandboxClass(String::class.java))
        assertThat(accessTask.apply(0L)).isEqualTo(classLoader.toSandboxClass(Long::class.javaObjectType))
        assertThat(accessTask.apply(null)).isEqualTo(classLoader.toSandboxClass(Henry::class.java))
    }

    class AccessKotlinClass : Function<Any?, Class<*>> {
        override fun apply(input: Any?): Class<*> {
            return if (input == null) {
                Henry::class.java
            } else {
                input::class.java
            }
        }
    }

    class Henry
}
