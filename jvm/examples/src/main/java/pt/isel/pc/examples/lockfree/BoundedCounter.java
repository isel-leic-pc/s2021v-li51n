package pt.isel.pc.examples.lockfree;

import java.util.concurrent.atomic.AtomicInteger;

public class BoundedCounter {

    private final int max;
    private final AtomicInteger counter = new AtomicInteger(0);

    public BoundedCounter(int max) {
        this.max = max;
    }

    public boolean inc() {
        while(true) {
            int observedCounter = counter.get();
            if (observedCounter == max) {
                return false;
            }
            if (counter.compareAndSet(observedCounter, observedCounter + 1)) {
                return true;
            }
        }
    }

    public boolean dec() {
        while(true) {
            int observedCounter = counter.get();
            if (observedCounter == 0) {
                return false;
            }
            if (counter.compareAndSet(observedCounter, observedCounter - 1)) {
                return true;
            }
        }
    }

    public int get() {
        return counter.get();
    }

}
