package net.corda.djvm.code.impl;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import sandbox.java.lang.DJVMClass;
import sandbox.java.lang.DJVMClassLoader;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

class MethodThunkTest {
    private static final int THUNK_MASK = ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_STATIC;

    private static final Map<String, List<Method>> CLASS_STATIC_METHODS = mapStaticByName(Class.class);
    private static final Map<String, List<Method>> CLASS_VIRTUAL_METHODS = mapVirtualByName(Class.class);
    private static final Map<String, List<Method>> CLASS_THUNK_METHODS = mapStaticByName(DJVMClass.class);

    private static final Map<String, List<Method>> CLASSLOADER_STATIC_METHODS = mapStaticByName(ClassLoader.class);
    private static final Map<String, List<Method>> CLASSLOADER_VIRTUAL_METHODS = mapVirtualByName(ClassLoader.class);
    private static final Map<String, List<Method>> CLASSLOADER_THUNK_METHODS = mapStaticByName(DJVMClassLoader.class);

    private static Map<String, List<Method>> mapVirtualByName(@NotNull Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> ((method.getModifiers() & THUNK_MASK) | ACC_PUBLIC) == ACC_PUBLIC)
            .collect(groupingBy(Method::getName));
    }

    private static Map<String, List<Method>> mapStaticByName(@NotNull Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> ((method.getModifiers() & THUNK_MASK) | ACC_PUBLIC) == (ACC_STATIC | ACC_PUBLIC))
            .collect(groupingBy(Method::getName));
    }

    /**
     * Match thunks for virtual methods in {@link java.lang.Class}.
     */
    static class ClassVirtualThunkSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Thunks.getClassVirtual().stream().map(Arguments::of);
        }
    }

    @ParameterizedTest(name = "class virtual thunk: {index} => DJVMClass.{0}")
    @ArgumentsSource(ClassVirtualThunkSource.class)
    void validateClassVirtualThunks(String thunkName) {
        List<Method> actualMethods = CLASS_VIRTUAL_METHODS.get(thunkName);
        assertNotNull(actualMethods, thunkName + " method not found in Class");

        List<Method> thunkMethods = CLASS_THUNK_METHODS.get(thunkName);
        assertNotNull(thunkMethods, thunkName + " method not found in DJVMClass");
        assertThat(thunkMethods).isNotEmpty();

        ThunkMatcher matcher = new ThunkMatcher(Class.class, actualMethods);
        for (Method thunkMethod : thunkMethods) {
            Method[] matches = matcher.findVirtualThunk(thunkMethod);
            assertEquals(1, matches.length, "No corresponding Class method for " + thunkMethod);
        }
    }

    /**
     * Match thunks for static methods in {@link java.lang.Class}.
     */
    static class ClassStaticThunkSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Thunks.getClassStatic().stream().map(Arguments::of);
        }
    }

    @ParameterizedTest(name = "class static thunk: {index} => DJVMClass.{0}")
    @ArgumentsSource(ClassStaticThunkSource.class)
    void validateClassStaticThunks(String thunkName) {
        List<Method> actualMethods = CLASS_STATIC_METHODS.get(thunkName);
        assertNotNull(actualMethods, thunkName + " method not found in Class");

        List<Method> thunkMethods = CLASS_THUNK_METHODS.get(thunkName);
        assertNotNull(thunkMethods, thunkName + " method not found in DJVMClass");
        assertThat(thunkMethods).isNotEmpty();

        ThunkMatcher matcher = new ThunkMatcher(Class.class, actualMethods);
        for (Method thunkMethod : thunkMethods) {
            Method[] matches = matcher.findStaticThunk(thunkMethod);
            assertEquals(1, matches.length, "No corresponding Class method for " + thunkMethod);
        }
    }

    /**
     * Match thunks for virtual methods in {@link java.lang.ClassLoader}.
     */
    static class ClassLoaderVirtualThunkSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Thunks.getClassLoaderVirtual().stream().map(Arguments::of);
        }
    }

    @ParameterizedTest(name = "classloader virtual thunk: {index} => DJVMClassLoader.{0}")
    @ArgumentsSource(ClassLoaderVirtualThunkSource.class)
    void validateClassLoaderVirtualThunks(String thunkName) {
        List<Method> actualMethods = CLASSLOADER_VIRTUAL_METHODS.get(thunkName);
        assertNotNull(actualMethods, thunkName + " method not found in ClassLoader");

        List<Method> thunkMethods = CLASSLOADER_THUNK_METHODS.get(thunkName);
        assertNotNull(thunkMethods, thunkName + " method not found in DJVMClassLoader");
        assertThat(thunkMethods).isNotEmpty();

        ThunkMatcher matcher = new ThunkMatcher(ClassLoader.class, actualMethods);
        for (Method thunkMethod : thunkMethods) {
            Method[] matches = matcher.findVirtualThunk(thunkMethod);
            assertEquals(1, matches.length, "No corresponding ClassLoader method for " + thunkMethod);
        }
    }

    /**
     * Match thunks for static methods in {@link java.lang.ClassLoader}.
     */
    static class ClassLoaderStaticThunkSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Thunks.getClassLoaderStatic().stream().map(Arguments::of);
        }
    }

    @ParameterizedTest(name = "classloader static thunk: {index} => DJVMClassLoader.{0}")
    @ArgumentsSource(ClassLoaderStaticThunkSource.class)
    void validateClassLoaderStaticThunks(String thunkName) {
        List<Method> actualMethods = CLASSLOADER_STATIC_METHODS.get(thunkName);
        assertNotNull(actualMethods, thunkName + " method not found in ClassLoader");

        List<Method> thunkMethods = CLASSLOADER_THUNK_METHODS.get(thunkName);
        assertNotNull(thunkMethods, thunkName + " method not found in DJVMClassLoader");
        assertThat(thunkMethods).isNotEmpty();

        ThunkMatcher matcher = new ThunkMatcher(ClassLoader.class, actualMethods);
        for (Method thunkMethod : thunkMethods) {
            Method[] matches = matcher.findStaticThunk(thunkMethod);
            assertEquals(1, matches.length, "No corresponding ClassLoader method for " + thunkMethod);
        }
    }

    /**
     * Logic class to identify which method in the base class we are thunking.
     */
    private static class ThunkMatcher {
        private static final String SANDBOX_PREFIX = "sandbox.";
        private final Class<?> baseType;
        private final Collection<Method> candidates;

        ThunkMatcher(Class<?> baseType, Collection<Method> candidates) {
            this.baseType = baseType;
            this.candidates = candidates;
        }

        Method[] findStaticThunk(@NotNull Method thunkMethod) {
            String[] expectedParameterTypes = getActualTypeNames(thunkMethod.getParameterTypes());
            return findMatches(thunkMethod, expectedParameterTypes);
        }

        Method[] findVirtualThunk(@NotNull Method thunkMethod) {
            String[] actualParameterTypes = getActualTypeNames(thunkMethod.getParameterTypes());
            assertThat(actualParameterTypes).isNotEmpty();
            assertThat(actualParameterTypes[0]).isEqualTo(baseType.getName());

            String[] expectedParameterTypes = Arrays.copyOfRange(actualParameterTypes, 1, actualParameterTypes.length);
            return findMatches(thunkMethod, expectedParameterTypes);
        }

        @NotNull
        private Method[] findMatches(@NotNull Method thunkMethod, String[] expectedTypes) {
            int expectedModifiers = thunkMethod.getModifiers() & THUNK_MASK;
            return candidates.stream()
                .filter(m -> thunked(m.getModifiers()) == expectedModifiers)
                .filter(m -> Arrays.deepEquals(getParameterTypes(m), expectedTypes))
                .toArray(Method[]::new);
        }

        private static int thunked(int flags) {
            return (flags | ACC_STATIC) & THUNK_MASK;
        }

        @NotNull
        private static String[] getParameterTypes(@NotNull Method method) {
            return typeNamesFor(method.getParameterTypes())
                .toArray(String[]::new);
        }

        @NotNull
        private static String[] getActualTypeNames(Class<?>[] parameterTypes) {
            return typeNamesFor(parameterTypes)
                .map(ThunkMatcher::removeSandbox)
                .toArray(String[]::new);
        }

        private static Stream<String> typeNamesFor(Class<?>[] parameterTypes) {
            return Arrays.stream(parameterTypes).map(Class::getName);
        }

        private static String removeSandbox(@NotNull String className) {
            return className.startsWith(SANDBOX_PREFIX)
                    ? className.substring(SANDBOX_PREFIX.length()) : className;
        }
    }
}
