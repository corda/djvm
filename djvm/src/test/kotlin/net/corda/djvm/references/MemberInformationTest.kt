package net.corda.djvm.references

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MemberInformationTest {
    @Test
    fun `can detect methods and fields from descriptors`() {
        assertTrue(reference("()V").isMethod)
        assertTrue(reference("(IJ)V").isMethod)
        assertTrue(reference("(IJ)Lfoo/Bar;").isMethod)
        assertTrue(reference("(Ljava/lang/String;J)V").isMethod)
        assertTrue(reference("(Ljava/lang/String;J)Lfoo/Bar;").isMethod)
        assertFalse(reference("V").isMethod)
        assertFalse(reference("[Z").isMethod)
        assertFalse(reference("Ljava/lang/String;").isMethod)
        assertFalse(reference("[Ljava/lang/String;").isMethod)
    }

    private fun reference(descriptor: String) =
            MemberReference("", "", descriptor)
}