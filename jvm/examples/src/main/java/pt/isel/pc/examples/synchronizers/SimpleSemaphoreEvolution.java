package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.Timeouts;

public class SimpleSemaphoreEvolution {

    /*
     * First version
     */
    static class SimpleSemaphoreV1 {

        private int units;
        private final Object monitor = new Object();

        public SimpleSemaphoreV1(int initialUnits) {
            units = initialUnits;
        }

        public void acquire() throws InterruptedException {
            synchronized (monitor) {
                while (!(units > 0)) {
                    monitor.wait();
                }
                units -= 1;
            }
        }

        public void release() {
            synchronized (monitor) {
                units += 1;
                monitor.notifyAll();
            }
        }
    }
    /*
     * - The release operation is not subject to any condition, meaning that it can always be performed and no
     * waiting is required.
     * - The acquire operation is subject to the condition "units > 0". It can only be completed when that condition
     * is true.
     * - There is no assurance that the condition is true after the monitor.wait().
     *   - The wait method has "spurious wakeup" - see https://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#wait().
     *   - Even without spurious wakeups, there is NO state immutability guarantee between the monitor.notifyAll()
     *   and the exit from monitor.wait().
     *     - Other threads coming from the "outside" can acquire the mutual exclusion before threads coming from the "inside"
     *       - "from the outside" - threads that acquired the mutual exclusion due to calling the acquire.
     *       - "from the inside" - threads that acquired the mutual exclusion because they exits the monitor.wait().
     * - This lack of assurance implies the while in the acquire method, i.e., the constant re-evaluation of state
     * each time the monitor.wait returns
     *
     * - The use of notifyAll on the release method can be improved:
     *   - The release method will create conditions for at most one thread to evolve.
     *   - However, the notifyAll will make all threads transition to the ready state, where most will observe a state
     *   that doesn't fulfill the condition and therefore will have to wait again.
     *   - This means unnecessary context switches.
     */

    /*
     * Version without using notifyAll
     */
    static class SimpleSemaphoreV2 {

        private int units;
        private final Object monitor = new Object();

        public SimpleSemaphoreV2(int initialUnits) {
            units = initialUnits;
        }

        public void acquire() throws InterruptedException {
            synchronized (monitor) {
                while (!(units > 0)) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        /*
                         * To ensure a notification is not lost, on a race between a notify and an interruption.
                         * Not required since JSR-133.
                         */
                        if (units > 0) {
                            monitor.notify();
                        }
                    }
                }
                units -= 1;
            }
        }

        public void release() {
            synchronized (monitor) {
                units += 1;
                monitor.notify();
            }
        }
    }

    /*
     * Version with timeouts
     */
    static class SimpleSemaphoreV3 {
        private int units;
        private final Object monitor = new Object();

        public SimpleSemaphoreV3(int initialUnits) {
            units = initialUnits;
        }

        public boolean acquire(long timeout) throws InterruptedException {
            synchronized (monitor) {

                // fast-path
                if (units > 0) {
                    units -= 1;
                    return true;
                }
                if (Timeouts.noWait(timeout)) {
                    return false;
                }
                // wait-path
                long deadline = Timeouts.deadlineFor(timeout);
                long remaining = Timeouts.remainingUntil(deadline);
                while (true) {
                    try {
                        monitor.wait(remaining);
                    } catch (InterruptedException e) {
                        if (units > 0) {
                            monitor.notify();
                        }
                    }
                    // Is condition true?
                    if (units > 0) {
                        units -= 1;
                    }
                    // Compute new remaining time and decide if it should leave due to timeout
                    remaining = Timeouts.remainingUntil(deadline);
                    if (Timeouts.isTimeout(remaining)) {
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

}
