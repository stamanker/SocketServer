package com.stamanker.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AcceptFilesSocketServer8082 {

    public static final int DATA_LIMIT = 1024 * 1024;
    public static final int ID_LENGTH = 10;
    private static final int PASSWORD_LENGTH = 6;
    public static final int HEADER_LENGTH = ID_LENGTH + PASSWORD_LENGTH;
    private final int port;
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

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
                try {
                    executorService.submit(() -> processConnection(ch));
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        List<byte[]> result = new LinkedList<>();
        String address = null;
        int totalSize = 0;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1024*16);
        try {
            address = ch.getRemoteAddress().toString().substring(1);
            //ch.write(ByteBuffer.wrap(("Hello, " + address + "\r\n").getBytes()));
            int bytesRead;
            while ((bytesRead = ch.read(byteBuffer).get(1, TimeUnit.MINUTES)) != -1) {
                byteBuffer.flip();
                byte[] bytes = new byte[bytesRead];
                byteBuffer.get(bytes);
                byteBuffer.clear();
                result.add(bytes);
                totalSize += bytes.length;
                if (totalSize > DATA_LIMIT) {
                    throw new IllegalStateException("too much data");
                }
            }
            saveData(result, totalSize);
        } catch (TimeoutException e) {
            ch.write(ByteBuffer.wrap("Timeout\n".getBytes()));
            System.err.println(sessionId + " " + address + " Connection timed out, closing connection");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(ch);
        }

    }

    private void saveData(List<byte[]> dataPortions, int totalSize) {
        //String fileName = DateUtils.getCurrentDateTime() + ".jpg";
        //System.out.println(sessionId + " End of conversation. Result: " + result.length + " bytes, will write to file " + fileName);
        byte[] data = new byte[totalSize];
        int cursor = 0;
        for (byte[] portion : dataPortions) {
            System.arraycopy(portion, 0, data, cursor, portion.length);
            cursor += portion.length;
        }
        // password
        byte[] password = new byte[6];
        System.arraycopy(data, 0, password, 0, PASSWORD_LENGTH);
        auth(password);
        // id
        byte[] identifier = new byte[ID_LENGTH];
        System.arraycopy(data, PASSWORD_LENGTH+1, identifier, 0, ID_LENGTH);
        // ---
        byte[] resultWithoutId = new byte[data.length - HEADER_LENGTH];
        System.arraycopy(data, HEADER_LENGTH, resultWithoutId, 0, data.length - HEADER_LENGTH);
        String identifierDir = identifierToHexString(identifier).toString();
        createIdentifierDir(identifierDir);
        //FileUtils.writeToFile(identifierDir + "/" + "image.jpg", resultWithoutId);
        FileUtils.writeToFile("image.jpg", resultWithoutId);
    }

    private void close(AsynchronousSocketChannel ch) {
        try {
            // Close the connection if we need to
            if (ch.isOpen()) {
                ch.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void auth(byte[] password) {
        if (password[0]!=23) {
            throw new IllegalArgumentException("Bad header: " + password[0]);
        }
        byte prev = -1;
        final int start = 1;
        for (int i = start; i < password.length; i++) {
            if (i != start) {
                if(password[i] != (prev + 1)) {
                    throw new IllegalArgumentException("Bad password");
                }
            }
            prev = password[i];
        }
    }

    private StringBuilder identifierToHexString(byte[] identifier) {
        StringBuilder idStr = new StringBuilder();
        for (byte b : identifier) {
            idStr.append(String.format("%x", b));
        }
        //System.out.println("idStr = " + idStr);
        return idStr;
    }

    private void createIdentifierDir(String idStr) {
        try {
            //System.out.println("idStr = " + idStr);
            Files.createDirectory(Paths.get(idStr));
        } catch (FileAlreadyExistsException ignore) {
            //System.out.println("already exist");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}