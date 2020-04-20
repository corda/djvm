package net.corda.djvm;

import net.corda.djvm.costing.ThresholdViolationError;
import net.corda.djvm.rules.RuleViolationError;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

/**
 * Whitelist this {@link Utilities} class inside the sandbox to allow
 * tests to invoke these functions.
 */
public final class Utilities {
    public static final String CANNOT_CATCH = "Can't catch this!";

    private Utilities() {
    }

    public static void throwRuleViolationError() {
        throw new RuleViolationError(CANNOT_CATCH);
    }

    public static void throwThresholdViolationError() {
        throw new ThresholdViolationError(CANNOT_CATCH);
    }

    @NotNull
    public static <T> T newInstance(@NotNull Class<T> clazz)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return clazz.getDeclaredConstructor().newInstance();
    }
}
