package net.corda.djvm.analysis

import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhitelistTest : TestBase() {

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

}
