package pt.isel.pc.examples.futures;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class FuturesExampleTests {

    private static final Logger logger = LoggerFactory.getLogger(FuturesExampleTests.class);

    @Test
    public void first() throws URISyntaxException, InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("https://httpbin.org/delay/4"))
                .build();

        logger.info("sendAsync about to be called");
        CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        logger.info("sendAsync returned");
        CompletableFuture<String> future2 = future.thenApply(response -> {
            logger.info("continuation called");
            return response.body().toUpperCase();
        });
        logger.info("thenApply returned");

        future2.whenComplete((result, exc) -> {
            logger.info("future2 completed");
            latch.countDown();
        });

        latch.await();
    }

    private HttpClient client = HttpClient.newHttpClient();

    @Test
    public void second() throws URISyntaxException, InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("https://httpbin.org/delay/4"))
                .build();

        logger.info("sendAsync about to be called");
        CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        logger.info("sendAsync returned");
        CompletableFuture<String> future2 = future.thenCompose(response -> {
            logger.info("continuation called");
            return someAsyncFunction(response.body());
        });
        logger.info("thenApply returned");

        future2.whenComplete((result, exc) -> {
            logger.info("future2 completed");
            latch.countDown();
        });

        latch.await();
    }

    private CompletableFuture<String> someAsyncFunction(String s) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(new URI("https://httpbin.org/delay/4"))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> response.body());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI");
        }
    }

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Test
    public void third() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<String> cf = new CompletableFuture<>();
        scheduler.schedule(() -> {
            logger.info("before complete");
            cf.complete("hello");
            logger.info("after complete");
        }, 2000, TimeUnit.MILLISECONDS);

        CompletableFuture<String> f2 = cf.thenApply(s -> {
            logger.info("continuation 1: {}", s);
            return s.toUpperCase();
        });

        f2.whenComplete((result, exc) -> {
            logger.info("continuation 2: {}", result);
            latch.countDown();
        });

        logger.info("waiting on latch");
        latch.await();

    }

    @Test
    public void fourth() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<String> cf = new CompletableFuture<>();
        scheduler.schedule(() -> {
            logger.info("before complete");
            cf.complete("hello");
            logger.info("after complete");
        }, 2000, TimeUnit.MILLISECONDS);

        CompletableFuture<String> f2 = cf.thenApplyAsync(s -> {
            logger.info("continuation 1: {}", s);
            return s.toUpperCase();
        });

        f2.whenCompleteAsync((result, exc) -> {
            logger.info("continuation 2: {}", result);
            latch.countDown();
        });

        logger.info("waiting on latch");
        latch.await();

    }

}
