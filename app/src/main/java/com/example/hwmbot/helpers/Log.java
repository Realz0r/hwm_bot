package com.example.hwmbot.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;



public class Log {
    private static String DEFAULT_PATH = "sdcard/log.txt";

    private static File getFile(String path) {
        File logFile = new File(path);

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return logFile;
    }

    public static void append(String path, String message) {
         try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(getFile(path), true));
            buf.append(new SimpleDateFormat("HH:mm").format(System.currentTimeMillis()) + ":  " + message);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(String path, String body) {
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(getFile(path)));
            buf.append(body);
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String read(String path) throws IOException {
        String file = null;

        try {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            InputStream inputStream = new FileInputStream(getFile(path));
            try {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
                file = writer.toString();
            }
        } catch (Exception ex) {
            android.util.Log.d("Error read", ex.getMessage());
            ex.printStackTrace();
        }

        return file;
    }

    public static void append(String message) {
        append(DEFAULT_PATH, message);
    }
}
