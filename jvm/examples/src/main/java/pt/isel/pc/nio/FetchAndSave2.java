package pt.isel.pc.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.pc.utils.Closeables;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

// Asynchronous, continuation-passing-style (CPS)
public class FetchAndSave2 {

    private static final Logger log = LoggerFactory.getLogger(FetchAndSave2.class);

    // The parameters
    private final URL url;
    private final String fileName;
    private final CompletionHandler<Integer, Void> continuation;

    // The local state
    AsynchronousSocketChannel socket;
    AsynchronousFileChannel file;

    ByteBuffer requestBuffer;
    int filePosition;
    ByteBuffer copyBuffer;

    private void handleError(Throwable exc) {
        log.error("Handling error");
        Throwable exception = Closeables.safeClose(exc, socket, file);
        continuation.failed(exception, null);
    }

    private FetchAndSave2(URL url, String fileName, CompletionHandler<Integer, Void> continuation) {
        this.url = url;
        this.fileName = fileName;
        this.continuation = continuation;
    }

    private void connect_0() throws IOException {
        socket = AsynchronousSocketChannel.open();
        file = AsynchronousFileChannel.open(Paths.get(fileName),
                WRITE, CREATE);
        socket.setOption(StandardSocketOptions.SO_SNDBUF, 16);
        socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), null,
                continueOn(ignore -> startWriteRequest_1()));
    }

    private void startWriteRequest_1() {
        String requestString = "GET " + url.getPath() + " HTTP/1.1\r\n"
                + "User-Agent: Me\r\nHost: httpbin.org\r\nConnection: close\r\n"
                + "\r\n";

        byte[] requestBytes = requestString.getBytes(StandardCharsets.US_ASCII);
        requestBuffer = ByteBuffer.wrap(requestBytes);

        socket.write(requestBuffer, null,
                continueOn(this::continueWriteRequest_2));
    }

    private void continueWriteRequest_2(Integer sendCount) {
        log.info("Sent {} bytes", sendCount);
        if (requestBuffer.position() != requestBuffer.limit()) {
            socket.write(requestBuffer, null, continueOn(this::continueWriteRequest_2));
        } else {
            filePosition = 0;
            copyBuffer = ByteBuffer.allocate(8);
            socket.read(copyBuffer, null, continueOn(this::writeFile_3));
        }
    }

    private void writeFile_3(Integer readCount) {
        log.info("Read {} bytes from socket", readCount);
        if (readCount == -1) {
            // return filePosition
            Throwable maybeThrowable = Closeables.safeClose(null, socket, file);
            if (maybeThrowable != null) {
                continuation.failed(maybeThrowable, null);
            } else {
                continuation.completed(filePosition, null);
            }
        } else {
            copyBuffer.flip();
            file.write(copyBuffer, filePosition, null, continueOn(this::readSocket_4));
        }
    }

    private void readSocket_4(Integer writeCount) {
        log.info("Wrote {} bytes into file", writeCount);
        filePosition += writeCount;
        copyBuffer.clear();
        socket.read(copyBuffer, null, continueOn(this::writeFile_3));
    }


    public static void run(URL url, String fileName, CompletionHandler<Integer, Void> continuation) {
        FetchAndSave2 fetchAndSave2 = new FetchAndSave2(url, fileName, continuation);
        try {
            fetchAndSave2.connect_0();
        } catch (Throwable exc) {
            fetchAndSave2.handleError(exc);
        }
    }

    private <T> CompletionHandler<T, Void> continueOn(Consumer<T> onSuccess) {
        return new CompletionHandler<>() {

            @Override
            public void completed(T result, Void attachment) {
                try {
                    log.info("CompletionHandler#completed called");
                    onSuccess.accept(result);
                } catch (Throwable exc) {
                    handleError(exc);
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                log.error("CompletionHandler#failed called");
                handleError(exc);
            }
        };
    }
}

