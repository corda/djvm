package net.corda.djvm.code

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.analysis.impl.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.code.impl.ClassMutator
import net.corda.djvm.code.impl.emit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Remapper

class EmitterModuleTest : TestBase(KOTLIN) {

    @Test
    fun `can emit code to method body`() {
        var hasEmittedTypeInstruction = false
        val methodVisitor = object : MethodVisitor(API_VERSION) {
            override fun visitTypeInsn(opcode: Int, type: String) {
                if (opcode == NEW && type == Type.getInternalName(java.lang.String::class.java)) {
                    hasEmittedTypeInstruction = true
                }
            }
        }
        val visitor = object : ClassVisitor(API_VERSION) {
            override fun visitMethod(
                    access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?
            ): MethodVisitor {
                return methodVisitor
            }
        }
        val emitter = object : Emitter {
            override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                new<java.lang.String>()
            }

        }
        val context = context
        val mutator = ClassMutator(
            classVisitor = visitor,
            remapper = object : Remapper() {},
            configuration = configuration,
            definitionProviders = emptyList(),
            emitters = listOf(emitter)
        )
        mutator.analyze<TestClass>(context)
        assertThat(hasEmittedTypeInstruction).isTrue()
    }

    class TestClass {
        fun foo() {}
    }

}
