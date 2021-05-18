package pt.isel.pc.examples.lockfree;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {

    static class Node<T> {
        final T value;
        Node<T> next;
        Node(T value) {
            this.value = value;
        }
    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>(null);

    public void push(T value) {
        Node<T> newNode = new Node<>(value);
        while(true) {
            Node<T> observedHead = head.get();
            newNode.next = observedHead;
            if (head.compareAndSet(observedHead, newNode)) {
                return;
            }
        }
    }

    public Optional<T> pop() {
        while(true) {
            Node<T> observedHead = head.get();
            if (observedHead == null) {
                return Optional.empty();
            }
            if (head.compareAndSet(observedHead, observedHead.next)) {
                return Optional.of(observedHead.value);
            }
        }
    }

}
