package net.corda.djvm.analysis

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhitelistTest : TestBase(KOTLIN) {

    @Test
    fun `can determine when a class is whitelisted when namespace is covered`() {
        val whitelist = Whitelist.MINIMAL
        assertThat(whitelist.matches("java/lang/Object")).isTrue()
        assertThat(whitelist.matches("java/lang/Object.<init>:()V")).isTrue()
        assertThat(whitelist.matches("java/lang/reflect/Array")).isTrue()
        assertThat(whitelist.matches("java/lang/reflect/Array.setInt:(Ljava/lang/Object;II)V")).isTrue()
        assertThat(whitelist.matches("java/lang/StrictMath.sin:(D)D")).isTrue()
    }

    @Test
    fun `can determine when a class is not whitelisted when namespace is covered`() {
        val whitelist = Whitelist.MINIMAL
        assertThat(whitelist.matches("java/util/Random")).isFalse()
        assertThat(whitelist.matches("java/util/Random.<init>:()V")).isFalse()
        assertThat(whitelist.matches("java/util/Random.nextInt:()I")).isFalse()
        assertThat(whitelist.matches("java/lang/StrictMath")).isFalse()
        assertThat(whitelist.matches("java/lang/StrictMath.random:()D")).isFalse()
    }

    @Test
    fun `can determine when a class is whitelisted when namespace is not covered`() {
        val whitelist = Whitelist.MINIMAL + setOf(
                "^org/assertj/.*\$".toRegex(),
                "^org/junit/.*\$".toRegex()
        )
        assertThat(whitelist.matches("org/junit/Test")).isTrue()
        assertThat(whitelist.matches("org/assertj/core/api/Assertions")).isTrue()
        assertThat(whitelist.matches("net/foo/bar/Baz")).isFalse()
    }

    @Test
    fun `can determine when a namespace is not covered`() {
        val whitelist = Whitelist.MINIMAL
        assertThat(whitelist.matches("java/lang/Object")).isTrue()
        assertThat(whitelist.matches("org/junit/Test")).isFalse()
    }

    @Test
    fun `test closeables are whitelisted`() {
        val whitelist = Whitelist.MINIMAL
        assertThat(whitelist.matches("java/lang/AutoCloseable")).isTrue()
        assertThat(whitelist.matches("java/lang/AutoCloseable.close:()V")).isTrue()

        assertThat(whitelist.matches("java/io/Closeable")).isTrue()
        assertThat(whitelist.matches("java/io/Closeable.close:()V")).isTrue()
    }

    @Test
    fun `test atomic field updater factories are whitelisted`() {
        val whitelist = Whitelist.MINIMAL
        assertThat(whitelist.matches("java/util/concurrent/atomic/AtomicIntegerFieldUpdater.newUpdater:(Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicIntegerFieldUpdater;")).isTrue()
        assertThat(whitelist.matches("java/util/concurrent/atomic/AtomicLongFieldUpdater.newUpdater:(Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicLongFieldUpdater;")).isTrue()
        assertThat(whitelist.matches("java/util/concurrent/atomic/AtomicReferenceFieldUpdater.newUpdater:(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater;")).isTrue()
    }

    @Test
    fun `test atomic field updaters are not whitelisted`() {
        val whitelist = Whitelist.MINIMAL
        assertThat(whitelist.matches("java/util/concurrent/atomic/AtomicIntegerFieldUpdater")).isFalse()
        assertThat(whitelist.matches("java/util/concurrent/atomic/AtomicLongFieldUpdater")).isFalse()
        assertThat(whitelist.matches("java/util/concurrent/atomic/AtomicReferenceFieldUpdater")).isFalse()
    }

    @Test
    fun `test access controller`() {
        val whitelist = Whitelist.MINIMAL
        assertThat(whitelist.matches("java/security/AccessController")).isFalse()
        assertThat(whitelist.matches("java/security/AccessController.doPrivileged:(Ljava/security/PrivilegedAction;)Ljava/lang/Object;")).isTrue()
        assertThat(whitelist.matches("java/security/AccessController.doPrivileged:(Ljava/security/PrivilegedExceptionAction;)Ljava/lang/Object;")).isTrue()
        assertThat(whitelist.matches("java/security/AccessController.doPrivilegedWithCombiner:(Ljava/security/PrivilegedAction;)Ljava/lang/Object;")).isTrue()
        assertThat(whitelist.matches("java/security/AccessController.doPrivilegedWithCombiner:(Ljava/security/PrivilegedExceptionAction;)Ljava/lang/Object;")).isTrue()
        assertThat(whitelist.matches("java/security/AccessController.doPrivileged:(Ljava/security/PrivilegedAction;Ljava/security/AccessContext;)Ljava/lang/Object;")).isFalse()
    }
}
