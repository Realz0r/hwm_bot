package com.example.hwmbot.helpers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class Request {
    private static String CHARSET = "Cp1251";

    public static String readResult(HttpsURLConnection httpsConnection) throws IOException {
        String inputLine;
        StringBuffer response = new StringBuffer();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream(), CHARSET));

        while ((inputLine = bufferedReader.readLine()) != null) {
            response.append(inputLine);
        }

        bufferedReader.close();

        return response.toString();
    }

    public static HttpsURLConnection GET(String urlPath, Map requestProperty) throws IOException {
        HttpsURLConnection httpsConnection = (HttpsURLConnection) new URL(urlPath).openConnection();
        httpsConnection.setRequestMethod("GET");
        setRequestProperty(httpsConnection, requestProperty);

        //System.out.println(urlPath);

        return httpsConnection;
    }

    public static HttpsURLConnection GET(String urlPath) throws IOException {
        return GET(urlPath, new HashMap());
    }

    public static HttpsURLConnection POST(String urlPath, Map<String, String> postData) throws IOException {
        return POST(urlPath, postData, new HashMap<String, String>());
    }

    public static HttpsURLConnection POST(String urlPath, Map postData, Map requestProperty) throws IOException {
        byte[] data = postDataToString(postData).getBytes(StandardCharsets.UTF_8);
        int dataLength = data.length;
        HttpsURLConnection httpsConnection = (HttpsURLConnection) new URL(urlPath).openConnection();

        httpsConnection.setDoOutput(true);
        httpsConnection.setDoInput(true);
        httpsConnection.setInstanceFollowRedirects(false);
        httpsConnection.setUseCaches(false);
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpsConnection.setRequestProperty("Content-Length", Integer.toString(dataLength));
        setRequestProperty(httpsConnection, requestProperty);

        //System.out.println(urlPath + ":   " + data);

        try (DataOutputStream wr = new DataOutputStream(httpsConnection.getOutputStream())) {
            wr.write(data);
        }

        return httpsConnection;
    }

    private static String postDataToString(Map<String, String> postData) {
        String postDataString = "";

        for (Map.Entry<String, String> item : postData.entrySet()) {
            postDataString += item.getKey() + "=" + item.getValue() + "&";
        }

        return  postDataString;
    }

    private static void setRequestProperty(HttpsURLConnection connection, Map<String, String> requestProperty) {
        for (Map.Entry<String, String> item : requestProperty.entrySet()) {
            connection.setRequestProperty(item.getKey(), item.getValue());
        }
    }
}
