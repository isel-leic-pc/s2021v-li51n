package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.NodeLinkedList;
import pt.isel.pc.utils.Timeouts;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingMessageQueue<M> {

    static class EnqueueRequest<M> {
        final M message;
        boolean isDone;
        Condition condition;
        public EnqueueRequest(M message, Condition condition) {
            this.message = message;
            this.condition = condition;
        }
    }

    static class DequeueRequest<M> {
        M message;
        boolean isDone;
        Condition condition;
        public DequeueRequest(Condition condition) {
            this.condition = condition;
        }
    }

    private final NodeLinkedList<EnqueueRequest<M>> enqueueRequests = new NodeLinkedList<>();
    private final NodeLinkedList<DequeueRequest<M>> dequeueRequests = new NodeLinkedList<>();
    private final Lock lock = new ReentrantLock();

    public boolean enqueue(M message, long timeout) throws InterruptedException {
        lock.lock();
        try{
            // fast-path
            if(dequeueRequests.isNotEmpty()) {
                DequeueRequest<M> dequeueRequest = dequeueRequests.pull().value;
                dequeueRequest.message = message;
                dequeueRequest.isDone = true;
                dequeueRequest.condition.signal();
                return true;
            }
            if(Timeouts.noWait(timeout)){
                return false;
            }
            // wait-path
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            NodeLinkedList.Node<EnqueueRequest<M>> node = enqueueRequests.enqueue(
                    new EnqueueRequest<>(message, lock.newCondition()));
            while(true){
                try {
                    node.value.condition.await(remaining, TimeUnit.MILLISECONDS);
                }catch(InterruptedException e) {
                    if(node.value.isDone) {
                        Thread.currentThread().interrupt();
                        return true;
                    }
                    enqueueRequests.remove(node);
                    throw e;
                }
                if(node.value.isDone) {
                    return true;
                }
                remaining = Timeouts.remainingUntil(deadline);
                if(Timeouts.isTimeout(remaining)) {
                    enqueueRequests.remove(node);
                    return false;
                }
            }
        }finally {
            lock.unlock();
        }
    }

    public Optional<M> dequeue(long timeout) throws InterruptedException {
        lock.lock();
        try{
            // fast-path
            if(enqueueRequests.isNotEmpty()) {
                EnqueueRequest<M> enqueueRequest = enqueueRequests.pull().value;
                enqueueRequest.isDone = true;
                enqueueRequest.condition.signal();
                return Optional.of(enqueueRequest.message);
            }
            if(Timeouts.noWait(timeout)){
                return Optional.empty();
            }
            // wait-path
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            NodeLinkedList.Node<DequeueRequest<M>> node = dequeueRequests.enqueue(
                    new DequeueRequest<>(lock.newCondition()));
            while(true){
                try {
                    node.value.condition.await(remaining, TimeUnit.MILLISECONDS);
                }catch(InterruptedException e) {
                    if(node.value.isDone) {
                        Thread.currentThread().interrupt();
                        return Optional.of(node.value.message);
                    }
                    dequeueRequests.remove(node);
                    throw e;
                }
                if(node.value.isDone) {
                    return Optional.of(node.value.message);
                }
                remaining = Timeouts.remainingUntil(deadline);
                if(Timeouts.isTimeout(remaining)) {
                    dequeueRequests.remove(node);
                    return Optional.empty();
                }
            }
        }finally {
            lock.unlock();
        }
    }
}
