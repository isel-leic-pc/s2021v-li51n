package pt.isel.pc.sketchboard;

import java.util.function.Supplier;

public class Lazy<T> {

    private final Supplier<T> supplier;
    private final Object lock = new Object();
    private T value = null;

    public Lazy(Supplier<T> supplier) {

        this.supplier = supplier;
    }

    // Double-Check Locking
    public T get() {
        if(value == null) {
            synchronized (lock) {
                if (value == null) {
                    value = supplier.get();
                }
                return value;
            }
        }
        return value;
    }
}
