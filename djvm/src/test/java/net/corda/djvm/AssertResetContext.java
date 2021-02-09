package net.corda.djvm;

import net.corda.djvm.ClassResetContext.View;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class AssertResetContext {
    private final View view;

    AssertResetContext(View view) {
        this.view = view;
    }

    public AssertResetContext withResetMethodHandles(@NotNull Consumer<List<String>> assertion) {
        assertion.accept(view.getResetMethodHandles());
        return this;
    }

    public AssertResetContext withInternStrings(@NotNull Consumer<List<String>> assertion) {
        assertion.accept(view.getInternStrings());
        return this;
    }

    public AssertResetContext withHashCodeCount(@NotNull IntConsumer assertion) {
        assertion.accept(view.getHashCodeCount());
        return this;
    }
}
