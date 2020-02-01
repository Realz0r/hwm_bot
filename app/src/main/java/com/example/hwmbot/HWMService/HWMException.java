package com.example.hwmbot.HWMService;

public class HWMException extends Exception {
    public static String NOT_FREE_SLOT = "В районе нет свободных рабочих мест";
    public static String INCORRECT_CODE = "Каптча распознана неверно";
    public static String OLD_CAPTCHA = "Старая каптча, наступил новый час";
    public static String FAILED_GET_JOB = "Не удалось устроиться на работу";

    public static String CODE_DIFFERENT_FROM_STANDARDS = "Обнаружены различия с эталонами, возможно скрыто поле каптчи";
    public static String NOT_CAPTCHA = "Не удалось получить каптчу";

    public HWMException(String message) {
        super(message);
    }
}
