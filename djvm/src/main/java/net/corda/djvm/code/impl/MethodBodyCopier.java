package net.corda.djvm.code.impl;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

class MethodBodyCopier extends MethodVisitor {
    private final MethodNode node;
    private final int parameterOffset;

    MethodBodyCopier(int api, MethodVisitor mv, MethodNode node, int parameterOffset) {
        super(api, mv);
        this.node = node;
        this.parameterOffset = parameterOffset;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        node.visitCode();
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        node.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        node.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        super.visitFrame(type, numLocal, local, numStack, stack);
        node.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitIincInsn(int varIdx, int increment) {
        super.visitIincInsn(varIdx, increment);
        node.visitIincInsn(varIdx + parameterOffset, increment);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        node.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        node.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArgs) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArgs);
        node.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArgs);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        node.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        node.visitLabel(label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
        node.visitLdcInsn(value);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        node.visitLineNumber(line, start);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
        node.visitLocalVariable(name, descriptor, signature, start, end, index + parameterOffset);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        node.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        node.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);
        node.visitParameter(name, access);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        node.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        node.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        node.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitVarInsn(int opcode, int varIdx) {
        super.visitVarInsn(opcode, varIdx);
        node.visitVarInsn(opcode, varIdx + parameterOffset);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute);
        node.visitAttribute(attribute);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
        node.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        node.visitEnd();
    }
}
