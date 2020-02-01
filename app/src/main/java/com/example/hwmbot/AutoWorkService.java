package com.example.hwmbot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import com.example.hwmbot.HWMService.*;
import com.example.hwmbot.helpers.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoWorkService extends BroadcastReceiver {
    public static int CODE_REQUEST_AUTO_WORK = 102;
    public static int MAX_EXP = 1200;
    public static double MIN_WORKAHOLIC_FINE = 0.4;
    public Context context;

    private static String PATH_DEPENDENCE_CSS = "https://dcdn.heroeswm.ru/sweetalert.css";
    private static AlarmManager alarmManager = null;
    private String login;
    private String password;

    public static boolean isEnabled(Context context) {
        Intent intent = new Intent(context, AutoWorkService.class);

        return PendingIntent.getBroadcast(context, CODE_REQUEST_AUTO_WORK, intent, PendingIntent.FLAG_NO_CREATE) != null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        this.context = context;
        this.login = intent.getStringExtra("login");
        this.password = intent.getStringExtra("password");

        if ("disabled".equals(action)) {
            disableAutoWork();
        } else {
            getJob();
        }
    }

    public void setNextWakeUp() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 1);
        calendar.set(calendar.MINUTE, calendar.get(Calendar.MINUTE) + 1);

        this.setNextWakeUp(calendar);
    }

    public void setNextWakeUp(Calendar calendar) {
        Intent intent = new Intent(context, AutoWorkService.class);
        intent.putExtra("login", login);
        intent.putExtra("password", password);
        PendingIntent pIntentAutoWork = PendingIntent.getBroadcast(context, CODE_REQUEST_AUTO_WORK, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        getAlarmManager().setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntentAutoWork);
    }

    public SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(context.getPackageName(), context.MODE_PRIVATE);
    }

    private void getJob() {
        String cookie = getSharedPreferences().getString("cookie", null);
        HWMService hwmService = new HWMService(login, password, cookie, getStandardCodes());
        new AsyncGetJob(this, hwmService).execute();
    }

    private void disableAutoWork() {
        Intent intent = new Intent(context, AutoWorkService.class);
        PendingIntent pIntentAutoWork = PendingIntent.getBroadcast(context, CODE_REQUEST_AUTO_WORK, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        getAlarmManager().cancel(pIntentAutoWork);
        pIntentAutoWork.cancel();
    }

    private AlarmManager getAlarmManager() {
        if (alarmManager == null) {
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }

        return alarmManager;
    }

    private StandardCodes getStandardCodes() {
        StandardCodes standardCodes = new StandardCodes();
        standardCodes.headCompany = FileReader.read(context.getResources().openRawResource(R.raw.head_company_page));
        standardCodes.formCaptcha = FileReader.read(context.getResources().openRawResource(R.raw.form_captcha));
        standardCodes.formReCaptcha = FileReader.read(context.getResources().openRawResource(R.raw.form_re_captcha));
        standardCodes.dependence.put(PATH_DEPENDENCE_CSS, FileReader.read(context.getResources().openRawResource(R.raw.dependence_css)));

        return standardCodes;
    }
}

class AsyncGetJob extends AsyncTask<Void, Void, String> {
    private int MAX_BAD_RECOGNIZE = 3;
    private AutoWorkService autoWorkService;
    private HWMService hwmService;

    AsyncGetJob(AutoWorkService service, HWMService hwmService) {
        this.autoWorkService = service;
        this.hwmService = hwmService;
    }

    protected String doInBackground(Void ...params) {
        String statusAutoWork = "OK";
        HeroInfo heroInfo = hwmService.getHeroInfo();

        try {
            hwmService.goToHome();
            hwmService.goToMap();
            hwmService.goToCompany();

            // Проверим охоту, если необходимо и есть возможность, нападем
            if (heroInfo.animalsHunting.size() > 0) {
                int indexGoodHunting = getGoodHunting(heroInfo.animalsHunting);

                if (indexGoodHunting == -1) {
                    hwmService.skipHunting();
                } else if (heroInfo.fineWorkaholic <= autoWorkService.MIN_WORKAHOLIC_FINE) {
                    hwmService.goHunting(indexGoodHunting);
                }
            }

            if (!heroInfo.isWorking) {
                statusAutoWork = recognizeAndGetJob(MAX_BAD_RECOGNIZE);
            }
        } catch (HWMException ex) {
            statusAutoWork = ex.getMessage();
        } catch (Exception ex) {
            Log.append(ex.getMessage());
        }

        return statusAutoWork;
    }

    private int getGoodHunting(List<AnimalHunting> animalsHunting) {
        int MIN_EXPERIENCE = autoWorkService.MAX_EXP * 5;
        int currentMinExp = MIN_EXPERIENCE + 1;
        int indexGoodHunting = -1;

        for (int indexHunting = 0; indexHunting < animalsHunting.size(); indexHunting++) {
           AnimalHunting animalHunting = animalsHunting.get(indexHunting);
           String expForOneCreature = getCreaturesExperience().get(animalHunting.name);

           if (expForOneCreature != null) {
               int currentExp = animalHunting.number * Integer.parseInt(expForOneCreature);
               if (currentExp < currentMinExp) {
                   currentMinExp = currentExp;
                   indexGoodHunting = indexHunting;
               }
           }
        }

        return indexGoodHunting;
    }

    private String recognizeAndGetJob(int numberAttempts) throws Exception {
        String recognizedCaptcha;
        String errorMessage = "OK";
        HeroInfo heroInfo = hwmService.getHeroInfo();

        if (heroInfo.urlToCaptcha != null) {
            recognizedCaptcha = CaptchaRecognizer.postCaptcha(heroInfo.urlToCaptcha);
        } else {
            recognizedCaptcha = CaptchaRecognizer.postReCaptcha2(heroInfo.keyReCaptcha, HWMRequests.PATH);
        }

        if (recognizedCaptcha == null) {
            errorMessage = "Не смогли распознать капчу";
        } else {
            try {
                hwmService.getJob(recognizedCaptcha);
                Log.append("Устроились на работу");
                saveInListGetJob();
            } catch (HWMException ex) {
                String textError = ex.getMessage();

                if (textError.equals(HWMException.INCORRECT_CODE)) {
                    CaptchaRecognizer.reportBad();

                    if (numberAttempts > 1) {
                        errorMessage = recognizeAndGetJob(--numberAttempts);
                    } else {
                        errorMessage = textError;
                    }
                } else if (textError.equals(HWMException.OLD_CAPTCHA)) {
                    hwmService.goToMap();
                    hwmService.goToCompany();
                    errorMessage = recognizeAndGetJob(MAX_BAD_RECOGNIZE);
                } else {
                    errorMessage = textError;
                }
            }
        }

        return errorMessage;
    }

    private void saveInListGetJob() {
        try {
            Map<String, String> mapGetJob = null;
            String currentDate = new SimpleDateFormat("dd:MM").format(System.currentTimeMillis());
            String listGetWork = Log.read("sdcard/listGetWork.txt");
            Type empMapType = new TypeToken<Map<String, String>>() {}.getType();
            Integer newValue = 1;


            try {
                mapGetJob = new Gson().fromJson(listGetWork, empMapType);
            } catch (Exception ex) {}

            if (mapGetJob == null) {
                mapGetJob = new HashMap<>();
            }

            if (mapGetJob.containsKey(currentDate)) {
                newValue = Integer.parseInt(mapGetJob.get(currentDate)) + 1;
            }

            mapGetJob.put(currentDate, newValue.toString());
            String body = new Gson().toJson(mapGetJob, Map.class);
            Log.write("sdcard/listGetWork.txt", body);

        } catch (IOException ex) {
            Log.append("Не удалось запись устройство");
        }
    }

    private Map<String, String> getCreaturesExperience() {
        String creaturesExperienceText = FileReader.read(autoWorkService.context.getResources().openRawResource(R.raw.creatures_experience));
        Type empMapType = new TypeToken<Map<String, String>>() {}.getType();

        return new Gson().fromJson(creaturesExperienceText, empMapType);
    }


    protected void onPostExecute(String statusAutoWork) {
        HeroInfo heroInfo = hwmService.getHeroInfo();
        SharedPreferences sharedPreferences = autoWorkService.getSharedPreferences();
        sharedPreferences.edit().putString("cookie", hwmService.getCookie()).commit();

        if (heroInfo.isWorking) {
            autoWorkService.setNextWakeUp(heroInfo.endShift);
        } else {
            autoWorkService.setNextWakeUp();
        }

        if (!statusAutoWork.equals("OK")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(HWMRequests.PATH + HWMRequests.MAP_ADDRESS));
            ServiceNotification.send(autoWorkService.context, "Ошибка в автоустройстве", statusAutoWork, intent);

            Log.append(statusAutoWork);
        }
    }
}
