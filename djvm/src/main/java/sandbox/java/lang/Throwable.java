package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;
import sandbox.TaskTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Throwable extends Object implements Serializable {
    private static final StackTraceElement[] NO_STACK_TRACE = new StackTraceElement[0];
    private static final Throwable[] EMPTY_THROWABLE_ARRAY = new Throwable[0];
    private static final List<Throwable> DEFAULT_SUPPRESSED = emptyList();

    private String message;
    private Throwable cause;
    private StackTraceElement[] stackTrace;
    private List<Throwable> suppressedExceptions = DEFAULT_SUPPRESSED;

    public Throwable() {
        this.cause = this;
        fillInStackTrace();
    }

    public Throwable(String message) {
        this();
        this.message = message;
    }

    public Throwable(Throwable cause) {
        this.cause = cause;
        this.message = (cause == null) ? null : cause.toDJVMString();
        fillInStackTrace();
    }

    public Throwable(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
        fillInStackTrace();
    }

    protected Throwable(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        if (writableStackTrace) {
            fillInStackTrace();
        } else {
            stackTrace = NO_STACK_TRACE;
        }
        if (!enableSuppression) {
            suppressedExceptions = null;
        }
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    public Throwable getCause() {
        return (cause == this) ? null : cause;
    }

    public Throwable initCause(Throwable cause) {
        if (this.cause != this) {
            // DO NOT INVOKE Throwable.fromDJVM() here in case someone manages to override it!
            throw new java.lang.IllegalStateException(
                "Can't overwrite cause with " + java.util.Objects.toString(cause, "a null"), DJVM.fromDJVM(this));
        }
        if (cause == this) {
            // DO NOT INVOKE Throwable.fromDJVM() here in case someone manages to override it!
            throw new java.lang.IllegalArgumentException("Self-causation not permitted", DJVM.fromDJVM(this));
        }
        this.cause = cause;
        return this;
    }

    @Override
    @NotNull
    public String toDJVMString() {
        java.lang.String s = getClass().getName();
        String localized = getLocalizedMessage();
        return String.valueOf((localized != null) ? (s + ": " + localized) : s);
    }

    public StackTraceElement[] getStackTrace() {
        return (stackTrace == NO_STACK_TRACE) ? stackTrace : stackTrace.clone();
    }

    public void setStackTrace(@NotNull StackTraceElement[] stackTrace) {
        StackTraceElement[] traceCopy = stackTrace.clone();

        for (int i = 0; i < traceCopy.length; ++i) {
            if (traceCopy[i] == null) {
                throw new java.lang.NullPointerException("stackTrace[" + i + ']');
            }
        }

        this.stackTrace = traceCopy;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Throwable fillInStackTrace() {
        if (stackTrace == null) {
            /*
             * We have been invoked from within this exception's constructor.
             * Work our way up the stack trace until we find this constructor,
             * and then find out who actually invoked it. This is where our
             * sandboxed stack trace will start from.
             *
             * Our stack trace will end at the point where we entered the sandbox.
             */
            final java.lang.StackTraceElement[] elements = new java.lang.Throwable().getStackTrace();
            final java.lang.String exceptionName = getClass().getName();
            int startIdx = 1;
            while (startIdx < elements.length && !isConstructorFor(elements[startIdx], exceptionName)) {
                ++startIdx;
            }
            while (startIdx < elements.length && isConstructorFor(elements[startIdx], exceptionName)) {
                ++startIdx;
            }

            int endIdx = startIdx;
            while (endIdx < elements.length && !TaskTypes.isEntryPoint(elements[endIdx])) {
                ++endIdx;
            }
            stackTrace = (startIdx == elements.length) ? NO_STACK_TRACE : DJVM.copyToDJVM(elements, startIdx, endIdx);
        }
        return this;
    }

    public final Throwable[] getSuppressed() {
        return (suppressedExceptions == null || suppressedExceptions == DEFAULT_SUPPRESSED)
                ? EMPTY_THROWABLE_ARRAY : suppressedExceptions.toArray(EMPTY_THROWABLE_ARRAY);
    }

    public final void addSuppressed(Throwable throwable) {
        if (throwable == this) {
            throw new java.lang.IllegalArgumentException("Self-suppression not permitted");
        } else if (throwable == null) {
            throw new java.lang.NullPointerException("Cannot suppress a null exception.");
        }
        if (suppressedExceptions != null) {
            if (suppressedExceptions == DEFAULT_SUPPRESSED) {
                suppressedExceptions = new ArrayList<>();
            }
            suppressedExceptions.add(throwable);
        }
    }

    private static boolean isConstructorFor(@NotNull java.lang.StackTraceElement elt, java.lang.String className) {
        return elt.getClassName().equals(className) && elt.getMethodName().equals("<init>");
    }

    public void printStackTrace() {}

    @Override
    @NotNull
    protected java.lang.Throwable fromDJVM() {
        return DJVM.fromDJVM(this);
    }
}
