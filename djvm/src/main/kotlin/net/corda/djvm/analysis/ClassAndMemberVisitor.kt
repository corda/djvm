package net.corda.djvm.analysis

import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.emptyAsNull
import net.corda.djvm.code.instructions.*
import net.corda.djvm.messages.Message
import net.corda.djvm.references.*
import org.objectweb.asm.*

/**
 * Functionality for traversing a class and its members.
 *
 * @property configuration The configuration to use for the analysis
 * @property classVisitor Class visitor to use when traversing the structure of classes.
 */
open class ClassAndMemberVisitor(
        private val configuration: AnalysisConfiguration,
        private val classVisitor: ClassVisitor?
) {

    /**
     * Holds a reference to the currently used analysis context.
     */
    protected var analysisContext: AnalysisContext = AnalysisContext.fromConfiguration(configuration)

    /**
     * Holds a link to the class currently being traversed.
     */
    private var currentClass: ClassRepresentation? = null

    /**
     * Holds a link to the member currently being traversed.
     */
    private var currentMember: Member? = null

    /**
     * The current source location.
     */
    private var sourceLocation = SourceLocation()

    /**
     * Analyze class by using the provided qualified name of the class.
     */
    inline fun <reified T> analyze(context: AnalysisContext, options: Int = 0) = analyze(T::class.java.name, context, options)

    /**
     * Analyze class by using the provided qualified name of the class.
     *
     * @param className The full, qualified name of the class.
     * @param context The context in which the analysis is conducted.
     * @param options Options for how to parse and process the class.
     * @param origin The originating class for the analysis.
     */
    fun analyze(className: String, context: AnalysisContext, options: Int, origin: String? = null) {
        configuration.supportingClassLoader.classReader(className, context, origin).apply {
            analyze(this, context, options)
        }
    }

    /**
     * Analyze class by using the provided class reader.
     *
     * @param classReader An instance of the class reader to use to access the byte code.
     * @param context The context in which to analyse the provided class.
     * @param options Options for how to parse and process the class.
     */
    fun analyze(classReader: ClassReader, context: AnalysisContext, options: Int) {
        analysisContext = context
        classReader.accept(ClassVisitorImpl(classVisitor), options)
    }

    /**
     * Extract information about the traversed class.
     */
    open fun visitClass(clazz: ClassRepresentation): ClassRepresentation = clazz

    /**
     * Process class after it has been fully traversed and analyzed.
     * The [classVisitor] has finished visiting all of the class's
     * existing elements (i.e. methods, fields, inner classes etc)
     * and is about to complete. However, it can still add new
     * elements to the class, if required.
     */
    open fun visitClassEnd(classVisitor: ClassVisitor, clazz: ClassRepresentation) {}

    /**
     * Extract the meta-data indicating the source file of the traversed class (i.e., where it is compiled from).
     */
    open fun visitSource(clazz: ClassRepresentation, source: String) {}

    /**
     * Extract information about the traversed class annotation.
     */
    open fun visitClassAnnotation(clazz: ClassRepresentation, descriptor: String) {}

    /**
     * Extract information about the traversed member annotation.
     */
    open fun visitMemberAnnotation(clazz: ClassRepresentation, member: Member, descriptor: String) {}

    /**
     * Extract information about the traversed method.
     */
    open fun visitMethod(clazz: ClassRepresentation, method: Member): Member = method

    /**
     * Extract information about the traversed field.
     */
    open fun visitField(clazz: ClassRepresentation, field: Member): Member = field

    /**
     * Extract information about the traversed instruction.
     */
    open fun visitInstruction(method: Member, emitter: EmitterModule, instruction: Instruction) {}

    /**
     * Get the analysis context to pass on to method and field visitors.
     */
    protected fun currentAnalysisContext(): AnalysisRuntimeContext {
        return AnalysisRuntimeContext(
                currentClass!!,
                currentMember,
                sourceLocation,
                analysisContext.messages,
                configuration
        )
    }

    /**
     * Check if a class should be processed or not.
     */
    protected fun shouldBeProcessed(className: String): Boolean {
        return !configuration.whitelist.inNamespace(className) &&
                !configuration.isPinnedClass(className)
    }

    /**
     * Extract information about the traversed member annotation.
     */
    private fun visitMemberAnnotation(
            descriptor: String,
            referencedClass: ClassRepresentation? = null,
            referencedMember: Member? = null
    ) {
        val clazz = (referencedClass ?: currentClass) ?: return
        val member = (referencedMember ?: currentMember) ?: return
        member.annotations.add(descriptor)
        captureExceptions {
            visitMemberAnnotation(clazz, member, descriptor)
        }
    }

    /**
     * Run action with a guard that populates [AnalysisRuntimeContext.messages]
     * based on the output.
     */
    private inline fun captureExceptions(action: () -> Unit): Boolean {
        return try {
            action()
            true
        } catch (exception: Throwable) {
            recordMessage(exception, currentAnalysisContext())
            false
        }
    }

    /**
     * Record a message derived from a [Throwable].
     */
    private fun recordMessage(exception: Throwable, context: AnalysisRuntimeContext) {
        context.messages.add(Message.fromThrowable(exception, context.location))
    }

    /**
     * Record a reference to a class.
     */
    private fun recordTypeReference(type: String) {
        val typeName = configuration.classModule
                .normalizeClassName(type)
                .replace("[]", "")
        if (shouldBeProcessed(currentClass!!.name)) {
            val classReference = ClassReference(typeName)
            analysisContext.references.add(classReference, sourceLocation)
        }
    }

    /**
     * Record a reference to a class member.
     */
    private fun recordMemberReference(owner: String, name: String, desc: String) {
        if (shouldBeProcessed(currentClass!!.name)) {
            recordTypeReference(owner)
            val memberReference = MemberReference(owner, name, desc)
            analysisContext.references.add(memberReference, sourceLocation)
        }
    }

    /**
     * Visitor used to traverse and analyze a class.
     */
    private inner class ClassVisitorImpl(
            targetVisitor: ClassVisitor?
    ) : ClassVisitor(API_VERSION, targetVisitor) {

        /**
         * Extract information about the traversed class.
         */
        override fun visit(
                version: Int, access: Int, name: String, signature: String?, superName: String?,
                interfaces: Array<String>?
        ) {
            val superClassName = superName ?: ""
            val interfaceNames = interfaces?.toMutableList() ?: mutableListOf()
            ClassRepresentation(version, access, name, superClassName, interfaceNames, genericsDetails = signature ?: "").also {
                currentClass = it
                currentMember = null
                sourceLocation = SourceLocation(className = name)
            }
            captureExceptions {
                currentClass = visitClass(currentClass!!)
            }
            val visitedClass = currentClass!!
            analysisContext.classes.add(visitedClass)
            super.visit(
                    version, access, visitedClass.name, signature,
                    visitedClass.superClass.emptyAsNull,
                    visitedClass.interfaces.toTypedArray()
            )
        }

        /**
         * Post-processing of the traversed class.
         */
        override fun visitEnd() {
            configuration.classModule
                    .getClassReferencesFromClass(currentClass!!, configuration.analyzeAnnotations)
                    .forEach(::recordTypeReference)
            captureExceptions {
                visitClassEnd(this, currentClass!!)
            }
            super.visitEnd()
        }

        /**
         * Extract the meta-data indicating the source file of the traversed class (i.e., where it is compiled from).
         */
        override fun visitSource(source: String?, debug: String?) {
            currentClass!!.apply {
                sourceFile = configuration.classModule.getFullSourceLocation(this, source)
                sourceLocation = sourceLocation.copy(sourceFile = sourceFile)
                captureExceptions {
                    visitSource(this, sourceFile)
                }
            }
            super.visitSource(source, debug)
        }

        /**
         * Extract information about provided annotations.
         */
        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
            currentClass!!.apply {
                annotations.add(desc)
                captureExceptions {
                    visitClassAnnotation(this, desc)
                }
            }
            return super.visitAnnotation(desc, visible)
        }

        /**
         * Extract information about the traversed method.
         */
        override fun visitMethod(
                access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
        ): MethodVisitor? {
            var visitedMember: Member? = null
            val clazz = currentClass!!
            val member = Member(
                access = access,
                className = clazz.name,
                memberName = name,
                descriptor = desc,
                genericsDetails = signature ?: "",
                exceptions = exceptions?.toMutableSet() ?: mutableSetOf()
            )
            currentMember = member
            sourceLocation = sourceLocation.copy(
                memberName = name,
                descriptor = desc,
                lineNumber = 0
            )
            val processMember = captureExceptions {
                visitedMember = visitMethod(clazz, member)
            }
            configuration.memberModule.addToClass(clazz, visitedMember ?: member)
            return if (processMember) {
                val derivedMember = visitedMember ?: member
                super.visitMethod(
                    derivedMember.access,
                    derivedMember.memberName,
                    derivedMember.descriptor,
                    signature,
                    derivedMember.exceptions.toTypedArray()
                )?.let { targetVisitor ->
                    MethodVisitorImpl(targetVisitor, derivedMember)
                }
            } else {
                null
            }
        }

        /**
         * Extract information about the traversed field.
         */
        override fun visitField(
                access: Int, name: String, desc: String, signature: String?, value: Any?
        ): FieldVisitor? {
            var visitedMember: Member? = null
            val clazz = currentClass!!
            val member = Member(
                access = access,
                className = clazz.name,
                memberName = name,
                descriptor = desc,
                genericsDetails = "",
                value = value
            )
            currentMember = member
            sourceLocation = sourceLocation.copy(
                memberName = name,
                descriptor = desc,
                lineNumber = 0
            )
            val processMember = captureExceptions {
                visitedMember = visitField(clazz, member)
            }
            configuration.memberModule.addToClass(clazz, visitedMember ?: member)
            return if (processMember) {
                val derivedMember = visitedMember ?: member
                super.visitField(
                    derivedMember.access,
                    derivedMember.memberName,
                    derivedMember.descriptor,
                    signature,
                    derivedMember.value
                )?.let { targetVisitor ->
                    FieldVisitorImpl(targetVisitor)
                }
            } else {
                null
            }
        }

    }

    /**
     * Visitor used to traverse and analyze a method.
     */
    private inner class MethodVisitorImpl(
            targetVisitor: MethodVisitor,
            private val method: Member
    ) : MethodVisitor(API_VERSION, targetVisitor) {

        /**
         * Record line number of current instruction.
         */
        override fun visitLineNumber(line: Int, start: Label?) {
            sourceLocation = sourceLocation.copy(lineNumber = line)
            super.visitLineNumber(line, start)
        }

        /**
         * Extract information about provided label.
         */
        override fun visitLabel(label: Label) {
            visit(CodeLabel(label), defaultFirst = true) {
                super.visitLabel(label)
            }
        }

        /**
         * Extract information about provided annotations.
         */
        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
            visitMemberAnnotation(desc)
            return super.visitAnnotation(desc, visible)
        }

        /**
         * Write any new method body code, assuming the definition providers
         * have provided any. This handler will not be visited if this method
         * has no existing code.
         */
        override fun visitCode() {
            tryReplaceMethodBody()
            visit(MethodEntry(method)) {
                super.visitCode()
            }
        }

        /**
         * Extract information about provided field access instruction.
         */
        override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
            recordMemberReference(owner, name, desc)
            visit(MemberAccessInstruction(opcode, owner, name, desc, isMethod = false)) {
                super.visitFieldInsn(opcode, owner, name, desc)
            }
        }

        /**
         * Extract information about provided method invocation instruction.
         */
        override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
            recordMemberReference(owner, name, desc)
            visit(MemberAccessInstruction(opcode, owner, name, desc, itf, isMethod = true)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf)
            }
        }

        /**
         * Extract information about provided dynamic invocation instruction.
         */
        override fun visitInvokeDynamicInsn(name: String, desc: String, bsm: Handle?, vararg bsmArgs: Any?) {
            val module = configuration.memberModule
            visit(DynamicInvocationInstruction(
                    name, desc, module.numberOfArguments(desc), module.returnsValueOrReference(desc)
            )) {
                super.visitInvokeDynamicInsn(name, desc, bsm, *bsmArgs)
            }
        }

        /**
         * Extract information about provided jump instruction.
         */
        override fun visitJumpInsn(opcode: Int, label: Label) {
            visit(BranchInstruction(opcode, label)) {
                super.visitJumpInsn(opcode, label)
            }
        }

        /**
         * Extract information about provided instruction (general instruction with no operands).
         */
        override fun visitInsn(opcode: Int) {
            visit(Instruction(opcode)) {
                super.visitInsn(opcode)
            }
        }

        /**
         * Extract information about provided instruction (general instruction with one operand).
         */
        override fun visitIntInsn(opcode: Int, operand: Int) {
            visit(IntegerInstruction(opcode, operand)) {
                super.visitIntInsn(opcode, operand)
            }
        }

        /**
         * Extract information about provided type instruction (e.g., [Opcodes.NEW], [Opcodes.ANEWARRAY],
         * [Opcodes.INSTANCEOF] and [Opcodes.CHECKCAST]).
         */
        override fun visitTypeInsn(opcode: Int, type: String) {
            recordTypeReference(type)
            visit(TypeInstruction(opcode, type)) {
                try {
                    super.visitTypeInsn(opcode, type)
                } catch (exception: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid name used in type instruction; $type", exception)
                }
            }
        }

        /**
         * Extract information about provided try-catch/finally block.
         */
        override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) {
            val block = if (type != null) {
                TryCatchBlock(type, handler)
            } else {
                TryFinallyBlock(handler)
            }
            visit(block) {
                super.visitTryCatchBlock(start, end, handler, type)
            }
        }

        /**
         * Extract information about provided table switch instruction.
         */
        override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
            visit(TableSwitchInstruction(min, max, dflt, labels.toList())) {
                super.visitTableSwitchInsn(min, max, dflt, *labels)
            }
        }

        /**
         * Extract information about provided increment instruction.
         */
        override fun visitIincInsn(`var`: Int, increment: Int) {
            visit(IntegerInstruction(Opcodes.IINC, increment)) {
                super.visitIincInsn(`var`, increment)
            }
        }

        /**
         * Transform values loaded from the constants pool.
         */
        override fun visitLdcInsn(value: Any) {
            visit(ConstantInstruction(value), defaultFirst = true) {
                super.visitLdcInsn(value)
            }
        }

        /**
         * Finish visiting this method, writing any new method body byte-code
         * if we haven't written it already. This would (presumably) only happen
         * for methods that previously had no body, e.g. native methods.
         */
        override fun visitEnd() {
            tryReplaceMethodBody()
            super.visitEnd()
        }

        private fun tryReplaceMethodBody() {
            if (method.body.isNotEmpty() && (mv != null)) {
                EmitterModule(mv, configuration).apply {
                    for (body in method.body) {
                        body(this)
                    }
                }
                mv.visitMaxs(-1, -1)
                mv.visitEnd()
                mv = null
            }
        }

        /**
         * Helper function used to streamline the access to an instruction and to catch any related processing errors.
         */
        private fun visit(instruction: Instruction, defaultFirst: Boolean = false, defaultAction: () -> Unit) {
            val emitterModule = EmitterModule(mv ?: StubMethodVisitor(), configuration)
            if (defaultFirst) {
                defaultAction()
            }
            val success = captureExceptions {
                visitInstruction(currentMember!!, emitterModule, instruction)
            }
            if (!defaultFirst) {
                if (success && emitterModule.emitDefaultInstruction) {
                    defaultAction()
                }
            }
        }

    }

    /**
     * Visitor used to traverse and analyze a field.
     */
    private inner class FieldVisitorImpl(
            targetVisitor: FieldVisitor
    ) : FieldVisitor(API_VERSION, targetVisitor) {

        /**
         * Extract information about provided annotations.
         */
        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
            visitMemberAnnotation(desc)
            return super.visitAnnotation(desc, visible)
        }

    }

    private inner class StubMethodVisitor : MethodVisitor(API_VERSION, null)

    companion object {

        /**
         * The API version of ASM.
         */
        const val API_VERSION: Int = Opcodes.ASM7

    }

}
