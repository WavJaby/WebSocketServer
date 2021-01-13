package com.server;


import java.io.*;
import java.net.Socket;
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
                    String message = readData(in);

                    System.out.println(TAG + "receive: " + message);

                    if (message.equals("getData")){
                        sendData("{\"C\":{\"detailed\":\"21日14時的中心位置在北緯 9.0 度，東經 112.5 度，以每小時18公里速度，向西南西進行。|中心氣壓|1004|百帕|，|近中心最大風速每秒|15|公尺|，瞬間最大陣風每秒 23 公尺。|\",\"prediction\":\"現況|2020年12月21日14時|中心位置在北緯 9.0 度，東經 112.5 度|過去移動方向 西南西|過去移動時速 9公里|中心氣壓 1004百帕|近中心最大風速每秒 15 公尺|瞬間最大陣風每秒 23 公尺|,|預測|預測 12 小時平均移向移速為|西南西 時速 18 公里|預測 12月22日02時|中心位置在北緯 8.4 度，東經 110.6 度|中心氣壓1004百帕|近中心最大風速每秒 15 公尺|瞬間最大陣風每秒 23 公尺|70%機率半徑 120 公里|預測 12-24 小時平均移向移速為|西 時速 11 公里|預測 12月22日14時|中心位置在北緯 8.5 度，東經 109.4 度|中心氣壓1004百帕|近中心最大風速每秒 15 公尺|瞬間最大陣風每秒 23 公尺|70%機率半徑 170 公里|\",\"info\":\"熱帶性低氣壓|TD26|(原科羅旺颱風)\"},\"E\":{\"detailed\":\"Position 210600Z at 9.0N 112.5E, Movement: WSW 18km/hr. |Minimum pressure 1004 hpa, Max sustained winds near center 15 meter per second|, Gusts 23 meter per second.|\",\"prediction\":\"Analysis|0600UTC 21 December 2020|Center Location 9.0N 112.5E|Movement WSW 9km/hr|Minimum Pressure 1004 hPa|Maximum Wind Speed 15 m/s|Gust 23 m/s|,|Forecast|12 hours valid at:|1800UTC 21 December 2020|Center Position 8.4N 110.6E|Vector to 12 HR Position|WSW 18 km/hr|Minimum Pressure 1004 hPa|Maximum Wind Speed 15 m/s|Gust 23 m/s|Radius of 70% probability circle 120km|24 hours valid at:|0600UTC 22 December 2020|Center Position 8.5N 109.4E|Vector to 24 HR Position|W 11 km/hr|Minimum Pressure 1004 hPa|Maximum Wind Speed 15 m/s|Gust 23 m/s|Radius of 70% probability circle 170km|\",\"info\":\"TROPICAL DEPRESSION|TD26| (KROVANH)\"}}".getBytes(), writer);
                    }


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

    private String readData(InputStream in) throws IOException {
        //decode Data
        byte[] inData = new byte[in.available()];
        in.read(inData);

        int opcode = ((char) inData[0]) & 0x0f;
        //如果連接關閉
        if (opcode == Opcode.connectionClose) {
            running = false;
            return null;
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
        return new String(payload);
    }

    public void sendData(byte[] payloadInput, OutputStream writer) {
        int count = 0;
        int fin, opcode = Opcode.textFrame, mask = 0;
        int payloadLength;

        long time = System.currentTimeMillis();

        do {
            //計算每包長度不可以超過125
            payloadLength = payloadInput.length - count * 125;
            if (payloadLength > 125) {
                fin = 0;
                payloadLength = 125;
            } else {
                fin = 1;
            }

            //如果發送第二包
            if (count > 0)
                opcode = Opcode.continuationFrame;

            //製作包
            byte[] payload = new byte[payloadLength];
            System.arraycopy(payloadInput, count * 125, payload, 0, payload.length);

            //開頭資料
            byte[] frameHead = new byte[2];
            frameHead[0] = (byte) ((fin << 7) + opcode);
            frameHead[1] = (byte) ((mask << 7) + payload.length);

            //合併開頭跟資料
            byte[] response = new byte[frameHead.length + payload.length];
            System.arraycopy(frameHead, 0, response, 0, frameHead.length);
            System.arraycopy(payload, 0, response, frameHead.length, payload.length);

            //sendData
            try {
                writer.write(response);
            } catch (IOException e) {
                e.printStackTrace();
            }

            count++;
        } while (payloadInput.length - count * 125 > 0);

        System.out.println(System.currentTimeMillis() - time);
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
