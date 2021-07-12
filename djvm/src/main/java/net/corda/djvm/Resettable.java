package net.corda.djvm;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.List;

import static java.util.Collections.emptyList;

final class Resettable {
    private static final Object[] NONE = {};
    private final MethodHandle resetMethod;
    private final List<Field> finalFields;
    private final Object[] resetArgs;

    Resettable(MethodHandle resetMethod, List<Field> finalFields) {
        this.resetMethod = resetMethod;
        this.finalFields = finalFields;
        this.resetArgs = NONE;
    }

    Resettable(MethodHandle resetMethod, Object... resetArgs) {
        this.resetMethod = resetMethod;
        this.finalFields = emptyList();
        this.resetArgs = resetArgs;
    }

    MethodHandle getResetMethod() {
        return resetMethod;
    }

    List<Field> getFinalFields() {
        return finalFields;
    }

    boolean hasArgs() {
        return resetArgs.length != 0;
    }

    void invokeWithArgs() throws Throwable {
        resetMethod.invokeWithArguments(resetArgs);
    }
}
