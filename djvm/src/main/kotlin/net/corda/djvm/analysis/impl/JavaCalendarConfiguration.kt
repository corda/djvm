@file:JvmName("JavaCalendarConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.impl.DJVM_NAME
import net.corda.djvm.references.Member
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC

/**
 * Generate [Member] objects that will be stitched into [sandbox.sun.util.calendar.CalendarSystem].
 */
fun generateJavaCalendarMethods(): List<Member> = listOf(
        object : MethodBuilder(
            access = ACC_PUBLIC or ACC_STATIC,
            className = "sandbox/sun/util/calendar/CalendarSystem",
            memberName = "getCalendarProperties",
            descriptor = "()Lsandbox/java/util/Properties;"
        ) {
            /**
             * Replace getCalendarProperties():
             *     return DJVM.getCalendarProperties()
             */
            override fun writeBody(emitter: EmitterModule) = with(emitter) {
                invokeStatic(DJVM_NAME, memberName, descriptor)
                returnObject()
            }
        }.withBody().build()
)
