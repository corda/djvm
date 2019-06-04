package net.corda.djvm.code

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.references.MethodBody
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import sandbox.net.corda.djvm.costing.RuntimeCostAccounter

/**
 * Helper functions for emitting code to a method body.
 *
 * @property methodVisitor The underlying visitor which controls all the byte code for the current method.
 * @property configuration
 */
class EmitterModule(
        private val methodVisitor: MethodVisitor,
        private val configuration: AnalysisConfiguration
) {

    /**
     * Indicates whether the default instruction in the currently processed block is to be emitted or not.
     */
    var emitDefaultInstruction: Boolean = true
        private set

    /**
     * Indicates whether any custom code has been emitted in the applicable context.
     */
    var hasEmittedCustomCode: Boolean = false
        private set

    /**
     * Emit instruction for creating a new object of type [typeName].
     */
    fun new(typeName: String, opcode: Int = NEW) {
        hasEmittedCustomCode = true
        methodVisitor.visitTypeInsn(opcode, typeName)
    }

    /**
     * Emit instruction for creating a new object of type [T].
     */
    inline fun <reified T> new() {
        new(Type.getInternalName(T::class.java))
    }

    /**
     * Emit instruction for loading a constant onto the stack.
     */
    fun loadConstant(constant: Any) {
        hasEmittedCustomCode = true
        methodVisitor.visitLdcInsn(constant)
    }

    /**
     * Emit instruction for invoking a static method.
     */
    fun invokeStatic(owner: String, name: String, descriptor: String, isInterface: Boolean = false) {
        hasEmittedCustomCode = true
        methodVisitor.visitMethodInsn(INVOKESTATIC, owner, name, descriptor, isInterface)
    }

    /**
     * Emit instruction for invoking a virtual method.
     */
    fun invokeVirtual(owner: String, name: String, descriptor: String, isInterface: Boolean = false) {
        hasEmittedCustomCode = true
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, owner, name, descriptor, isInterface)
    }

    /**
     * Emit instruction for invoking a special method, e.g. a constructor or a method on a super-type.
     */
    fun invokeSpecial(owner: String, name: String, descriptor: String, isInterface: Boolean = false) {
        hasEmittedCustomCode = true
        methodVisitor.visitMethodInsn(INVOKESPECIAL, owner, name, descriptor, isInterface)
    }

    /**
     * Emit instruction for invoking a special method on class [T], e.g. a constructor or a method on a super-type.
     */
    @Suppress("unused")
    inline fun <reified T> invokeSpecial(name: String, descriptor: String, isInterface: Boolean = false) {
        invokeSpecial(Type.getInternalName(T::class.java), name, descriptor, isInterface)
    }

    fun invokeInterface(owner: String, name: String, descriptor: String) {
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, owner, name, descriptor, true)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instruction for storing a value into a static field.
     */
    fun putStatic(owner: String, name: String, descriptor: String) {
        methodVisitor.visitFieldInsn(PUTSTATIC, owner, name, descriptor)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instruction for popping one element off the stack.
     */
    fun pop() {
        hasEmittedCustomCode = true
        methodVisitor.visitInsn(POP)
    }

    /**
     * Emit instruction for duplicating the top of the stack.
     */
    fun duplicate() {
        hasEmittedCustomCode = true
        methodVisitor.visitInsn(DUP)
    }

    /**
     * Emit instruction for pushing an object reference
     * from a register onto the stack.
     */
    fun pushObject(regNum: Int) {
        methodVisitor.visitVarInsn(ALOAD, regNum)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instruction for pushing an integer value
     * from a register onto the stack.
     */
    fun pushInteger(regNum: Int) {
        methodVisitor.visitVarInsn(ILOAD, regNum)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instructions to rearrange the stack as follows:
     *     [W1]    [W3]
     *     [W2] -> [W1]
     *     [w3]    [W2]
     */
    fun raiseThirdWordToTop() {
        methodVisitor.visitInsn(DUP2_X1)
        methodVisitor.visitInsn(POP2)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instructions to rearrange the stack as follows:
     *     [W1]    [W2]
     *     [W2] -> [W3]
     *     [W3]    [W1]
     */
    fun sinkTopToThirdWord() {
        methodVisitor.visitInsn(DUP_X2)
        methodVisitor.visitInsn(POP)
        hasEmittedCustomCode = true
    }

    /**
     * Emit a sequence of instructions for instantiating and throwing an exception based on the provided message.
     */
    fun <T : Throwable> throwException(exceptionType: Class<T>, message: String) {
        val exceptionName = Type.getInternalName(exceptionType)
        new(exceptionName)
        methodVisitor.visitInsn(DUP)
        methodVisitor.visitLdcInsn(message)
        invokeSpecial(exceptionName, "<init>", "(Ljava/lang/String;)V")
        methodVisitor.visitInsn(ATHROW)
    }

    inline fun <reified T : Throwable> throwException(message: String) = throwException(T::class.java, message)

    /**
     * Attempt to cast the object on the top of the stack to the given class.
     */
    fun castObjectTo(className: String) {
        methodVisitor.visitTypeInsn(CHECKCAST, className)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instruction for returning from "void" method.
     */
    fun returnVoid() {
        methodVisitor.visitInsn(RETURN)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instruction for a function that returns an object reference.
     */
    fun returnObject() {
        methodVisitor.visitInsn(ARETURN)
        hasEmittedCustomCode = true
    }

    /**
     * Emit instructions for a new line number.
     */
    fun lineNumber(line: Int) {
        val label = Label()
        methodVisitor.visitLabel(label)
        methodVisitor.visitLineNumber(line, label)
        hasEmittedCustomCode = true
    }

    /**
     * This determines which [sandbox.java.lang.Throwable] type we must up-cast
     * the return value of [sandbox.java.lang.DJVM.catch] to.
     */
    fun commonThrowableClassOf(classNames: Iterable<String>): String {
        val classIterator = classNames.iterator()
        var throwable = classIterator.next()
        while (classIterator.hasNext() && throwable != THROWABLE_NAME) {
            throwable = commonSuperclassOf(throwable, classIterator.next())
            if (throwable == OBJECT_NAME) {
                throw ClassCastException("Classes $classNames must all be Throwable")
            }
        }
        return throwable
    }

    /**
     * The ASM library uses [net.corda.djvm.rewiring.SandboxClassWriter.getCommonSuperClass]
     * to compute the stack frames for the classes that it generates,
     * c.f. [org.objectweb.asm.ClassWriter.COMPUTE_FRAMES]. We need to mirror that algorithm
     * here so that the [sandbox.java.lang.Throwable] can be assigned to the local variable
     * defined in the method's local variable table. Unfortunately the local variable table
     * is stored after the method's byte-code, and so we cannot just read the variable's type
     * from there.
     */
    private fun commonSuperclassOf(type1: String, type2: String): String {
        return when {
            type1 == OBJECT_NAME -> type1
            type2 == OBJECT_NAME -> type2
            else -> {
                val class1 = configuration.supportingClassLoader.loadClass(type1.asPackagePath)
                val class2 = configuration.supportingClassLoader.loadClass(type2.asPackagePath)
                when {
                    class1.isAssignableFrom(class2) -> type1
                    class2.isAssignableFrom(class1) -> type2
                    class1.isInterface || class2.isInterface -> OBJECT_NAME
                    else -> {
                        var commonClass = class1
                        do {
                            commonClass = commonClass.superclass
                        } while (!commonClass.isAssignableFrom(class2))
                        Type.getInternalName(commonClass)
                    }
                }
            }
        }
    }

    /**
     * Write the bytecode from these [MethodBody] objects as provided.
     */
    fun writeByteCode(bodies: Iterable<MethodBody>) {
        for (body in bodies) {
            body(this)
        }
    }

    /**
     * Tell the code writer not to emit the default instruction.
     */
    fun preventDefault() {
        emitDefaultInstruction = false
    }

    /**
     * Emit instruction for invoking a method on the static runtime cost accounting and instrumentation object.
     */
    fun invokeInstrumenter(methodName: String, methodSignature: String) {
        invokeStatic(RuntimeCostAccounter.TYPE_NAME, methodName, methodSignature)
    }

}