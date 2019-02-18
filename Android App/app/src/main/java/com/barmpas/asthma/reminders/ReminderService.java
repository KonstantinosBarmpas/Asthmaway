package com.barmpas.asthma.reminders;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.barmpas.asthma.MainActivity;
import com.barmpas.asthma.R;


public class ReminderService extends IntentService {

    private static final String TAG = ReminderService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 42;


    public ReminderService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //Present a notification to the user
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Create action intent
        Intent action = new Intent(this, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        action.setAction("foo");

        PendingIntent operation =
                PendingIntent.getActivity(this, 0, action, PendingIntent.FLAG_CANCEL_CURRENT);

        //Create the notifiaction
        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle("iBreathe")
                .setContentText("It's time for your asthma inhaler")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(operation)
                .setAutoCancel(true)
                .build();

        // Vibrate when the broadcast receiver is triggered
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(500);
        }

        manager.notify(NOTIFICATION_ID, note);

        //Activate the broadcast reveiver
        Intent intent1 = new Intent("com.myapp.mycustomaction");
        sendBroadcast(intent1);

    }
}
