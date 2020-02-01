package com.example.hwmbot.helpers;

import net.marketer.ruCaptcha.RuCaptcha;

public class CaptchaRecognizer {
    private static String captchaId = null;
    private static String API_KEY = "YOU_KEY";
    private static int DEFAULT_NUMBER_ATTEMPTS = 5;

    private static String waitResult(String response) throws Exception {
        String decryption = null;

        if (response.startsWith("OK")) {
            captchaId = response.substring(3);

            while (true) {
                response = RuCaptcha.getDecryption(captchaId);
                if (response.equals(RuCaptcha.Responses.CAPCHA_NOT_READY.toString())) {
                    Thread.sleep(5000);
                    continue;
                } else if (response.startsWith("OK")) {
                    decryption = response.substring(3);
                    break;
                } else {
                    //обработка ошибок
                }
            }
        }

        return decryption;
    }

    public static String postCaptcha(String urlWorkCode, int numberAttempts) throws Exception {
        RuCaptcha.API_KEY = API_KEY;
        String response = RuCaptcha.postCaptcha(urlWorkCode);
        String decryption = waitResult(response);

        if (decryption == null && numberAttempts > 1) {
            decryption = postCaptcha(urlWorkCode, --numberAttempts);
        }

        return decryption;
    }

    public static String postReCaptcha2(String googleKey, String url, int numberAttempts) throws Exception {
        RuCaptcha.API_KEY = API_KEY;
        String response = RuCaptcha.postReCaptcha2(googleKey, url);
        String decryption = waitResult(response);

        if (decryption == null && numberAttempts > 1) {
            decryption = postReCaptcha2(googleKey, url, --numberAttempts);
        }

        return decryption;
    }

    public static String postCaptcha(String urlWorkCode) throws Exception {
        return postCaptcha(urlWorkCode, DEFAULT_NUMBER_ATTEMPTS);
    }

    public static String postReCaptcha2(String googleKey, String url) throws Exception {
        return postReCaptcha2(googleKey, url, DEFAULT_NUMBER_ATTEMPTS);
    }

    public static void reportBad() {
        try {
            RuCaptcha.reportBad(captchaId);
        } catch (Exception ex) {
            Log.append(ex.getMessage());
        }
    }
}
