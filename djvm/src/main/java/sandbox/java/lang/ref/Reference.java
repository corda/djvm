package sandbox.java.lang.ref;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Reference<T> extends sandbox.java.lang.Object {

    private T referent;

    Reference(T referent) {
        this(referent, null);
    }

    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
    }

    public T get() {
        return referent;
    }

    public void clear() {
        referent = null;
    }

    public boolean isEnqueued() {
        return false;
    }

    public boolean enqueue() {
        return false;
    }
}
