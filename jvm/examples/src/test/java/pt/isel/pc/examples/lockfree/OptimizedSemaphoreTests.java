package pt.isel.pc.examples.lockfree;

import org.junit.Test;
import pt.isel.pc.examples.utils.TestHelper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class OptimizedSemaphoreTests {

    @Test
    public void todo() throws InterruptedException {
        int N_OF_THREADS = 16;
        int INITIAL_UNITS = 6;
        TestHelper testHelper = new TestHelper(Duration.ofSeconds(5));
        AtomicInteger counter = new AtomicInteger(INITIAL_UNITS);
        OptimizedSemaphore sem = new OptimizedSemaphore(INITIAL_UNITS);

        testHelper.createAndStartMultiple(N_OF_THREADS, (index, isDone) -> {
            while(!isDone.get()) {
                sem.acquire(Long.MAX_VALUE);
                int observedCounter = counter.addAndGet(-1);
                assertTrue(observedCounter >= 0);
                counter.addAndGet(1);
                sem.release();
            }
        });

        testHelper.join();

    }

}
