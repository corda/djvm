package net.corda.djvm.validation

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.ClassAndMemberVisitor
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.Instruction
import net.corda.djvm.references.ClassRepresentation
import net.corda.djvm.references.Member
import net.corda.djvm.rules.ClassRule
import net.corda.djvm.rules.InstructionRule
import net.corda.djvm.rules.MemberRule
import net.corda.djvm.rules.Rule
import net.corda.djvm.utilities.Processor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import java.util.function.Consumer

/**
 * Helper class for validating a set of rules for a class or set of classes.
 *
 * @property rules A set of rules to validate for provided classes.
 * @param configuration The configuration to use for class analysis.
 */
class RuleValidator(
        private val rules: List<Rule>,
        configuration: AnalysisConfiguration
) : ClassAndMemberVisitor(configuration, STUB) {
    private companion object {
        @JvmField
        val STUB = StubClassReader(API_VERSION)
    }

    /**
     * Apply the set of rules to the traversed class and record any violations.
     */
    override fun visitClass(clazz: ClassRepresentation): ClassRepresentation {
        if (shouldClassBeProcessed(clazz.name)) {
            val context = RuleContext(currentAnalysisContext())
            Processor.processEntriesOfType<ClassRule>(rules, analysisContext.messages, Consumer {
                it.validate(context, clazz)
            })
        }
        return super.visitClass(clazz)
    }

    /**
     * Apply the set of rules to the traversed method and record any violations.
     */
    override fun visitMethod(clazz: ClassRepresentation, method: Member): Member {
        if (shouldClassBeProcessed(clazz.name) && shouldMemberBeProcessed(method.reference)) {
            val context = RuleContext(currentAnalysisContext())
            Processor.processEntriesOfType<MemberRule>(rules, analysisContext.messages, Consumer {
                it.validate(context, method)
            })
        }
        return super.visitMethod(clazz, method)
    }

    /**
     * Apply the set of rules to the traversed field and record any violations.
     */
    override fun visitField(clazz: ClassRepresentation, field: Member): Member {
        if (shouldClassBeProcessed(clazz.name) && shouldMemberBeProcessed(field.reference)) {
            val context = RuleContext(currentAnalysisContext())
            Processor.processEntriesOfType<MemberRule>(rules, analysisContext.messages, Consumer {
                it.validate(context, field)
            })
        }
        return super.visitField(clazz, field)
    }

    /**
     * Apply the set of rules to the traversed instruction and record any violations.
     */
    override fun visitInstruction(method: Member, emitter: EmitterModule, instruction: Instruction) {
        if (shouldClassBeProcessed(method.className) && shouldMemberBeProcessed(method.reference)) {
            val context = RuleContext(currentAnalysisContext())
            Processor.processEntriesOfType<InstructionRule>(rules, analysisContext.messages, Consumer {
                it.validate(context, instruction)
            })
        }
        super.visitInstruction(method, emitter, instruction)
    }

    /**
     * Provide some "stub" visitors for methods and fields so that we can apply
     * [InstructionRule] operations.
     */
    private class StubClassReader(api: Int) : ClassVisitor(api) {
        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            return StubMethodReader(api)
        }

        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor {
            return StubFieldReader(api)
        }
    }

    private class StubMethodReader(api: Int) : MethodVisitor(api)
    private class StubFieldReader(api: Int) : FieldVisitor(api)
}
