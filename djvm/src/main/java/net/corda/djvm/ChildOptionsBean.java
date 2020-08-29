package net.corda.djvm;

import net.corda.djvm.api.AnalysisOptions;
import net.corda.djvm.api.ChildOptions;
import net.corda.djvm.api.ConfigurationOptions;
import net.corda.djvm.rewiring.ByteCode;
import net.corda.djvm.rewiring.ByteCodeKey;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Internal bean to "downcast" the {@link Consumer<AnalysisOptions>}
 * object into {@link Consumer<ChildOptions>} using a dynamic proxy.
 *
 * Written in Java so that it can be package-private.
 */
class ChildOptionsBean implements ConfigurationOptions, Consumer<AnalysisOptions> {
    private final Consumer<? super ChildOptions> configure;
    private ConcurrentMap<ByteCodeKey, ByteCode> externalCache;

    ChildOptionsBean(Consumer<? super ChildOptions> configure) {
        this.configure = configure;
    }

    @Override
    public void accept(AnalysisOptions analysis) {
        ChildOptions options = (ChildOptions) Proxy.newProxyInstance(
            ChildOptions.class.getClassLoader(),
            new Class[] { ChildOptions.class },
            new Handler(this, analysis)
        );
        configure.accept(options);
    }

    @Override
    public void setExternalCache(ConcurrentMap<ByteCodeKey, ByteCode> externalCache) {
        this.externalCache = externalCache;
    }

    public ConcurrentMap<ByteCodeKey, ByteCode> getExternalCache() {
        return externalCache;
    }

    private static class Handler implements InvocationHandler {
        private final ConfigurationOptions configuration;
        private final AnalysisOptions analysis;

        Handler(ConfigurationOptions configuration, AnalysisOptions analysis) {
            this.configuration = configuration;
            this.analysis = analysis;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final Class<?> targetClass = method.getDeclaringClass();
            final Object target;

            if (targetClass.isInstance(analysis)) {
                target = analysis;
            } else if (targetClass.isInstance(configuration)) {
                target = configuration;
            } else {
                target = proxy;
            }

            return method.invoke(target, args);
        }
    }
}
