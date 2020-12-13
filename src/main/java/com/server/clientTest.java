package com.server;

import java.io.*;
import java.net.Socket;

public class clientTest {
    static ObjectOutputStream out;
    static BufferedReader in;

    public static void main(String[] args) {
        String hostName = "localhost";
        int portNumber = 25565;

        try {
            //server
            Socket echoSocket = new Socket(hostName, portNumber);
            out = new ObjectOutputStream(echoSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));

            new thread().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class thread extends Thread {
        @Override
        public void run() {
            super.run();

            try {
                thread.sleep(100);
                System.out.println(in.readLine());
                out.writeObject("hello");
                thread.sleep(1000);

            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
