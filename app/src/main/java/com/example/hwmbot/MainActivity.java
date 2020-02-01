package com.example.hwmbot;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hwmbot.HWMService.HWMService;
import com.example.hwmbot.helpers.Permissions;


public class MainActivity extends AppCompatActivity {
    private String login;
    private String password;
    private String cookie;
    private AlarmManager alarmManager;
    private int currentViewId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferencesActivity = getPreferences(MODE_PRIVATE);
        login = preferencesActivity.getString("login", "");
        password = preferencesActivity.getString("password", "");
        cookie = getSharedPreferences().getString("cookie", "");
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (cookie.equals("")) {
            showLoginForm();
        } else {
            new VerificationAuthorizationData(this).execute(login, password, cookie);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (getCurrentViewById() == R.layout.activity_main) {
            updateUI(AutoWorkService.isEnabled(this));
        }
    }

    public void enabledAutoWork(View view) {
        updateUI(true);
        toggleAutoWork(true);
    }

    public void disabledAutoWork(View view) {
        updateUI(false);
        toggleAutoWork(false);
    }

    private void toggleAutoWork(boolean state) {
        Intent intent = new Intent(this, AutoWorkService.class);

        if (state) {
            intent.setAction("enabled");
            intent.putExtra("login", login);
            intent.putExtra("password", password);
        } else {
            intent.setAction("disabled");
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, AutoWorkService.CODE_REQUEST_AUTO_WORK, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
    }

    public void setCurrentViewById(int id) {
        setContentView(id);
        currentViewId = id;
    }

    public int getCurrentViewById() {
        return currentViewId;
    }

    private void updateUI(boolean autoWorkIsEnabled) {
        findViewById(R.id.enabledAutoWork).setEnabled(!autoWorkIsEnabled);
        findViewById(R.id.disabledAutoWork).setEnabled(autoWorkIsEnabled);
    }

    private void showMainActivity() {
        setCurrentViewById(R.layout.activity_main);
        updateUI(AutoWorkService.isEnabled(this));
        Permissions.ignoringBatteryOptimizations(this);
    }

    private void showLoginForm() {
        setCurrentViewById(R.layout.login_page);

        TextView loginView = findViewById(R.id.login);
        TextView passwordView = findViewById(R.id.password);
        loginView.setText(login);
        passwordView.setText(password);
    }


    public void authorizationCallback(String stateAuthorization) {
        if (stateAuthorization.equals("OK")) {
            if (getCurrentViewById() == R.layout.login_page) {
                saveAuthorizationData();
            }

            showMainActivity();
        } else if (getCurrentViewById() == R.layout.login_page) {
            TextView passwordView = findViewById(R.id.password);
            passwordView.setText("");
            Toast.makeText(this, stateAuthorization, Toast.LENGTH_SHORT).show();
        } else {
            showLoginForm();
        }
    }

    public void sendAuthorizationData(View view) {
        TextView loginView = findViewById(R.id.login);
        TextView passwordView = findViewById(R.id.password);

        login = loginView.getText().toString();
        password = passwordView.getText().toString();

        if (login.equals("") || password.equals("")) {
            Toast.makeText(this, "Заполните поля для авторизации", Toast.LENGTH_SHORT).show();
        } else {
            new VerificationAuthorizationData(this).execute(login, password, null);
        }
    }

    public SharedPreferences getSharedPreferences() {
        return getSharedPreferences(getPackageName(), MODE_PRIVATE);
    }


    private void saveAuthorizationData() {
        getPreferences(MODE_PRIVATE).edit()
                .putString("login", login)
                .putString("password", password)
                .commit();
    }
}

class VerificationAuthorizationData extends AsyncTask<String, Void, String> {
    private MainActivity mainActivity;

    VerificationAuthorizationData(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    protected String doInBackground(String ...params) {
        String login = params[0];
        String password = params[1];
        String cookie =  params[2];

        HWMService hwmService = new HWMService(login, password, cookie);
        String stateAuthorization = hwmService.getStatusAuthorization();
        SharedPreferences sharedPreferences = mainActivity.getSharedPreferences();
        sharedPreferences.edit().putString("cookie", hwmService.getCookie()).commit();

        return stateAuthorization;
    }

    protected void onPostExecute(String stateAuthorization) {
        mainActivity.authorizationCallback(stateAuthorization);
    }
}
