package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.NodeLinkedList;
import pt.isel.pc.utils.Timeouts;

public class NAryFifoSemaphore implements NArySemaphore{

    private final Object monitor = new Object();

    private int units;
    // The "completion" conditions depend on the requests queue
    // Any changes to the requests queue may change a completion condition from false to true!
    private final NodeLinkedList<Integer> requests = new NodeLinkedList<>();

    public NAryFifoSemaphore(int initialUnits) {
        units = initialUnits;
    }

    @Override
    public boolean acquire(int unitsToAcquire, long timeout) throws InterruptedException {
        synchronized (monitor) {
            // 1. fast-path
            if (requests.isEmpty() && units >= unitsToAcquire) {
                units -= unitsToAcquire;
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
            NodeLinkedList.Node<Integer> node = requests.enqueue(unitsToAcquire);
            while (true) {
                // 4. wait
                try {
                    monitor.wait(remaining);
                } catch (InterruptedException e) {
                    requests.remove(node);
                    notifyAllIfNeeded();
                    throw e;
                }
                // 5. is the condition true?
                if (requests.isHeadNode(node) && units >= unitsToAcquire) {
                    units -= unitsToAcquire;
                    requests.remove(node);
                    notifyAllIfNeeded();
                    return true;
                }
                // 6. compute new remaining time
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    // 7. if already at or after deadline, complete with a failure
                    requests.remove(node);
                    notifyAllIfNeeded();
                    return false;
                }
            }
        }
    }

    @Override
    public void release(int unitsToRelease) {
        synchronized (monitor) {
            units += unitsToRelease;
            notifyAllIfNeeded();
        }
    }

    private void notifyAllIfNeeded() {
        if(requests.isNotEmpty() && units >= requests.getHeadValue()) {
            monitor.notifyAll();
        }
    }

}
