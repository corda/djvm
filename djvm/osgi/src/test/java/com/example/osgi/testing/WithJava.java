package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

interface WithJava {
    @SuppressWarnings("unchecked")
    @NotNull
    static <T, R> Function<T, R> create(@NotNull SandboxClassLoader classLoader, String taskName) throws Exception {
        Function<? super Object, ? extends Function<T, R>> taskFactory = (Function<? super Object, ? extends Function<T, R>>) classLoader.createTaskFactory();
        Class<? extends Function<T, R>> taskClass = (Class<? extends Function<T, R>>) classLoader.toSandboxClass(taskName);
        return taskFactory.apply(taskClass.getDeclaredConstructor().newInstance());
    }

    @NotNull
    static <T, R> R run(@NotNull SandboxClassLoader classLoader, String taskName, T input) throws Exception {
        return WithJava.<T, R>create(classLoader, taskName).apply(input);
    }
}
