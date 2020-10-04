package net.corda.djvm.code.impl

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.SyntheticResolver
import net.corda.djvm.analysis.impl.ClassAndMemberVisitor
import net.corda.djvm.analysis.impl.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.code.ClassDefinitionProvider
import net.corda.djvm.code.DefinitionProvider
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.references.ClassRepresentation
import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.references.ImmutableMember
import net.corda.djvm.references.Member
import net.corda.djvm.references.MethodBody
import net.corda.djvm.utilities.loggerFor
import net.corda.djvm.utilities.processEntriesOfType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.MethodNode
import java.util.Collections.unmodifiableList
import java.util.function.Consumer

/**
 * Helper class for applying a set of definition providers and emitters to a class or set of classes.
 *
 * @param classVisitor Class visitor to use when traversing the structure of classes.
 * @param configuration The configuration to use for class analysis.
 * @param remapper [Remapper] for transforming classes into sandbox classes.
 * @param definitionProviders A set of providers used to update the name or meta-data of classes and members.
 * @param emitters A set of code emitters used to modify and instrument method bodies.
 */
class ClassMutator(
    classVisitor: ClassVisitor,
    configuration: AnalysisConfiguration,
    private val remapper: Remapper,
    private val definitionProviders: List<DefinitionProvider>,
    emitters: List<Emitter>
) : ClassAndMemberVisitor(classVisitor, configuration, remapper) {

    override fun specialise(cv: ClassVisitor, args: Array<out Any?>): ClassVisitor {
        return SandboxClassRemapper(
            ExceptionRemapper(SandboxStitcher(ResetVisitor(cv), configuration), configuration.syntheticResolver),
            args[0] as Remapper,
            configuration
        )
    }

    private fun getMappedClassName(): String {
        return remapper.map(getCurrentClass().name)
    }

    private inner class ResetVisitor(classVisitor: ClassVisitor) : ClassVisitor(API_VERSION, classVisitor) {
        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor {
            if (access and ACC_STATIC_FINAL == ACC_STATIC && !isImmutable) {
                val zeroOpcode = when (descriptor) {
                    "I", "Z", "S", "B", "C" -> ICONST_0
                    "J" -> LCONST_0
                    "D" -> DCONST_0
                    "F" -> FCONST_0
                    else -> ACONST_NULL
                }
                initializationCode.visitInsn(zeroOpcode)
                initializationCode.visitFieldInsn(PUTSTATIC, getMappedClassName(), name, descriptor)
            }
            return super.visitField(access, name, descriptor, signature, value)
        }

        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return super.visitMethod(access, name, descriptor, signature, exceptions)?.let { mv ->
                if (name == CLASS_CONSTRUCTOR_NAME && descriptor == "()V" && !isImmutable) {
                    ClassInitVisitor(api, mv, getMappedClassName())
                } else {
                    mv
                }
            }
        }
    }

    private inner class ClassInitVisitor(api: Int, mv: MethodVisitor, private val mappedName: String)
        : MethodBodyCopier(api, mv, initializationCode)
    {
        override fun visitCode() {
            super.visitCode()
            EmitterModuleImpl(mv, configuration).apply {
                registerResetMethod(mappedName, getCurrentClass().isInterface)
                isResetRegistered = true
            }
        }
    }

    /*
     * Some emitters must be executed before others. E.g. we need to apply
     * the tracing emitters before the non-tracing ones.
     */
    private val allEmitters: List<Emitter> = unmodifiableList(emitters.sortedBy(Emitter::priority))
    private var emitters = allEmitters
    private val initializationCode = MethodNode()
    private var isResetRegistered = false
    private var isImmutable = false

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
        var resultingClass: ImmutableClass = clazz
        processEntriesOfType(definitionProviders.filterIsInstance<ClassDefinitionProvider>(), analysisContext.messages, Consumer {
            resultingClass = it.define(currentAnalysisContext(), resultingClass)
        })
        if (clazz !== resultingClass) {
            logger.trace("Type has been mutated {}", clazz)
            setModified()
        }
        if (resultingClass.access and ACC_ANNOTATION != 0) {
            setAnnotation()
        }
        isImmutable = configuration.isImmutable(getMappedClassName())
        if (isImmutable) {
            // Do not instrument immutable classes as the sandbox does not reset them.
            emitters = allEmitters.subList(allEmitters.indexOfFirst { it.priority > EMIT_TRACING }, allEmitters.size)
        }
        return super.visitClass(resultingClass as ClassRepresentation)
    }

    /**
     * If we have some static fields to reset, and haven't already registered
     * our reset method via an existing class initialiser block then we need
     * to create one and register it.
     */
    override fun visitClassEnd(classVisitor: ClassVisitor, clazz: ClassRepresentation) {
        if (initializationCode.instructions.size() > 0) {
            writeClassResetter(classVisitor)
            if (!isResetRegistered) {
                writeClassInitializer(classVisitor)
            }
        }
        super.visitClassEnd(classVisitor, clazz)
    }

    private fun writeClassResetter(classVisitor: ClassVisitor) {
        classVisitor.visitMethod(CLASS_RESET_ACCESS, CLASS_RESET_NAME, "()V", null, null)?.also { mv ->
            initializationCode.visitMaxs(-1, -1)
            if (initializationCode.instructions.last.opcode != RETURN) {
                initializationCode.visitInsn(RETURN)
            }
            initializationCode.accept(mv)
            setModified()
        }
    }

    private fun writeClassInitializer(classVisitor: ClassVisitor) {
        classVisitor.visitMethod(ACC_STATIC or ACC_STRICT, CLASS_CONSTRUCTOR_NAME, "()V", null, null)?.also { mv ->
            mv.visitCode()
            EmitterModuleImpl(mv, configuration).registerResetMethod(getMappedClassName(), getCurrentClass().isInterface)
            mv.visitInsn(RETURN)
            mv.visitMaxs(-1, -1)
            mv.visitEnd()
            setModified()
        }
    }

    /**
     * Apply definition providers to a method. This can be used to update the name or definition (pertinent meta-data)
     * of a class member.
     */
    override fun visitMethod(clazz: ClassRepresentation, method: Member): Member {
        var resultingMethod: ImmutableMember = method
        processEntriesOfType(definitionProviders.filterIsInstance<MemberDefinitionProvider>(), analysisContext.messages, Consumer {
            resultingMethod = it.define(currentAnalysisContext(), resultingMethod)
        })
        if (method !== resultingMethod) {
            logger.trace("Method has been mutated {}", method)
            setModified()
        }
        return super.visitMethod(clazz, resultingMethod as Member)
    }

    /**
     * Apply definition providers to a field. This can be used to update the name or definition (pertinent meta-data)
     * of a class member.
     */
    override fun visitField(clazz: ClassRepresentation, field: Member): Member {
        var resultingField: ImmutableMember = field
        processEntriesOfType(definitionProviders.filterIsInstance<MemberDefinitionProvider>(), analysisContext.messages, Consumer {
            resultingField = it.define(currentAnalysisContext(), resultingField)
        })
        if (field !== resultingField) {
            logger.trace("Field has been mutated {}", field)
            setModified()
        }
        return super.visitField(clazz, resultingField as Member)
    }

    /**
     * Apply emitters to an instruction. This can be used to instrument a part of the code block, change behaviour of
     * an existing instruction, or strip it out completely.
     */
    override fun visitInstruction(method: Member, emitter: EmitterModuleImpl, instruction: Instruction) {
        val context = EmitterContextImpl(currentAnalysisContext(), configuration, emitter)
        processEntriesOfType(emitters, analysisContext.messages, Consumer {
            it.emit(context, instruction)
        })
        if (!emitter.emitDefaultInstruction || emitter.hasEmittedCustomCode) {
            setModified()
        }
        super.visitInstruction(method, emitter, instruction)
    }

    private companion object {
        private val logger = loggerFor<ClassMutator>()
        private const val ACC_STATIC_FINAL: Int = ACC_STATIC or ACC_FINAL
        private const val CLASS_RESET_ACCESS: Int = ACC_PRIVATE or ACC_STATIC or ACC_SYNTHETIC or ACC_STRICT
    }
}

/**
 * Extra visitor that is applied after remapping is complete. This "stitches" the
 * original unmapped interface as a super-interface of the mapped version, as well
 * as adding or replacing any extra methods that are needed.
 */
private class SandboxStitcher(parent: ClassVisitor, private val configuration: AnalysisConfiguration)
    : ClassVisitor(API_VERSION, parent)
{
    private companion object {
        private val GENERIC_SIGNATURE = "^<([^:]++):.*>.*".toRegex()
    }

    private val extraMethods = mutableListOf<Member>()

    override fun visit(version: Int, access: Int, className: String, signature: String?, superName: String?, interfaces: Array<String>?) {
        var stitchedSignature = signature
        val stitchedInterfaces = configuration.stitchedInterfaces[className]?.let { methods ->
            extraMethods += methods
            val baseInterface = configuration.classResolver.reverse(className)
            if (stitchedSignature != null) {
                /*
                 * All of our stitched interfaces have a single generic
                 * parameter. This simplifies how we update the signature
                 * to include this new interface.
                 */
                GENERIC_SIGNATURE.matchEntire(stitchedSignature)?.apply {
                    val typeVar = groupValues[1]
                    stitchedSignature += "L$baseInterface<T$typeVar;>;"
                }
            }
            arrayOf(*(interfaces ?: emptyArray()), baseInterface)
        } ?: interfaces

        configuration.stitchedClasses[className]?.also { methods ->
            extraMethods += methods
        }

        super.visit(version, access, className, stitchedSignature, superName, stitchedInterfaces)
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (extraMethods.isEmpty()) {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        } else {
            val idx = extraMethods.indexOfFirst { it.memberName == name && it.descriptor == descriptor && it.genericsDetails.emptyAsNull == signature }
            if (idx != -1) {
                val replacement = extraMethods.removeAt(idx)
                if (replacement.body.isNotEmpty() || (access and ACC_ABSTRACT) != 0) {
                    // Replace an existing method, or delete it entirely if
                    // the replacement has no method body and isn't abstract.
                    super.visitMethod(access, name, descriptor, signature, exceptions)?.also { mv ->
                        // This COMPLETELY replaces the original method, and
                        // will also discard any annotations it may have had.
                        writeMethodBody(mv, replacement.body)
                    }
                }
                null
            } else {
                super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }
    }

    override fun visitEnd() {
        for (method in extraMethods) {
            with(method) {
                super.visitMethod(access, memberName, descriptor, genericsDetails.emptyAsNull, exceptions.toTypedArray())?.also { mv ->
                    writeMethodBody(mv, body)
                }
            }
        }
        extraMethods.clear()
        super.visitEnd()
    }

    private fun writeMethodBody(mv: MethodVisitor, body: List<MethodBody>) {
        mv.visitCode()
        EmitterModuleImpl(mv, configuration).writeByteCode(body)
        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }
}

/**
 * Map exceptions in method signatures to their sandboxed equivalents.
 */
private class ExceptionRemapper(parent: ClassVisitor, private val syntheticResolver: SyntheticResolver) : ClassVisitor(API_VERSION, parent) {
    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val mappedExceptions = exceptions?.map(syntheticResolver::getRealThrowableName)?.toTypedArray()
        return super.visitMethod(access, name, descriptor, signature, mappedExceptions)?.let(::MethodExceptionRemapper)
    }

    /**
     * Map exceptions in method try-catch blocks to their sandboxed equivalents.
     */
    private inner class MethodExceptionRemapper(parent: MethodVisitor) : MethodVisitor(api, parent) {
        override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, exceptionType: String?) {
            val mappedExceptionType = exceptionType?.let(syntheticResolver::getRealThrowableName)
            super.visitTryCatchBlock(start, end, handler, mappedExceptionType)
        }
    }
}
