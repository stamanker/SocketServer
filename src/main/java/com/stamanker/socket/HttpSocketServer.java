package com.stamanker.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpSocketServer {

    public static final int DATA_LIMIT = 4096 * 8;
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String TEXT_HTML = "text/html";
    private int port;

    public HttpSocketServer(int port) {
        this.port = port;
    }

    public HttpSocketServer process() throws IOException {
        final AsynchronousServerSocketChannel connection = openConnection();
        System.out.println("open port " + port);
        final AsynchronousServerSocketChannel listener = connection.bind(new InetSocketAddress(port));
        listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel ch, Void att) {
                listener.accept(null, this);
                processConnection(ch);
            }

            @Override
            public void failed(Throwable exc, Void att) {
                // ...
            }
        });
        return this;
    }

    private AsynchronousServerSocketChannel openConnection() {
        try {
            return AsynchronousServerSocketChannel.open();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void processConnection(AsynchronousSocketChannel ch) {
        String sessionId = UUID.randomUUID().toString();
        String address = null;
        ByteBuffer byteBuffer = ByteBuffer.allocate(DATA_LIMIT);
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            address = ch.getRemoteAddress().toString().substring(1);
            int bytesRead = ch.read(byteBuffer).get(5, TimeUnit.SECONDS);
            byteBuffer.flip();
            byte[] bytes = new byte[bytesRead];
            byteBuffer.get(bytes);
            byteBuffer.clear();
            byteArrayOutputStream.write(bytes);
            if (byteArrayOutputStream.size() > DATA_LIMIT) {
                throw new IllegalStateException("too much data");
            }
            processBytes(ch, bytes);
            //System.out.println(sessionId + " End of conversation");
        } catch (TimeoutException e) {
            ch.write(ByteBuffer.wrap(" Timeout\n".getBytes()));
            System.out.println(sessionId + " " + address + " Connection timed out, closing connection");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // Close the connection if we need to
            if (ch.isOpen()) {
                ch.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void processBytes(AsynchronousSocketChannel ch, byte[] result) throws Exception {
        Response response;
        try {
            response = processRequest(result);
        } catch (Exception e) {
            response = new Response(500, TEXT_HTML, null) {
                @Override
                protected byte[] composeResult() {
                    return "\n".getBytes();
                }
            };
        }
        ch.write(ByteBuffer.wrap(response.getHeaders().getBytes())).get();
        ch.write(ByteBuffer.wrap(response.getResult()));
    }

    private Response processRequest(byte[] result) {
        String[] resultStrs = new String(result, StandardCharsets.UTF_8).split("\n");
        if (resultStrs.length > 0) {
            String[] s = resultStrs[0].split(" ");
            if (s.length > 2) {
                if (!s[0].equalsIgnoreCase("GET")) {
                    throw new RuntimeException("Only GET-requests :)");
                }
                if (s[1].equalsIgnoreCase("/")) {
                    return new Response(200, TEXT_HTML, "OK") {
                        @Override
                        public byte[] composeResult() {
                            return readFile("index.html");
                        }
                    };
                } else if (s[1].startsWith("/image.jpg") || s[1].startsWith("/image.jpeg")) {
                    return new Response(200, IMAGE_JPEG, "OK") {
                        @Override
                        public byte[] composeResult() {
                            return readFile("image.jpg");
                        }
                    };
                } else if (s[1].startsWith("/favicon")) {
                    return new Response(404, "image/jpeg", "no") {
                        @Override
                        public byte[] composeResult() {
                            return "\n".getBytes();
                        }
                    };
                }
            }
        }
        return new Response(500, TEXT_HTML, "ERROR") {
            @Override
            public byte[] composeResult() {
                return "\n".getBytes();
            }
        };
    }

    private byte[] readFile(String fileName) {
        try {
            return Files.readAllBytes(Paths.get(fileName));
        } catch (Exception e) {
            return "error [1]".getBytes();
        }
    }


}
