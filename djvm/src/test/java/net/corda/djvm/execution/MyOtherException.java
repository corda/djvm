package net.corda.djvm.execution;

@SuppressWarnings("WeakerAccess")
public final class MyOtherException extends MyBaseException {
    public MyOtherException(String message) {
        super(message);
    }
}
