package com.example.hwmbot.HWMService;

import com.example.hwmbot.helpers.BrowserSession;
import com.example.hwmbot.helpers.Parser;
import com.example.hwmbot.helpers.RequestInfo;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class HWMRequests {
    public static String HOST = "www.heroeswm.ru";
    public static String PATH = "https://www.heroeswm.ru/";
    public static String LOGIN_ADDRESS = "login.php";
    public static String MAP_ADDRESS = "map.php";
    public static String HOME_ADDRESS = "home.php";
    public static String WAITING_RESULT = "waiting_for_results.php";
    public static String GL_ADDRESS = "leader_guild.php";

    private String login;
    private String password;
    private BrowserSession browserSession = null;

    HWMRequests(String login, String password, String cookies) {
        this.login = login;
        this.password = password;
        this.browserSession = new BrowserSession(HOST, cookies);
    }

    public void authorization() throws IOException {
        authorization(browserSession.GET("").responseText);
    }

    public String getCookie() {
        return browserSession.getCookie();
    }

    public RequestInfo getPage(String urlPath) throws IOException {
        RequestInfo requestInfo = browserSession.GET(urlPath);

        if (isLocationToStartPage(urlPath, requestInfo.connection)) {
            authorization(requestInfo.responseText);
            requestInfo = browserSession.GET(urlPath);
        }

        return requestInfo;
    }

    public RequestInfo send(String urlPath, Map postData) throws IOException {
        RequestInfo requestInfo = browserSession.POST(urlPath, postData);

        if (isLocationToStartPage(urlPath, requestInfo.connection)) {
            authorization(requestInfo.responseText);
            requestInfo = browserSession.POST(urlPath, postData);
        }

        return requestInfo;
    }

    private void authorization(String startPage) throws IOException {
        browserSession.setCookie(null);
        HttpsURLConnection httpsURLConnection = send(LOGIN_ADDRESS, getParamsAuthorization(startPage)).connection;

        if (!httpsURLConnection.getURL().toString().equals(PATH + HOME_ADDRESS)) {
            throw new IOException("Неверный логин и пароль");
        }
    }

    private Map getParamsAuthorization(String startPage) throws IOException {
        String REG_FORM_AUTHORIZATION = "<form.+?action=[\"']" + LOGIN_ADDRESS + ".+?/form>";
        Matcher matcher = Pattern.compile(REG_FORM_AUTHORIZATION).matcher(startPage);
        Map paramsAuthorization;

        if (matcher.find()) {
            String formCode = matcher.group();
            paramsAuthorization = Parser.form(formCode);
            paramsAuthorization.put("login", login);
            paramsAuthorization.put("pass", password);
        } else {
            throw new IOException("Не удалось получить форму авторизации");
        }

        return paramsAuthorization;
    }

    private boolean isLocationToStartPage(String urlPath, HttpsURLConnection httpsURLConnection) {
        return !urlPath.equals("") && httpsURLConnection.getURL().toString().equals(PATH);
    }
}
