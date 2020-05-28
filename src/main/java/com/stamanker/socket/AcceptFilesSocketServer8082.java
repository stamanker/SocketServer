package com.stamanker.socket;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AcceptFilesSocketServer8082 {

    public static final int DATA_LIMIT = 1024 * 1024;
    public static final int IDENTIFIER_LENGTH = 16;
    private int port;

    public AcceptFilesSocketServer8082(int port) {
        this.port = port;
    }

    public AcceptFilesSocketServer8082 process() throws IOException {
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
            byte[] identifier = new byte[IDENTIFIER_LENGTH];
            System.arraycopy(result, 0, identifier, 0, IDENTIFIER_LENGTH);
            byte[] resultWithoutId = new byte[result.length-IDENTIFIER_LENGTH];
            System.arraycopy(resultWithoutId, 0, result, 16, result.length-IDENTIFIER_LENGTH);
            String identifierDir = identifierToHexString(identifier).toString();
            createIdentifierDir(identifierDir);
            FileUtils.writeToFile(identifierDir + "/" + fileName, resultWithoutId);
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

    private StringBuilder identifierToHexString(byte[] identifier) {
        StringBuilder idStr = new StringBuilder();
        for (byte b : identifier) {
            idStr.append(String.format("%x", b));
        }
        System.out.println("idStr = " + idStr);
        return idStr;
    }

    private void createIdentifierDir(String idStr) {
        try {
            Files.createDirectory(Paths.get(idStr));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}