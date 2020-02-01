package com.example.hwmbot.helpers;

import javax.net.ssl.HttpsURLConnection;
import java.util.Map;

public class RequestInfo {
    public String responseText;
    public Map dependencies;
    public HttpsURLConnection connection;

    RequestInfo(HttpsURLConnection connection, String responseText, Map dependencies) {
        this.connection = connection;
        this.responseText = responseText;
        this.dependencies = dependencies;
    }
}
