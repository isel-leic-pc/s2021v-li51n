package pt.isel.pc.examples.synchronizers;

import pt.isel.pc.utils.NodeLinkedList;

import java.util.Optional;

public class SimpleThreadPool {

    private final int maxPoolSize;
    private int nOfThreads = 0;
    private final NodeLinkedList<Runnable> runnables = new NodeLinkedList<>();
    private final Object monitor = new Object();

    public SimpleThreadPool(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public void execute(Runnable runnable) {
        synchronized (monitor) {
            if (nOfThreads == maxPoolSize) {
                runnables.enqueue(runnable);
                return;
            }
            Thread th = new Thread(() -> threadMethod(runnable));
            th.start();
            nOfThreads += 1;
        }
    }

    private Optional<Runnable> getRunnable() {
        synchronized (monitor) {
            if (runnables.isNotEmpty()) {
                return Optional.of(runnables.pull().value);
            }
            nOfThreads -= 1;
            return Optional.empty();
        }
    }

    private void threadMethod(Runnable runnable) {
        Runnable currentRunnable = runnable;
        while (true) {
            currentRunnable.run();
            Optional<Runnable> maybeRunnable = getRunnable();
            if (maybeRunnable.isEmpty()) {
                return;
            }
            currentRunnable = maybeRunnable.get();
        }
    }
}
