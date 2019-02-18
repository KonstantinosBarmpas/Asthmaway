package com.barmpas.asthma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;


/*
 * The settings activity. This activity allows the user to register his/her GP doses per day and session per day.
 * It uses the Preference method to store the variables even when the app is terminated.
 */


public class SettingsActivity extends AppCompatActivity {
    String[] times, number;
    SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefs = this.getSharedPreferences(
                "com.barmpas.asthma", Context.MODE_PRIVATE);

        int number_per_day = prefs.getInt("number_of_times", 0);
        int number_of_presses = prefs.getInt("number_of_presses", 0);

        final NumberPicker picker_time_per_day = (NumberPicker) findViewById(R.id.times_per_day_picker);
        final NumberPicker picker_number_of_air = (NumberPicker) findViewById(R.id.number_of_air);

        times = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"};
        number = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"};

        picker_time_per_day.setMinValue(0);
        picker_time_per_day.setMaxValue(times.length - 1);
        picker_time_per_day.setDisplayedValues(times);
        picker_time_per_day.setValue(number_per_day);

        picker_number_of_air.setMinValue(0);
        picker_number_of_air.setMaxValue(number.length - 1);
        picker_number_of_air.setDisplayedValues(number);
        picker_number_of_air.setValue(number_of_presses);

        Button save = (Button) findViewById(R.id.result);

        save.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onClick(View v) {
                prefs.edit().putInt("number_of_presses", Integer.parseInt(number[picker_number_of_air.getValue()])).apply();
                prefs.edit().putInt("number_of_times", Integer.parseInt(times[picker_time_per_day.getValue()])).apply();
                Intent intent1 = new Intent("com.myapp.mycustomaction");
                sendBroadcast(intent1);

                finish();

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

}
