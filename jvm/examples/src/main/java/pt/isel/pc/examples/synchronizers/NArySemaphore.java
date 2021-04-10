package pt.isel.pc.examples.synchronizers;

public interface NArySemaphore {

    boolean acquire(int requestedUnits, long timeout) throws InterruptedException;

    void release(int releasedUnits);
}
