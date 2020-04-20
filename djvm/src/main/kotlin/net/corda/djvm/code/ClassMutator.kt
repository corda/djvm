package net.corda.djvm.code

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.ClassAndMemberVisitor
import net.corda.djvm.code.instructions.MethodEntry
import net.corda.djvm.references.ClassRepresentation
import net.corda.djvm.references.Member
import net.corda.djvm.references.MethodBody
import net.corda.djvm.utilities.loggerFor
import net.corda.djvm.utilities.processEntriesOfType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes.*
import java.util.function.Consumer

/**
 * Helper class for applying a set of definition providers and emitters to a class or set of classes.
 *
 * @param classVisitor Class visitor to use when traversing the structure of classes.
 * @property configuration The configuration to use for class analysis.
 * @property definitionProviders A set of providers used to update the name or meta-data of classes and members.
 * @param emitters A set of code emitters used to modify and instrument method bodies.
 */
class ClassMutator(
        classVisitor: ClassVisitor,
        private val configuration: AnalysisConfiguration,
        private val definitionProviders: List<DefinitionProvider>,
        emitters: List<Emitter>
) : ClassAndMemberVisitor(configuration, classVisitor) {

    /**
     * Internal [Emitter] to add static field initializers to
     * any class constructor method.
     */
    private inner class PrependClassInitializer : Emitter {
        override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
            if (instruction is MethodEntry
                    && instruction.method.memberName == CLASS_CONSTRUCTOR_NAME && instruction.method.descriptor == "()V"
                    && initializers.isNotEmpty()) {
                writeByteCode(initializers)
                initializers.clear()
            }
        }
    }

    /*
     * Some emitters must be executed before others. E.g. we need to apply
     * the tracing emitters before the non-tracing ones.
     */
    private val emitters: List<Emitter> = (emitters + PrependClassInitializer()).sortedBy(Emitter::priority)
    private val initializers = mutableListOf<MethodBody>()

    var flags: Int = 0
        private set(value) { field = field or value }

    /**
     * Tracks whether any modifications have been applied to any of the processed class(es) and pertinent members.
     */
    private fun setModified() {
        flags = DJVM_MODIFIED
    }

    private fun setAnnotation() {
        flags = DJVM_ANNOTATION
    }

    /**
     * Apply definition providers to a class. This can be used to update the name or definition (pertinent meta-data)
     * of the class itself.
     */
    override fun visitClass(clazz: ClassRepresentation): ClassRepresentation {
        var resultingClass = clazz
        processEntriesOfType<ClassDefinitionProvider>(definitionProviders, analysisContext.messages, Consumer {
            resultingClass = it.define(currentAnalysisContext(), resultingClass)
        })
        if (clazz != resultingClass) {
            logger.trace("Type has been mutated {}", clazz)
            setModified()
        }
        if (clazz.access and ACC_ANNOTATION != 0) {
            setAnnotation()
        }
        return super.visitClass(resultingClass)
    }

    /**
     * If we have some static fields to initialise, and haven't already added them
     * to an existing class initialiser block then we need to create one.
     */
    override fun visitClassEnd(classVisitor: ClassVisitor, clazz: ClassRepresentation) {
        tryWriteClassInitializer(classVisitor)
        super.visitClassEnd(classVisitor, clazz)
    }

    private fun tryWriteClassInitializer(classVisitor: ClassVisitor) {
        if (initializers.isNotEmpty()) {
            classVisitor.visitMethod(ACC_STATIC, CLASS_CONSTRUCTOR_NAME, "()V", null, null)?.also { mv ->
                mv.visitCode()
                EmitterModule(mv, configuration).writeByteCode(initializers)
                mv.visitInsn(RETURN)
                mv.visitMaxs(-1, -1)
                mv.visitEnd()
            }
            initializers.clear()
            setModified()
        }
    }

    /**
     * Apply definition providers to a method. This can be used to update the name or definition (pertinent meta-data)
     * of a class member.
     */
    override fun visitMethod(clazz: ClassRepresentation, method: Member): Member {
        var resultingMethod = method
        processEntriesOfType<MemberDefinitionProvider>(definitionProviders, analysisContext.messages, Consumer {
            resultingMethod = it.define(currentAnalysisContext(), resultingMethod)
        })
        if (method != resultingMethod) {
            logger.trace("Method has been mutated {}", method)
            setModified()
        }
        return super.visitMethod(clazz, resultingMethod)
    }

    /**
     * Apply definition providers to a field. This can be used to update the name or definition (pertinent meta-data)
     * of a class member.
     */
    override fun visitField(clazz: ClassRepresentation, field: Member): Member {
        var resultingField = field
        processEntriesOfType<MemberDefinitionProvider>(definitionProviders, analysisContext.messages, Consumer {
            resultingField = it.define(currentAnalysisContext(), resultingField)
        })
        if (field != resultingField) {
            logger.trace("Field has been mutated {}", field)
            initializers += resultingField.body
            setModified()
        }
        return super.visitField(clazz, resultingField)
    }

    /**
     * Apply emitters to an instruction. This can be used to instrument a part of the code block, change behaviour of
     * an existing instruction, or strip it out completely.
     */
    override fun visitInstruction(method: Member, emitter: EmitterModule, instruction: Instruction) {
        val context = EmitterContext(currentAnalysisContext(), configuration, emitter)
        processEntriesOfType<Emitter>(emitters, analysisContext.messages, Consumer {
            it.emit(context, instruction)
        })
        if (!emitter.emitDefaultInstruction || emitter.hasEmittedCustomCode) {
            setModified()
        }
        super.visitInstruction(method, emitter, instruction)
    }

    private companion object {
        private val logger = loggerFor<ClassMutator>()
    }

}
