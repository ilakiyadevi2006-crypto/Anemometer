package com.example.anemometer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID = "device_status_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static void showStatusNotification(Context context) {
        updateStatusNotification(context, true);
    }

    public static void updateStatusNotification(Context context, boolean isConnected) {
        AppPreferences prefs = new AppPreferences(context);
        if (!prefs.areNotificationsEnabled()) {
            cancelNotification(context);
            return;
        }

        createNotificationChannel(context);

        String title = isConnected ? "Device Connected" : "Server Disconnected";
        String text = isConnected ? "Ready To Test Data" : "Connection lost. Checking server...";
        int icon = isConnected ? R.drawable.ic_notifications : R.drawable.ic_server;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Permission handled in manifest
        }
    }

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Device Status";
            String description = "Shows connection status of the device";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
