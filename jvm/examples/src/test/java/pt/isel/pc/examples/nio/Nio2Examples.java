package pt.isel.pc.examples.nio;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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

    @Test
    public void reentrancyTest() throws IOException, ExecutionException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AsynchronousSocketChannel socket = AsynchronousSocketChannel.open();
        socket.connect(new InetSocketAddress("httpbin.org", 80))
                .get(); // Blocking


        String requestString = "GET " + "/get" + " HTTP/1.1\r\n"
                + "User-Agent: Me\r\nHost: httpbin.org\r\nConnection: close\r\n"
                + "\r\n";
        byte[] requestBytes = requestString.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
        // request write cycle
        do {
            int sendCount = socket.write(requestBuffer)
                    .get(); // blocking
            //--------------------------------------------------------
            logger.info("Sent {} bytes", sendCount);
        } while (requestBuffer.position() != requestBuffer.limit());

        ByteBuffer copyBuffer = ByteBuffer.allocate(1);
        CompletionHandler<Integer, Void> continuation = new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                if(result == -1) {
                    logger.info("Read completed");
                    latch.countDown();
                    return;
                }
                logger.info("Read {} bytes", result);
                copyBuffer.clear();
                logger.info("Before read");
                socket.read(copyBuffer, null, this);
                logger.info("After read");
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                latch.countDown();
                logger.error("Error occurred", exc);
            }
        };
        logger.info("Before read");
        socket.read(copyBuffer, null, continuation);
        logger.info("After read");

        latch.await();
    }

}
