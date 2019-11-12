package net.corda.djvm.rewiring;

import net.corda.djvm.analysis.AnalysisConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.unmodifiableSet;

/**
 * An internal cache of class byte-code, indexed by the class's binary name.
 * It has been written in Java so that both {@link #update} and {@link #clear}
 * can be package private.
 */
public final class ByteCodeCache {
    private final ConcurrentMap<String, ByteCode> byteCodeCache;
    private final ByteCodeCache parent;

    public ByteCodeCache(ByteCodeCache parent) {
        this.byteCodeCache = new ConcurrentHashMap<>();
        this.parent = parent;
    }

    public ByteCodeCache getParent() {
        return parent;
    }

    public ByteCode get(String name) {
        return byteCodeCache.get(name);
    }

    @NotNull
    public Set<String> getClassNames() {
        return unmodifiableSet(byteCodeCache.keySet());
    }

    void update(@NotNull Map<String, ByteCode> loadedClasses) {
        for (Map.Entry<String, ByteCode> entry : loadedClasses.entrySet()) {
            byteCodeCache.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    void clear() {
        byteCodeCache.clear();
    }

    /**
     * Create a chain of {@link ByteCodeCache} objects that will underlie
     * the sandbox's chain of {@link SandboxClassLoader} objects.
     * @param configuration An {@link AnalysisConfiguration} object.
     * @return A chain of {@link ByteCodeCache} objects with the same length as the
     * chain of {@link AnalysisConfiguration} objects.
     */
    @NotNull
    public static ByteCodeCache createFor(@NotNull AnalysisConfiguration configuration) {
        AnalysisConfiguration parentConfig = configuration.getParent();
        return new ByteCodeCache(parentConfig != null ? createFor(parentConfig) : null);
    }
}
