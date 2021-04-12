package pt.isel.pc.examples.basics;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.pc.examples.synchronizers.NAryFifoSemaphore;
import pt.isel.pc.examples.synchronizers.NAryFifoSemaphoreWithKernelStyle;
import pt.isel.pc.examples.synchronizers.NAryFifoSemaphoreWithKernelStyleAndSpecificNotification;
import pt.isel.pc.examples.synchronizers.NArySemaphore;
import pt.isel.pc.examples.utils.TestHelper;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

public class NArySemaphoreTests {

    private static final int N_OF_THREADS = 100;
    private static final Duration TEST_DURATION = Duration.ofSeconds(20);
    private static final Logger log = LoggerFactory.getLogger(NArySemaphoreTests.class);

    private void does_not_exceed_max_units(NArySemaphore semaphore, int units) throws InterruptedException {
        AtomicInteger acquiredUnits = new AtomicInteger(units);
        TestHelper helper = new TestHelper(TEST_DURATION);

        helper.createAndStartMultiple(N_OF_THREADS, (ignore, isDone) -> {
            while (!isDone.get()) {
                int requestedUnits = ThreadLocalRandom.current().nextInt(units) + 1;
                semaphore.acquire(requestedUnits, Long.MAX_VALUE);
                try {
                    int current = acquiredUnits.addAndGet(-requestedUnits);
                    assertTrue("acquiredUnits must not be negative", current >= 0);
                    Thread.yield();
                } finally {
                    acquiredUnits.addAndGet(requestedUnits);
                    semaphore.release(requestedUnits);
                }
            }
        });

        helper.join();
    }

    @Test
    public void NAryFifoSemaphore_simple_test() throws InterruptedException {
        int units = N_OF_THREADS / 3;
        does_not_exceed_max_units(new NAryFifoSemaphore(units), units);
    }

    @Test
    public void NAryFifoSemaphoreWithKernelStyle_simple_test() throws InterruptedException {
        int units = N_OF_THREADS / 3;
        does_not_exceed_max_units(new NAryFifoSemaphoreWithKernelStyle(units), units);
    }

    @Test
    public void NAryFifoSemaphoreWithKernelStyleAndSpecificNotification_simple_test() throws InterruptedException {
        int units = N_OF_THREADS / 3;
        does_not_exceed_max_units(new NAryFifoSemaphoreWithKernelStyleAndSpecificNotification(units), units);
    }

    private void order_test(NArySemaphore sem) throws InterruptedException {
        final ConcurrentLinkedQueue<Long> acquiredUnits = new ConcurrentLinkedQueue<>();
        final CountDownLatch cdl = new CountDownLatch(N_OF_THREADS);
        TestHelper helper = new TestHelper(TEST_DURATION);
        helper.createAndStartMultiple(N_OF_THREADS, (ignore, isDone) -> {
            long totalAcquires = 0;
            cdl.countDown();
            cdl.await();
            while (!isDone.get() && totalAcquires < Long.MAX_VALUE) {
                sem.acquire(1, Long.MAX_VALUE);
                totalAcquires += 1;
                Thread.yield();
                sem.release(1);
            }
            acquiredUnits.add(totalAcquires);
        });
        helper.join();
        long max = Collections.max(acquiredUnits);
        long min = Collections.min(acquiredUnits);
        log.info("min acquired units = {}, max acquired units = {}, diff = {}",
                min, max, max - min);
        assertTrue("acquired units (max-min) must not exceed 5% of min",
                max - min < 0.05 * min);

    }

    @Test
    public void NAryFifoSemaphore_order_test() throws InterruptedException {
        order_test(new NAryFifoSemaphore(1));
    }

    @Test
    public void NAryFifoSemaphoreWithKernelStyle_order_test() throws InterruptedException {
        order_test(new NAryFifoSemaphoreWithKernelStyle(1));
    }

    @Test
    public void NAryFifoSemaphoreWithKernelStyleAndSpecificNotification_order_test() throws InterruptedException {
        order_test(new NAryFifoSemaphoreWithKernelStyleAndSpecificNotification(1));
    }

    private void order_test2(Function<Integer, NArySemaphore> create) throws InterruptedException {
        NArySemaphore semaphore = create.apply(4);
        TestHelper helper = new TestHelper(TEST_DURATION);
        Duration waitDuration = Duration.ofMillis(100);
        ConcurrentHashMap<Integer, Long> counts = new ConcurrentHashMap<>();

        helper.createAndStartMultiple(2, (index, isDone) -> {
            long count = 0;
            while (!isDone.get()) {
                semaphore.acquire(2, Long.MAX_VALUE);
                count += 1;
                Thread.sleep(waitDuration.toMillis());
                semaphore.release(2);
            }
            counts.put(index, count);
        });
        helper.createAndStartMultiple(1, (index, isDone) -> {
            long count = 0;
            while (!isDone.get()) {
                semaphore.acquire(3, Long.MAX_VALUE);
                count += 1;
                Thread.sleep(waitDuration.toMillis());
                semaphore.release(3);
            }
            counts.put(2, count);
        });
        helper.join();
        List<Long> results = Arrays.asList(counts.get(0), counts.get(1), counts.get(2));
        long targetCount = TEST_DURATION.dividedBy(waitDuration) / 2;
        log.info("targetCount = {}, counts = {}", targetCount, results);
        results.forEach(count -> {
            assertTrue(Math.abs(targetCount - count) <= 5);
        });
        long min = Collections.min(results);
        long max = Collections.max(results);
        assertTrue(max <= min + 2);
    }

    @Test
    public void NAryFifoSemaphore_order_test2() throws InterruptedException {
        order_test2(NAryFifoSemaphore::new);
    }

    @Test
    public void NAryFifoSemaphoreWithKernelStyle_order_test2() throws InterruptedException {
        order_test2(NAryFifoSemaphoreWithKernelStyle::new);
    }

    @Test
    public void NAryFifoSemaphoreWithKernelStyleAndSpecificNotification_order_test2() throws InterruptedException {
        order_test2(NAryFifoSemaphoreWithKernelStyleAndSpecificNotification::new);
    }

}
