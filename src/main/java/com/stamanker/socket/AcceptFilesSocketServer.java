package com.stamanker.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AcceptFilesSocketServer {

    public static final int DATA_LIMIT = 1024 * 1024;
    private int port;

    public AcceptFilesSocketServer(int port) {
        this.port = port;
    }

    public AcceptFilesSocketServer process() throws IOException {
        AsynchronousServerSocketChannel open = openConnection();
        System.out.println("open port " + port);
        final AsynchronousServerSocketChannel listener = open.bind(new InetSocketAddress(port));
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
        byte[] result = null;
        String address = null;
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            address = ch.getRemoteAddress().toString().substring(1);
            System.out.println("Session with " + address);
            ch.write(ByteBuffer.wrap(("Hello, " + address +"\r\n").getBytes()));
            int bytesRead;
            while ((bytesRead = ch.read(byteBuffer).get(1, TimeUnit.MINUTES)) != -1) {
                byteBuffer.flip();
                byte[] bytes = new byte[bytesRead];
                byteBuffer.get(bytes);
                byteBuffer.clear();
                byteArrayOutputStream.write(bytes);
                if(byteArrayOutputStream.size()> DATA_LIMIT) {
                    throw new IllegalStateException("too much data");
                }
            }
            result = byteArrayOutputStream.toByteArray();
        } catch (TimeoutException e) {
            ch.write(ByteBuffer.wrap("Timeout\n".getBytes()));
            System.out.println(sessionId + " " + address + " Connection timed out, closing connection");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(sessionId + " End of conversation");
        if (result == null) {
            System.out.println(sessionId + " Got no data");
        } else {
            String fileName = DateUtils.getCurrentDateTime() + ".jpg";
            System.out.println(sessionId + " Result got: " + result.length + " bytes, will write data to file " + fileName);
            FileUtils.writeToFile(fileName, result);
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

}