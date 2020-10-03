package net.corda.djvm;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

final class ClassResetContext {
    private static final int MAGIC_HASH_OFFSET = 0xfed_c0de;

    private final SandboxClassResetter resetter;
    private final Resettables setupPhase;
    private final Resettables runPhase;
    private final Map<String, Object> internStrings;
    private final Map<Integer, Integer> hashCodes;
    private Function<? super Integer, ? extends Integer> nextHashOffset;
    private int objectCounter;

    private volatile Resettables current;

    ClassResetContext() {
        resetter = new SandboxClassResetter();
        setupPhase = new Resettables();
        runPhase = new Resettables();
        hashCodes = new HashMap<>();
        internStrings = new HashMap<>();
        nextHashOffset = this::decrementHashOffset;
        current = setupPhase;
    }

    void add(MethodHandle resetMethod, @NotNull List<Field> finalFields) {
        current.add(new Resettable(resetMethod, finalFields.isEmpty() ? emptyList() : unmodifiableList(finalFields)));
    }

    void add(MethodHandle resetMethod) {
        current.add(new Resettable(resetMethod));
    }

    synchronized void reset() throws Throwable {
        nextHashOffset = this::decrementHashOffset;
        objectCounter = 0;
        hashCodes.clear();
        internStrings.clear();
        resetter.reset(current = setupPhase);
    }

    synchronized void ready() throws Throwable {
        if (current == runPhase) {
            throw new IllegalStateException("May only be invoked once.");
        }
        nextHashOffset = this::incrementHashOffset;
        objectCounter = 0;
        resetter.reset(current = runPhase);
    }

    @NotNull
    Object intern(@NotNull String key, @NotNull Object value) {
        Object mapValue = internStrings.get(key);
        if (mapValue == null) {
            internStrings.put(key, value);
            mapValue = value;
        }
        return mapValue;
    }

    // TODO Instead of using a magic offset below, one could take in a per-context seed
    int getHashCodeFor(int nativeHashCode) {
        return hashCodes.computeIfAbsent(nativeHashCode, nextHashOffset);
    }

    private int incrementHashOffset(int key) {
        return ++objectCounter + MAGIC_HASH_OFFSET;
    }

    private int decrementHashOffset(int key) {
        return --objectCounter + MAGIC_HASH_OFFSET;
    }
}
