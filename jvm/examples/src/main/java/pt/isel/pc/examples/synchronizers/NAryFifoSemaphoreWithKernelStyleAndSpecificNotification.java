package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.NodeLinkedList;
import pt.isel.pc.utils.Timeouts;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NAryFifoSemaphoreWithKernelStyleAndSpecificNotification {

    private final Lock monitor = new ReentrantLock();

    private static class Request {
        final int requestedUnits;
        final Condition condition;
        boolean isDone = false;

        Request(int requestedUnits, Condition condition) {

            this.requestedUnits = requestedUnits;
            this.condition = condition;
        }
    }

    private int units;
    private final NodeLinkedList<Request> requests = new NodeLinkedList<>();

    public NAryFifoSemaphoreWithKernelStyleAndSpecificNotification(int initialUnits) {
        units = initialUnits;
    }

    public boolean acquire(int unitsToAcquire, long timeout) throws InterruptedException {
        monitor.lock();
        try {
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
            Condition myCondition = monitor.newCondition();
            NodeLinkedList.Node<Request> myRequest = requests.enqueue(new Request(
                    unitsToAcquire,
                    myCondition));
            while (true) {
                // 4. wait
                try {
                    myCondition.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // Is the request done?
                    if (myRequest.value.isDone) {
                        // If so, it needs to return with success
                        Thread.currentThread().interrupt();
                        return true;
                    }
                    // Otherwise, give-up by removing the node
                    requests.remove(myRequest);
                    // Which changes the state, allowing for other threads to complete.
                    completeAllPossible();
                    throw e;
                }
                // 5. is the request done?
                if (myRequest.value.isDone) {
                    return true;
                }
                // 6. compute new remaining time
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    // 7. if already at or after deadline, complete with a failure
                    // give-up by removing the node
                    requests.remove(myRequest);
                    // Which changes the state, allowing for other threads to complete.
                    completeAllPossible();
                    return false;
                }
            }
        } finally {
            monitor.unlock();
        }
    }

    public void release(int unitsToRelease) {
        monitor.lock();
        try {
            units += unitsToRelease;
            completeAllPossible();
        } finally {
            monitor.unlock();
        }
    }

    private void completeAllPossible() {
        while (requests.isNotEmpty() && units >= requests.getHeadValue().requestedUnits) {
            NodeLinkedList.Node<Request> headNode = requests.pull();
            headNode.value.isDone = true;
            headNode.value.condition.signal();
            units -= headNode.value.requestedUnits;
        }
    }
}
