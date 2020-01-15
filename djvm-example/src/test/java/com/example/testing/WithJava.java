package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface WithJava {

    @NotNull
    static <T,R> R run(TypedTaskFactory taskFactory, Class<? extends Function<T,R>> taskClass, T input) {
        try {
            return taskFactory.create(taskClass).apply(input);
        } catch(Exception e) {
            throw asRuntime(e);
        }
    }

    @NotNull
    static RuntimeException asRuntime(Throwable t) {
        return (t instanceof RuntimeException) ? (RuntimeException) t : new RuntimeException(t.getMessage(), t);
    }
}
