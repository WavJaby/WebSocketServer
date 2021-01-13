package com.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileReader {
    public byte[] data;

    FileReader(String FileName){
        InputStream in = getClass().getResourceAsStream(FileName);
        try {
            data = getBytes(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static byte[] getBytes(InputStream is) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();)
        {
            byte[] buffer = new byte[0xFFFF];
            for (int len; (len = is.read(buffer)) != -1;)
                os.write(buffer, 0, len);
            os.flush();
            return os.toByteArray();
        }
    }
}
