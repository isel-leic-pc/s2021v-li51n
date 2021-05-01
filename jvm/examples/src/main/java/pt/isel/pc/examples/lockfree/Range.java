package pt.isel.pc.examples.lockfree;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Range {

    private static class Holder {
        private final int low;
        private final int high;

        Holder(int low, int high) {
            this.low = low;
            this.high = high;
        }
    }

    private final AtomicReference<Holder> holder;

    public Range(int low, int high) {
        if(low>high) {
            throw new IllegalArgumentException("TODO");
        }
        this.holder = new AtomicReference<>(new Holder(low, high));
    }

    public void setLow(int newLow) {

    }

    public void setHigh(int newHigh) {

    }

    public void set(int newLow, int newHigh) {

    }

    public boolean isInside(int value) {
        Holder observedHolder = holder.get();
        return value >= observedHolder.low && value <= observedHolder.high;
    }

}
