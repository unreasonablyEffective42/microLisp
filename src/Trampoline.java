import java.util.function.Supplier;

public interface Trampoline<T> {
    boolean complete();
    T result();
    Trampoline<T> bounce();

    default T run() {
        Trampoline<T> t = this;
        while (!t.complete()) {
            t = t.bounce();
        }
        return t.result();
    }

    static <T> Trampoline<T> done(T value) {
        return new Done<>(value);
    }

    static <T> Trampoline<T> more(Supplier<Trampoline<T>> thunk) {
        return new More<>(thunk);
    }
}

final class Done<T> implements Trampoline<T> {
    private final T value;
    Done(T v) { this.value = v; }
    public boolean complete() { return true; }
    public T result() { return value; }
    public Trampoline<T> bounce() {
        throw new IllegalStateException("Already complete");
    }
}

final class More<T> implements Trampoline<T> {
    private final Supplier<Trampoline<T>> thunk;
    More(Supplier<Trampoline<T>> thunk) { this.thunk = thunk; }
    public boolean complete() { return false; }
    public T result() {
        throw new IllegalStateException("Not complete yet");
    }
    public Trampoline<T> bounce() { return thunk.get(); }
}
