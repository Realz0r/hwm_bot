package com.example.hwmbot.helpers;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BrowserSession {
    private static String USER_AGENT ="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.62 Safari/537.36";
    private static String USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; Android 9; SM-G950F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.62 Mobile Safari/537.36";
    private static String PROTOCOL = "https://";
    private String path = null;
    private String lastRequestURL = null;
    private Map<String, String> cookies = new HashMap<>();

    public BrowserSession(String host) {
        this.path = PROTOCOL + host + '/';
    }

    public BrowserSession(String host, Map<String, String> cookies) {
        this.path = PROTOCOL + host + '/';
        this.cookies = cookies == null ? this.cookies : cookies;
    }

    public BrowserSession(String host, String cookies) {
        this.path = PROTOCOL + host + '/';
        this.cookies = parseCookies(cookies);
    }

    public RequestInfo GET(String URL, Map requestProperty) throws IOException {
        Map<String, String> requestHeaders = getRequestHeaders();
        requestHeaders.putAll(requestProperty);
        HttpsURLConnection httpsURLConnection = Request.GET(path + URL, requestHeaders);

        Log.append("sdcard/LogRequest.txt", path + URL);

        return processResultConnection(httpsURLConnection);
    }

    public RequestInfo GET(String URL) throws IOException {
        return GET(URL, new HashMap());
    }

    public RequestInfo POST(String URL, Map postData, Map requestProperty) throws IOException {
        Map<String, String> requestHeaders = getRequestHeaders();
        requestHeaders.putAll(requestProperty);
        HttpsURLConnection httpsURLConnection = Request.POST(path + URL, postData, requestHeaders);

        Log.append("sdcard/LogRequest.txt", path + URL);

        return processResultConnection(httpsURLConnection);
    }

    public RequestInfo POST(String URL, Map postData) throws IOException {
        return POST(URL, postData, new HashMap<String, String>());
    }

    public String getCookie() {
        return cookies.entrySet().stream()
                .map(cookie -> cookie.getKey() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
    }

    public void setCookie(String cookie) {
        this.cookies = parseCookies(cookie);
    }

    private Map<String, String> parseCookies(String cookies) {
        Map<String, String> cookieMap = new HashMap<>();

        try {
            String[] listCookie = cookies.split("; ");
            Arrays.stream(listCookie).forEach((cookie) -> {
                if (cookie.indexOf("=") != -1) {
                    String[] cookieParams = cookie.split("=");
                    cookieMap.put(cookieParams[0], cookieParams[1]);
                }
            });
        } catch (Exception ex) {}

        return cookieMap;
    }

    private RequestInfo processResultConnection (HttpsURLConnection httpsURLConnection) throws IOException {
        String responseText = Request.readResult(httpsURLConnection);
        Map<String, List<String>> headersResponse = httpsURLConnection.getHeaderFields();
        List<String> location = headersResponse.get("Location");
        List<String> setCookieHeader = headersResponse.get("Set-Cookie");

        if (setCookieHeader != null) {
            setCookie(setCookieHeader);
        }

        if (location == null) {
            lastRequestURL = httpsURLConnection.getURL().toString();

            return new RequestInfo(httpsURLConnection, responseText, getDependencies(responseText));
        } else {
            return GET(location.get(0));
        }
    }

    private void setCookie(List<String> setCookieHeader) {
        setCookieHeader.stream().forEach(cookieStr -> {
            String[] cookieData = cookieStr.split("; ")[0].split("=");

            if (cookieData[1].equals("deleted")) {
                cookies.remove(cookieData[0]);
            }
        });

        setCookieHeader.stream().forEach(cookieStr -> {
            String[] cookieData = cookieStr.split("; ")[0].split("=");

            if (!cookieData[1].equals("deleted")) {
                cookies.put(cookieData[0], cookieData[1]);
            }
        });
    }

    private Map getDependencies(String responseText) {
        Map dependencies = new HashMap();
        Matcher matherSrc =  Pattern.compile("src=\"(.+?)\"").matcher(responseText);
        Matcher matcherCss = Pattern.compile("<link.+?href=\"(.+?)\">").matcher(responseText);
        String urlPath;

        while (matherSrc.find()) {
            try {
                urlPath = matherSrc.group(1);
                if (!dependencies.containsKey(urlPath)) {
                    dependencies.put(urlPath, Request.GET(urlPath, getHeadersDependencies()));
                }
            } catch (IOException ex) {}
        }

        while (matcherCss.find()) {
            try {
                urlPath = matcherCss.group(1);
                if (!dependencies.containsKey(urlPath)) {
                    dependencies.put(urlPath, Request.GET(urlPath, getHeadersDependencies()));
                }
            } catch (IOException ex) {}
        }

       return dependencies;
    }

    private Map<String, String> getRequestHeaders() {
        Map<String, String> requestHeaders = getHeadersDependencies();

        requestHeaders.put("Cache-Control", "no-cache");
        requestHeaders.put("Cookie", getCookie());

        return  requestHeaders;
    }

    private Map<String, String> getHeadersDependencies() {
        Map<String, String> requestHeaders = new HashMap<>();

        if (lastRequestURL != null) {
            //requestHeaders.put("Referer", lastRequestURL);
        }

        requestHeaders.put("Accept", "*/*");
        requestHeaders.put("Accept-Language", "en-US,en;q=0.9,ru-RU;q=0.8,ru;q=0.7,uk;q=0.6");
        requestHeaders.put("User-Agent", USER_AGENT);

        return requestHeaders;
    }
}
