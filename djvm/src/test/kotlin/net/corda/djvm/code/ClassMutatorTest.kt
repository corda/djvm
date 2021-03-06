package net.corda.djvm.code

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.impl.ClassMutator
import net.corda.djvm.code.impl.emit
import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.references.ImmutableMember
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes.ACC_STRICT

@Suppress("unused")
class ClassMutatorTest : TestBase(KOTLIN) {

    @Test
    fun `can mutate class definition`() {
        var hasProvidedDefinition = false
        val definitionProvider = object : ClassDefinitionProvider {
            override fun define(context: AnalysisRuntimeContext, clazz: ImmutableClass): ImmutableClass {
                hasProvidedDefinition = true
                return clazz.toMutable().copy(access = clazz.access or ACC_STRICT)
            }
        }
        val context = context
        val mutator = ClassMutator(
            classVisitor = Writer(),
            remapper = remapper,
            configuration = configuration,
            definitionProviders = listOf(definitionProvider),
            emitters = emptyList()
        )
        mutator.analyze<TestClass>(context)
        assertThat(hasProvidedDefinition).isTrue()
        assertThat(context.classes.get<TestClass>().access or ACC_STRICT).isNotEqualTo(0)
    }

    class TestClass

    @Test
    fun `can mutate member definition`() {
        var hasProvidedDefinition = false
        val definitionProvider = object : MemberDefinitionProvider {
            override fun define(context: AnalysisRuntimeContext, member: ImmutableMember): ImmutableMember {
                hasProvidedDefinition = true
                return member.toMutable().copy(access = member.access or ACC_STRICT)
            }
        }
        val context = context
        val mutator = ClassMutator(
            classVisitor = Writer(),
            remapper = remapper,
            configuration = configuration,
            definitionProviders = listOf(definitionProvider),
            emitters = emptyList()
        )
        mutator.analyze<TestClassWithMembers>(context)
        assertThat(hasProvidedDefinition).isTrue()
        for (member in context.classes.get<TestClassWithMembers>().members.values) {
            assertThat(member.access or ACC_STRICT).isNotEqualTo(0)
        }
    }

    class TestClassWithMembers {
        fun foo() {}
        fun bar(): Int = 0
        fun baz(): Double = 1.0
    }

    @Test
    fun `can mutate code`() {
        var hasEmittedCode = false
        var shouldPreventDefault = false
        val emitter = object : Emitter {
            override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
                loadConstant(0)
                pop()
                preventDefault()
                hasEmittedCode = hasEmittedCustomCode
                shouldPreventDefault = !emitDefaultInstruction
            }
        }
        val context = context
        val mutator = ClassMutator(
            classVisitor = Writer(),
            remapper = remapper,
            configuration = configuration,
            definitionProviders = emptyList(),
            emitters = listOf(emitter)
        )
        mutator.analyze<TestClassWithMembers>(context)
        assertThat(hasEmittedCode).isTrue()
        assertThat(shouldPreventDefault).isTrue()
    }

}
