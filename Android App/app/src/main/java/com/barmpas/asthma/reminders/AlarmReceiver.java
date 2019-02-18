package com.barmpas.asthma.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

public class AlarmReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        //Schedule alarm on BOOT_COMPLETED
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || intent.getAction().equals("com.myapp.mycustomaction")) {
            scheduleAlarm(context);
            context.startService(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void scheduleAlarm(Context context) {
        AlarmManager manager = AlarmManagerProvider.getAlarmManager(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        //Intent to trigger
        Intent intent = new Intent(context, ReminderService.class);
        PendingIntent operation = PendingIntent
                .getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        preferences = context.getSharedPreferences(
                "com.barmpas.asthma", Context.MODE_PRIVATE);

        int number_per_day = preferences.getInt("number_of_times", 0);

        //Set the intervals to repeat the broadcast receiver based on the number of sessions the GP has registered.

        if (number_per_day != 0) {
            manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (1000 * 60 * 60 * 18 / number_per_day),
                    AlarmManager.INTERVAL_HALF_HOUR, operation);
            try {
                //Play sound when notification is triggered
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context, notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            manager.cancel(operation);
        }
    }

}
