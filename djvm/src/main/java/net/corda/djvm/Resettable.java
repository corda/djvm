package net.corda.djvm;

import java.lang.invoke.MethodHandle;

final class Resettable {
    private final MethodHandle resetMethod;

    Resettable(MethodHandle resetMethod) {
        this.resetMethod = resetMethod;
    }

    MethodHandle getResetMethod() {
        return resetMethod;
    }
}
