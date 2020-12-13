package com.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int portNumber = 25565;

    public static void main(String[] args) {
//        ExecutorService executor = Executors.newFixedThreadPool(6);

        //server
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("伺服器開啟");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("client connect!");
                new RequestHandler(clientSocket).run();
//                executor.execute(client);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
