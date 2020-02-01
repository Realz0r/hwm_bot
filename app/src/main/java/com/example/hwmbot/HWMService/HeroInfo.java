package com.example.hwmbot.HWMService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HeroInfo {
    // home.php
    public boolean isWorking = false;
    public String workStatus = "Не трудоустроен";
    public Calendar endShift = null;

    // map.php
    public List<AnimalHunting> animalsHunting = new ArrayList();

    // object-info.php
    public String urlToCaptcha = null;
    public String keyReCaptcha = null;
    public double fineWorkaholic = 1.0;
}
