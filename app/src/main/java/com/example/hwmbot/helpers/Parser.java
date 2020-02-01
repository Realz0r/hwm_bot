package com.example.hwmbot.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static String REG_FROM_PARAMS = "<input.+?>";
    private static String REG_INPUT_VALUE = "value=['\"]*(.+?)\\W";
    private static String REG_INPUT_NAME = "name=['\"]*(.+?)\\W";

    public static Map<String, String> form(String formCode) {
        Map<String, String> formData = new HashMap<>();
        Matcher matcherFormParams = Pattern.compile(REG_FROM_PARAMS).matcher(formCode);

        while (matcherFormParams.find()) {
            String inputCode = matcherFormParams.group();
            Matcher matcherNameValue = Pattern.compile(REG_INPUT_NAME).matcher(inputCode);
            Matcher matcherInputValue = Pattern.compile(REG_INPUT_VALUE).matcher(inputCode);

            if (matcherNameValue.find()) {
                String paramName = matcherNameValue.group(1);
                String paramValue = "";

                if (matcherInputValue.find()) {
                    paramValue = matcherInputValue.group(1);
                }

                formData.put(paramName, paramValue);
            }
        }

        return  formData;
    }
}
