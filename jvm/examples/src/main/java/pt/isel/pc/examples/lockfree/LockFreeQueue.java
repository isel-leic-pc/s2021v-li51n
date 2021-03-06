package pt.isel.pc.examples.lockfree;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeQueue<T> {

    private static class Node<T> {
        final T value;
        final AtomicReference<Node<T>> next = new AtomicReference<>();
        Node(T value) {
            this.value = value;
        }
    }

    private final AtomicReference<Node<T>> head;
    private final AtomicReference<Node<T>> tail;

    public LockFreeQueue() {
        Node<T> dummy = new Node<T>(null);
        head = new AtomicReference<>(dummy);
        tail = new AtomicReference<>(dummy);
    }

    public void enqueue(T value) {
        Node<T> node = new Node<>(value);

        while(true) {
            Node<T> observedTail = tail.get();
            Node<T> observedNext = observedTail.next.get();
            if (observedNext == null) {
                if (observedTail.next.compareAndSet(null, node)) {
                    tail.compareAndSet(observedTail, node);
                    return;
                }
            } else {
                tail.compareAndSet(observedTail, observedNext);
            }
        }
    }

}
