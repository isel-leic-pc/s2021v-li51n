package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.NodeLinkedList;
import pt.isel.pc.utils.Timeouts;

public class NAryFifoSemaphoreWithKernelStyle {

    private final Object monitor = new Object();

    private static class Request {
        final int requestedUnits;
        boolean isDone = false;

        Request(int requestedUnits) {
            this.requestedUnits = requestedUnits;
        }
    }

    private int units;
    private final NodeLinkedList<Request> requests = new NodeLinkedList<>();

    public NAryFifoSemaphoreWithKernelStyle(int initialUnits) {
        units = initialUnits;
    }

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
            NodeLinkedList.Node<Request> node = requests.enqueue(new Request(unitsToAcquire));
            while (true) {
                // 4. wait
                try {
                    monitor.wait(remaining);
                } catch (InterruptedException e) {
                    // Is the request done?
                    if(node.value.isDone) {
                        // If so, it needs to return with success
                        Thread.currentThread().interrupt();
                        return true;
                    }
                    // Otherwise, give-up by removing the node
                    requests.remove(node);
                    // Which changes the state, allowing for other threads to complete.
                    completeAllPossible();
                    throw e;
                }
                // 5. is the request done?
                if (node.value.isDone) {
                    return true;
                }
                // 6. compute new remaining time
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    // 7. if already at or after deadline, complete with a failure
                    // give-up by removing the node
                    requests.remove(node);
                    // Which changes the state, allowing for other threads to complete.
                    completeAllPossible();
                    return false;
                }
            }
        }
    }

    public void release(int unitsToRelease) {
        synchronized (monitor) {
            units += unitsToRelease;
            completeAllPossible();
        }
    }

    private void completeAllPossible() {
        if (requests.isNotEmpty() && units >= requests.getHeadValue().requestedUnits) {
            do {
                NodeLinkedList.Node<Request> headNode = requests.pull();
                headNode.value.isDone = true;
                units -= headNode.value.requestedUnits;
            } while (requests.isNotEmpty() && units >= requests.getHeadValue().requestedUnits);
            monitor.notifyAll();
        }

    }

}
