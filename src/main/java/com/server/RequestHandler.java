package com.server;


import java.io.*;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler implements Runnable {
    private static final String TAG = "[Client]";
    private static final String LINE_END = "\r\n";
    private static final String HANDSHAKE = "Upgrade: websocket" + LINE_END +
            "Connection: Upgrade" + LINE_END +
            "Sec-WebSocket-Accept: {sha1}" + LINE_END + LINE_END;

    private Socket socket;

    private OutputStream writer;
    private InputStream in;

    boolean running = true;

    RequestHandler(Socket socket) {
        this.socket = socket;
    }


    @Override
    public void run() {
        try {
            in = socket.getInputStream();
            writer = socket.getOutputStream();

            Scanner s = new Scanner(in, "UTF-8");
            String data = s.useDelimiter("\\r\\n\\r\\n").next();
            Matcher get = Pattern.compile("^GET").matcher(data);

            //web用的handshake
            if (get.find()) {
                //取得handshake key
                Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                match.find();
                //make handshake
                byte[] response = ("" +
                        "HTTP/1.1 101 Switching Protocols" + LINE_END +
                        "Connection: Upgrade" + LINE_END +
                        "Upgrade: websocket" + LINE_END +
                        "Sec-WebSocket-Accept: " +
                        //加密handshake key
                        Base64.getEncoder().encodeToString(encryptSHA1(match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")) +
                        LINE_END + LINE_END).getBytes("UTF-8");
                //write handshake
                writer.write(response, 0, response.length);
            }
            System.out.println(TAG + "handshake done");

            while (running) {
                while (in.available() > 0) {
                    //decode Data
                    byte[] inData = new byte[in.available()];
                    in.read(inData);

                    int opcode = ((char) inData[0]) & 0x0f;
                    //如果連接關閉
                    if (opcode == Opcode.connectionClose) {
                        running = false;
                        continue;
                    }

                    //資料大小
                    int packetLength = ((char) inData[1]) & 0x7f;

                    //read mask byte
                    byte[] readMask = new byte[4];
                    readMask[0] = inData[2];
                    readMask[1] = inData[3];
                    readMask[2] = inData[4];
                    readMask[3] = inData[5];

                    //payload data
                    byte[] payload = new byte[packetLength];
                    //unmasking
                    for (int i = 0; i < packetLength; i++) {
                        payload[i] = (byte) ((int) inData[6 + i] ^ (int) readMask[i % 4]);
                    }

                    System.out.println(TAG + "receive: " + new String(payload));
                    sendData(("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").getBytes(), writer);

                }

//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

            }
        } catch (IOException e) {
            System.out.println(TAG + e.getMessage());
        }

        try {
            socket.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        System.out.println("Client close");
    }

    public static void sendData(byte[] payload, OutputStream writer) {
        System.out.println(payload.length);

        //write response
        byte[] frameHead = new byte[2];
        int fin = 1, opcode = 1, mask = 0;

        frameHead[0] = (byte) ((fin << 7) + opcode);
        frameHead[1] = (byte) ((mask << 7) + payload.length);
        System.out.println(printByte(frameHead));

        byte[] response = new byte[frameHead.length + payload.length];
        System.arraycopy(frameHead, 0, response, 0, frameHead.length);
        System.arraycopy(payload, 0, response, frameHead.length, payload.length);

        try {
            writer.write(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] encryptSHA1(String inputString) {
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(inputString.getBytes("UTF-8"));
            return crypt.digest();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String printByte(byte[] bytes) {
        int i = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (; i < bytes.length - 1; i++) {
            stringBuilder.append(getBits(bytes[i])).append(",");
        }
        stringBuilder.append(getBits(bytes[i])).append("]");
        return stringBuilder.toString();
    }

    private static String getBits(byte byteIn) {
        return String.format("%8s", Integer.toBinaryString(byteIn & 0xFF)).replace(' ', '0');
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3 - 1];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            if (j < bytes.length - 1)
                hexChars[j * 3 + 2] = ',';
        }
        return new String(hexChars);
    }

}