package pt.isel.pc.examples.lockfree;

import org.junit.Test;
import pt.isel.pc.examples.utils.TestHelper;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class LockFreeStackTests {

    @Test
    public void test_push_and_pop_do_not_loose_or_create_extra_nodes() throws InterruptedException {
        int N_OF_THREADS = 8;
        LockFreeStack<Integer> stack = new LockFreeStack<>();
        AtomicInteger counter = new AtomicInteger(0);
        TestHelper testHelper = new TestHelper(Duration.ofSeconds(2));

        // Producer threads
        testHelper.createAndStartMultiple(N_OF_THREADS, (index, isDone) -> {
            while (!isDone.get()) {
                stack.push(index);
                stack.push(index);
                stack.push(index);
                stack.push(index);
                counter.addAndGet(4*index);
            }
        });

        // Consumer threads
        testHelper.createAndStartMultiple(N_OF_THREADS, (index, isDone) -> {
            while (!isDone.get()) {
                stack.pop().ifPresent(value -> counter.addAndGet(-value));
            }
        });

        testHelper.join();

        while(true){
            Optional<Integer> value = stack.pop();
            if(value.isEmpty()) {
                break;
            }
            counter.addAndGet(-value.get());
        }

        assertEquals(0, counter.get());
    }
}
