package com.example.hwmbot;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;


public class ServiceNotification {
    public static void send(Context context, String title, String text, Intent intentActivity) {
        int DEFAULT_ID_NOTIFY = 1;
        int REQ_CODE_INTENT = 1002;
        String NOTIFICATION_CHANNEL_ID = "notification_hwm_bot";
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQ_CODE_INTENT, intentActivity, 0);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(_createNotificationChannel(NOTIFICATION_CHANNEL_ID));
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        notificationManager.notify(DEFAULT_ID_NOTIFY, notificationBuilder.build());
    }

    public static void send(Context context, String title, String text) {
        send(context, title, text, new Intent(context, MainActivity.class));
    }

    private static NotificationChannel _createNotificationChannel(String channelId) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(channelId, "My Notifications", NotificationManager.IMPORTANCE_MAX);
        notificationChannel.setDescription("Sample Channel description");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
        notificationChannel.enableVibration(true);
        notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);

        return notificationChannel;
    }
}
