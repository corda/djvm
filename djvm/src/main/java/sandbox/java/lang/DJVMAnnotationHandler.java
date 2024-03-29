package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sandbox.java.lang.annotation.Annotation;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

/**
 * Handler for {@link Annotation} dynamic proxies. The proxy class itself
 * has super-class {@link java.lang.Object} instead of {@link Object},
 */
final class DJVMAnnotationHandler implements InvocationHandler {
    private static final Set<java.lang.String> BASE_METHODS = unmodifiableSet(
        new HashSet<>(asList("toString", "hashCode", "annotationType"))
    );

    /**
     * The hash-code values for the underlying JVM annotations will
     * depend on the JVM's own private implementation, and so we
     * cannot rely on them to be repeatable. Map them to values that
     * we can control instead.
     * @see System#identityHashCode(java.lang.Object)
     */
    private static final int ANNOTATION_HASH_OFFSET = 0xbeef_c0de;
    private static final Map<java.lang.Integer, java.lang.Integer> hashCodes = new HashMap<>();
    private static int annotationCounter;

    private static int hashCodeFor(int jvmHashCode) {
        return hashCodes.computeIfAbsent(
            jvmHashCode, hash -> ++annotationCounter + ANNOTATION_HASH_OFFSET
        );
    }

    @SuppressWarnings("unused")
    private static void reset(BiConsumer<java.lang.Object, java.lang.String> resetter) {
        resetter.accept(new HashMap<>(), "hashCodes");
        annotationCounter = 0;
    }

    static {
        DJVM.forReset(MethodHandles.lookup(), "reset");
    }

    /**
     * Applies the appropriate "boxing" to an annotation method value
     * so that it can be returned from {@link Method#getDefaultValue}.
     */
    @Nullable
    static java.lang.Object getDefaultValue(
        @NotNull Class<?> resultType,
        @NotNull java.lang.Object jvmResult
    ) {
        java.lang.Object result = MethodValue.toDJVM(resultType, jvmResult);
        if (result != null && resultType.isPrimitive()) {
            if (resultType == int.class) {
                result = Integer.valueOf((java.lang.Integer) result);
            } else if (resultType == long.class) {
                result = Long.valueOf((java.lang.Long) result);
            } else if (resultType == short.class) {
                result = Short.valueOf((java.lang.Short) result);
            } else if (resultType == byte.class) {
                result = Byte.valueOf((java.lang.Byte) result);
            } else if (resultType == char.class) {
                result = Character.valueOf((java.lang.Character) result);
            } else if (resultType == boolean.class) {
                result = Boolean.valueOf((java.lang.Boolean) result);
            } else if (resultType == double.class) {
                result = Double.valueOf((java.lang.Double) result);
            } else if (resultType == float.class) {
                result = Float.valueOf((java.lang.Float) result);
            }
        }
        return result;
    }

    /**
     * Caches the values of each annotation method so that we
     * only need to compute them once. This also guarantees
     * that each invocation always returns the same object.
     */
    private final static class MethodValue {
        private static final java.lang.String FORBIDDEN_NULL = " cannot return 'null'";

        private final Method method;
        private java.lang.Object value;

        MethodValue(Method method) {
            this.method = method;
        }

        @NotNull
        java.lang.Object getValueFor(
            java.lang.annotation.Annotation underlying,
            final Class<? extends Annotation> annotationType
        ) throws java.lang.Throwable {
            // We know that these annotation methods have no parameters,
            // and that all interface methods must be public.
            return (value == null)
                ? getValueFor(underlying, annotationType.getMethod(method.getName()))
                : value;
        }

        @NotNull
        java.lang.Object getValueFor(
            java.lang.annotation.Annotation underlying,
            Method sandboxMethod
        ) throws java.lang.Throwable {
            if (value == null) {
                java.lang.Object jvmResult;
                try {
                    jvmResult = method.invoke(underlying);
                } catch (InvocationTargetException e) {
                    Throwable t = DJVM.doCatch(e);
                    throw DJVM.fromDJVM(t.getCause());
                }

                Class<?> returnType = sandboxMethod.getReturnType();
                java.lang.Object result = toDJVM(returnType, jvmResult);
                value = result == null ? DJVM.fail(sandboxMethod + FORBIDDEN_NULL) : result;
            }
            return value;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        static java.lang.Object toDJVM(@NotNull Class<?> resultType, java.lang.Object jvmResult) {
            if (isNativeType(resultType)) {
                // Primitive types and classes don't need sandboxing.
                return jvmResult;
            } else if (resultType == String.class) {
                return String.toDJVM(java.lang.String.valueOf(jvmResult));
            } else if (DJVMClass.isEnum(resultType)) {
                return Enum.valueOf(
                    // The byte-code stores the name value as a String.
                    // Use this to look up the actual Enum value.
                    resultType.asSubclass(Enum.class), String.toDJVM(java.lang.String.valueOf(jvmResult))
                );
            } else if (resultType.isAnnotation()) {
                return DJVM.createDJVMAnnotation(
                    resultType.asSubclass(Annotation.class), (java.lang.annotation.Annotation) jvmResult
                );
            } else if (resultType.isArray() && jvmResult instanceof java.lang.Object[]) {
                final Class<?> componentType;
                try {
                    componentType = DJVM.toDJVMType(resultType.getComponentType());
                } catch (java.lang.Exception e) {
                    throw DJVM.toRuleViolationError(e);
                }
                return toDJVMArray(componentType, (java.lang.Object[]) jvmResult);
            } else {
                // Unrecognised type? This isn't going to work...
                return null;
            }
        }

        private static java.lang.Object[] toDJVMArray(Class<?> componentType, @NotNull java.lang.Object[] source) {
            java.lang.Object[] target = (java.lang.Object[]) Array.newInstance(componentType, source.length);
            for (int i = 0; i < source.length; ++i) {
                target[i] = toDJVM(componentType, source[i]);
            }
            return target;
        }

        private static boolean isNativeType(Class<?> type) {
            return (type == Class.class)
                || type.isPrimitive()
                || (type.isArray() && type.getComponentType().isPrimitive());
        }
    }

    private final Class<? extends Annotation> annotationType;
    private final java.lang.annotation.Annotation underlying;
    private final Map<java.lang.String, MethodValue> methods;
    private final int hashCode;
    private String stringValue;

    DJVMAnnotationHandler(
        @NotNull Class<? extends Annotation> annotationType,
        @NotNull java.lang.annotation.Annotation underlying
    ) {
        this.annotationType = annotationType;
        this.underlying = underlying;
        this.hashCode = hashCodeFor(underlying.hashCode());

        Map<java.lang.String, MethodValue> map = new LinkedHashMap<>();
        for (Method method : underlying.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && !BASE_METHODS.contains(method.getName())) {
                map.put(method.getName(), new MethodValue(method));
            }
        }
        this.methods = unmodifiableMap(map);
    }

    @NotNull
    private java.lang.String format(@NotNull java.lang.Object[] items) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (java.lang.Object item : items) {
            joiner.add(item.toString());
        }
        return joiner.toString();
    }

    @NotNull
    private java.lang.String format(Map.Entry<java.lang.String, MethodValue> method) {
        try {
            java.lang.Object value = method.getValue().getValueFor(underlying, annotationType);
            java.lang.String strValue = value instanceof java.lang.Object[] ?
                    format((java.lang.Object[]) value) : value.toString();
            return method.getKey() + '=' + strValue;
        } catch (java.lang.Throwable t) {
            throw DJVM.toRuleViolationError(t);
        }
    }

    private String createStringValue() {
        java.util.StringJoiner joiner = new StringJoiner(
            ",", '@' + annotationType.getName() + '(', ")"
        );
        for (Map.Entry<java.lang.String, MethodValue> method : methods.entrySet()) {
            joiner.add(format(method));
        }
        return String.toDJVM(joiner.toString());
    }

    private String getStringValue() {
        if (stringValue == null) {
            stringValue = createStringValue();
        }
        return stringValue;
    }

    @Override
    public java.lang.Object invoke(java.lang.Object proxy, @NotNull Method method, java.lang.Object[] args)
            throws java.lang.Throwable {
        final java.lang.String methodName = method.getName();
        switch (method.getParameterCount()) {
            case 1:
                if (methodName.equals("equals")) {
                    final java.lang.Object arg = args[0];
                    return (arg == proxy) ||
                        (annotationType.isInstance(arg) && underlying.equals(((Annotation) arg).jvmAnnotation()));
                }
                break;

            case 0:
                switch (methodName) {
                    case "jvmAnnotation":
                        return underlying;
                    case "hashCode":
                        return hashCode;
                    case "toString":
                        return getStringValue().toString();
                    case "toDJVMString":
                        return getStringValue();
                    case "annotationType":
                        return annotationType;
                    default:
                        final MethodValue targetMethod = methods.get(methodName);
                        if (targetMethod != null) {
                            return targetMethod.getValueFor(underlying, method);
                        }
                        break;
                }
                break;
        }
        throw DJVM.fail(method.toString());
    }
}
