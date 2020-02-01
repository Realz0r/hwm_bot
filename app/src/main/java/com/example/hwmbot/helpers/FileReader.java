package com.example.hwmbot.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

public class FileReader {
    public static String read(InputStream inputStream) {
        String file = null;

        try {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
                file = writer.toString();
            }
        } catch (Exception ex) {
            Log.d("Error read", ex.getMessage());
            ex.printStackTrace();
        }

        return file;
    }

    public static Bitmap getImage(String urlPath) {
        Bitmap bitmap = null;
        try {
            InputStream in = new java.net.URL(urlPath).openStream();
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Exception ex) {
            Log.e("Error loaded image", ex.getMessage());
            ex.printStackTrace();
        }

        return bitmap;
    }
}
