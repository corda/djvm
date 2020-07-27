package net.corda.djvm;

/**
 * Allows test code to reset the {@link SandboxRuntimeContext}.
 * Deliberately package private.
 */
interface ResetVisitor {
    void visit(Runnable action);
}

