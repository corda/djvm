package net.corda.djvm;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.List;

final class Resettable {
    private final MethodHandle resetMethod;
    private final List<Field> finalFields;

    Resettable(MethodHandle resetMethod, List<Field> finalFields) {
        this.resetMethod = resetMethod;
        this.finalFields = finalFields;
    }

    MethodHandle getResetMethod() {
        return resetMethod;
    }

    List<Field> getFinalFields() {
        return finalFields;
    }
}
