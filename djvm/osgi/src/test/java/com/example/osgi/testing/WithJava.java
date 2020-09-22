package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

interface WithJava {
    @SuppressWarnings("unchecked")
    @NotNull
    static <T,R> R run(SandboxClassLoader classLoader, String taskName, T input) throws Exception {
        Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = classLoader.createTaskFactory();
        Class<? extends Function<? super Object, ?>> taskClass = (Class<? extends Function<? super Object, ?>>) classLoader.toSandboxClass(taskName);
        return (R) taskFactory.apply(taskClass.getDeclaredConstructor().newInstance()).apply(input);
    }
}
