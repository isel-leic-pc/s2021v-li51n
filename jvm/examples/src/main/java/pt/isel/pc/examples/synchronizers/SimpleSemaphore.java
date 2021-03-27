package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.Timeouts;

import java.sql.Time;

public class SimpleSemaphore {

    private int units;
    private final Object monitor = new Object();

    public SimpleSemaphore(int initialUnits) {
        units = initialUnits;
    }

    public boolean acquire(long timeout) throws InterruptedException {
        synchronized (monitor) {
            // 1. fast-path
            if (units > 0) {
                units -= 1;
                return true;
            }
            // 2. should wait or complete immediately with a failure
            if (Timeouts.noWait(timeout)) {
                return false;
            }

            // 3. wait-path
            // - compute wait deadline and current remaining
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            while (true) {
                // 4. wait
                try {
                    monitor.wait(remaining);
                } catch (InterruptedException e) {
                    if (units > 0) {
                        monitor.notify();
                    }
                    throw e;
                }
                // 5. is the condition true?
                if (units > 0) {
                    units -= 1;
                    return true;
                }
                // 6. compute new remaining time
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    // 7. if already at or after deadline, complete with a failure
                    return false;
                }
            }
        }
    }

    public void release() {
        synchronized (monitor) {
            units += 1;
            monitor.notify();
        }
    }

}
