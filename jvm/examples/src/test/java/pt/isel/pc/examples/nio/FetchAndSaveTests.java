package pt.isel.pc.examples.nio;

import org.junit.Test;
import pt.isel.pc.nio.FetchAndSave2;

import java.net.URL;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class FetchAndSaveTests {

    @Test
    public void testFetchAndSave2() throws Throwable {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> maybeThrowable = new AtomicReference<>();
        FetchAndSave2.run(new URL("http://httpbin.org:80/get"), "build/resp2.txt",
                new CompletionHandler<>() {
                    @Override
                    public void completed(Integer result, Void attachment) {
                        done.countDown();
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        maybeThrowable.set(exc);
                        done.countDown();
                    }
                });
        done.await();
        Throwable observedThrowable = maybeThrowable.get();
        if (observedThrowable != null) {
            throw observedThrowable;
        }
    }
}
