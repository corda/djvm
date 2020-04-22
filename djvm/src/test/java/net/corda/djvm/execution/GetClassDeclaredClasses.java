package net.corda.djvm.execution;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;

@SuppressWarnings("unused")
public class GetClassDeclaredClasses implements Function<String, String[]> {
    interface NestedInterface extends Cloneable, Serializable {}
    public static class NestedException extends Exception implements NestedInterface {}

    @Override
    public String[] apply(String unused) {
        return Arrays.stream(GetClassDeclaredClasses.class.getDeclaredClasses())
            .map(Class::getName)
            .toArray(String[]::new);
    }
}
