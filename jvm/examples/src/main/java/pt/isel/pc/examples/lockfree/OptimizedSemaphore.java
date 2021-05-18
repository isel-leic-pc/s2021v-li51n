package pt.isel.pc.examples.lockfree;

import pt.isel.pc.utils.Timeouts;

import java.util.concurrent.atomic.AtomicInteger;

public class OptimizedSemaphore {

    private final AtomicInteger units;
    private volatile int waiters = 0;
    private final Object lock = new Object();

    public OptimizedSemaphore(int initialUnits) {
        units = new AtomicInteger(initialUnits);
    }

    public boolean acquire(long timeout) throws InterruptedException {

        if(tryDecrement()) {
            return true;
        }
        if(Timeouts.noWait(timeout)) {
            return false;
        }
        synchronized (lock) {
            waiters += 1;
            if(tryDecrement()) {
                waiters -= 1;
                return true;
            }
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            while(true) {
                try {
                    lock.wait(remaining);
                }catch (InterruptedException e) {
                    lock.notify();
                    waiters -= 1;
                    throw e;
                }
                if(tryDecrement()) {
                    waiters -= 1;
                    return true;
                }
                remaining = Timeouts.remainingUntil(deadline);
                if(Timeouts.isTimeout(remaining)) {
                    waiters -= 1;
                    return false;
                }
            }
        }
    }

    public void release() {
        units.incrementAndGet();
        if(waiters == 0) {
            return;
        }
        synchronized (lock) {
            if(waiters != 0) {
                lock.notify();
            }
        }
    }

    private boolean tryDecrement() {
        while(true) {
            int observedUnits = units.get();
            if(observedUnits == 0) {
                return false;
            }
            if(units.compareAndSet(observedUnits, observedUnits - 1)) {
                return true;
            }
        }
    }

}
