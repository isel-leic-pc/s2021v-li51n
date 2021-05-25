package pt.isel.pc.nio;

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

public class FetchAndSave3 {

    private final URL url;
    private final CompletionHandler<Integer, Void> continuation;
    private final AsynchronousSocketChannel socket;
    private final AsynchronousFileChannel file;

    FetchAndSave3(AsynchronousSocketChannel socket,
                  AsynchronousFileChannel file,
                  URL url,
                  CompletionHandler<Integer, Void> continuation) {
        this.socket = socket;
        this.file = file;
        this.url = url;
        this.continuation = continuation;
    }

    public static void run(URL url, String fileName, CompletionHandler<Integer, Void> completionHandler) {
        AsynchronousSocketChannel socket = null;
        AsynchronousFileChannel file = null;
        try {
            socket = AsynchronousSocketChannel.open();
            file = AsynchronousFileChannel.open(Paths.get(fileName), WRITE, CREATE);
            FetchAndSave3 fas = new FetchAndSave3(socket, file, url, completionHandler);
            fas.connect(url);
        } catch (IOException ioException) {
            Throwable th = Closeables.safeClose(ioException, socket, file);
            completionHandler.failed(th, null);
        }
    }

    private void handleError(Throwable th) {
        th = Closeables.safeClose(th, socket, file);
        continuation.failed(th, null);
    }

    private void completed(Integer size) {
        Throwable th = Closeables.safeClose(null, socket, file);
        if (th != null) {
            continuation.failed(th, null);
        } else {
            continuation.completed(size, null);
        }
    }

    private void connect(URL url) throws IOException {
        socket.setOption(StandardSocketOptions.SO_SNDBUF, 16);
        socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), null,
                continueOn(this::sendRequest));
    }

    private void sendRequest(Void result) {
        String requestString = "GET " + url.getPath() + " HTTP/1.1\r\n"
                + "User-Agent: Me\r\nHost: httpbin.org\r\nConnection: close\r\n"
                + "\r\n";
        byte[] requestBytes = requestString.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
        BufferWrite.run(requestBuffer, socket,
                continueOn(this::copyResponse));
    }

    private void copyResponse(Integer ignore) {
        ReadWrite.run(socket, file,
                continueOn(this::completed));
    }

    private <R> CompletionHandler<R, Void> continueOn(Consumer<R> completed) {
        return new CompletionHandler<>() {
            @Override
            public void completed(R result, Void attachment) {
                try {
                    completed.accept(result);
                } catch (Throwable exc) {
                    handleError(exc);
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                handleError(exc);
            }
        };
    }
}
