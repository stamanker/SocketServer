package com.stamanker.socket;

import java.io.IOException;

public class App {

    public static void main(String[] args) throws IOException {
//        int port = 8081;
//        if (args.length == 1) {
//            port = Integer.parseInt(args[0]);
//        }
        System.out.println("Starting...");
//        System.out.println("port " + port);
//        new AcceptFilesSocketServer(port).process();
        new AcceptFilesSocketServer8082(8082).process();
        new HttpSocketServer(80).process();
        Thread thread = Thread.currentThread();
        synchronized (AcceptFilesSocketServer.class) {
            try {
                thread.join();
            } catch (Exception e) {
                System.out.println("interrupted...");
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("hook!");
                thread.interrupt();
            }
        });
    }

}
