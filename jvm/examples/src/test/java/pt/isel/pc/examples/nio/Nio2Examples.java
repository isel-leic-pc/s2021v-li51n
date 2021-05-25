package pt.isel.pc.examples.nio;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class Nio2Examples {

    private static final Logger logger = LoggerFactory.getLogger(Nio2Examples.class);

    @Test
    public void first() throws IOException, InterruptedException {

        int nOfOperations = 20;
        CountDownLatch latch = new CountDownLatch(nOfOperations);

        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(Executors.newSingleThreadExecutor());
        for(int i = 0 ; i<nOfOperations ; ++i) {
            int ix = i;
            AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(group);
            logger.info("about to start the connect operation...");
            socketChannel.connect(new InetSocketAddress("httpbin.org", 80), null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attachment) {
                    logger.info("...connect operation completed with success");
                    try {
                        if(ix == 0) {
                            Thread.sleep(2000);
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    latch.countDown();
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    logger.info("...connect operation completed with failure {}", exc.getMessage());
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

}
