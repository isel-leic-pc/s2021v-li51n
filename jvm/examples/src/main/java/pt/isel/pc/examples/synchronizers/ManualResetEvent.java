package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.Timeouts;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ManualResetEvent {

    private class Request {
        public boolean isDone;

        public Request(boolean done) {
            this.isDone = done;
        }
    }

    private boolean state;
    private final Lock monitor = new ReentrantLock();
    private final Condition condition = monitor.newCondition();
    private Request request;
    private int awaiters = 0;

    public ManualResetEvent(boolean initialState) {
        this.state = initialState;
        this.request = new Request(initialState);
    }

    public void set() {
        monitor.lock();
        try {
            this.state = true;
            this.request.isDone = true;
            if (awaiters != 0) {
                this.condition.signalAll();
                awaiters = 0;
            }
        } finally {
            monitor.unlock();
        }
    }

    public void reset() {
        monitor.lock();
        try {
            this.state = false;
            if (this.request.isDone) {
                this.request = new Request(false);
            }
        } finally {
            monitor.unlock();
        }
    }

    public boolean awaitSet(long timeout) throws InterruptedException {
        monitor.lock();
        try {
            // fast-path
            if (state) {
                return true;
            }
            if (Timeouts.noWait(timeout)) {
                return false;
            }
            // wait-path
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            Request observedRequest = request;
            awaiters += 1;
            while (true) {
                try {
                    condition.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (observedRequest.isDone) {
                        Thread.currentThread().interrupt();
                        return true;
                    }
                    awaiters -= 1;
                    return false;
                }
                if (observedRequest.isDone) {
                    return true;
                }
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    awaiters -= 1;
                    return false;
                }
            }

        } finally {
            monitor.unlock();
        }
    }


}
