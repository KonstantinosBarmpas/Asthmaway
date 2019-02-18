package com.barmpas.asthma;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.barmpas.asthma.data.SessionContract;
import com.sfyc.ctpv.CountTimeProgressView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.sql.Time;
import java.util.Calendar;

import static android.view.View.GONE;
import static java.lang.Math.abs;

/*
 * The Main Activity of the app. This establishes the connection with the broker. Sends / Receives messages
 * (using Publish / Subscribe method). Upon activation of the session it sends a message to warm up the sensor.
 * If the user responds back, it sets the clock to count 20 mins. Once these 20 mins elapse then it wait for the
 * session to start. Receives messages of the results of the user and either tell him/her to keep breathing or
 * that they made a mistake. When the session ends it stores the data to the SQLite database. One last functionality
 * is to check if the device is still connected using the last date the device sent to the broker.
 * If the broker is not connected to the app the UI changes and waits for reconnection.
 */


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //Variables

    MqttAndroidClient client;
    Handler handler, handler_breathe;
    Runnable runnable, runnable_breathe;
    LinearLayout main_layout, broker_down_layout, loading_layout, start_layout, breathe_layout;
    at.markushi.ui.CircleButton connect_btn, led, led_start, led_breathe;
    int breathes_in, air_in, number_of_presses;
    long start_time;
    com.sfyc.ctpv.CountTimeProgressView counter;
    TextView device, device_start, device_breathe, text_breathe, text_error, text_press, start_press;
    Button start_btn, end_btn;
    Vibrator vibrator;
    boolean breathe_in, counter_pressed;
    String numberString;
    String errorString;
    SharedPreferences prefs;
    int pi_month, pi_year, pi_day;
    int pi_hour, pi_min, pi_second;

    private static final int NOTIFICATION_ID = 42;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent1 = new Intent("com.myapp.mycustomaction");
        sendBroadcast(intent1);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        connect_btn = findViewById(R.id.connect);
        device = (TextView) findViewById(R.id.device);
        device_start = (TextView) findViewById(R.id.device_start);
        device_breathe = (TextView) findViewById(R.id.device_breathe);

        text_breathe = (TextView) findViewById(R.id.text_breath);
        text_error = (TextView) findViewById(R.id.text_error);
        text_press = (TextView) findViewById(R.id.text_press);

        start_press = (TextView) findViewById(R.id.start_press);

        start_btn = (Button) findViewById(R.id.start_btn);
        end_btn = (Button) findViewById(R.id.end_btn);

        led = (at.markushi.ui.CircleButton) findViewById(R.id.indicator_led);
        led_start = (at.markushi.ui.CircleButton) findViewById(R.id.indicator_led_start);
        led_breathe = (at.markushi.ui.CircleButton) findViewById(R.id.indicator_led_breathe);

        main_layout = findViewById(R.id.main_layout);
        broker_down_layout = findViewById(R.id.broker_down);
        loading_layout = findViewById(R.id.loading);
        start_layout = findViewById(R.id.start_layout);
        breathe_layout = findViewById(R.id.breath_layout);

        main_layout.setVisibility(GONE);
        broker_down_layout.setVisibility(GONE);
        breathe_layout.setVisibility(GONE);

        prefs = this.getSharedPreferences(
                "com.barmpas.asthma", Context.MODE_PRIVATE);
        number_of_presses = prefs.getInt("number_of_presses", 0);

        connect_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.out.println("Connect pressed");
                try {
                    client.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                new ConnectTask().execute("");
            }
        });

        end_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    endSession();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        start_btn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onClick(View v) {
                try {
                    if (client.isConnected()) {
                        Publish_Start();
                        Start_Session();
                        //Wait for the first inhaler press
                        start_press.setText("Press inhaler button");
                        start_btn.setVisibility(View.INVISIBLE);
                    } else {
                        Toast.makeText(MainActivity.this, "Device not Connected", Toast.LENGTH_LONG);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        counter = findViewById(R.id.counter);
        counter.addOnEndListener(new CountTimeProgressView.OnEndListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onAnimationEnd() {
                start_layout.setVisibility(View.VISIBLE);
                start_press.setText("Device is ready");
                start_btn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onClick(long overageTime) {
                if (counter.isRunning()) {
                    Count();
                    counter.cancelCountTimeAnimation();
                } else {
                    Count();
                    if (client.isConnected()) {
                        try {
                            //Start setup on the device
                            counter_pressed = true;
                            Publish_Settup();
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Device not Connected", Toast.LENGTH_LONG);
                        counter_pressed = false;
                    }
                    Subscribe("IC.embedded/SMARTasses/setup");
                }
            }
        });
        new ConnectTask().execute("");
    }


    //Count the 20 mimutes
    public void Count() {
        counter.setBackgroundColorCenter(Color.parseColor("#00B8D4"));
        counter.setBorderWidth(3);
        counter.setBorderBottomColor(Color.GRAY);
        counter.setBorderDrawColor(Color.parseColor("#64FFDA"));
        counter.setMarkBallColor(Color.parseColor("#64FFDA"));

        counter.setMarkBallFlag(true);
        counter.setMarkBallWidth(18);
        counter.setTitleCenterText("");
        counter.setTitleCenterTextSize(20);
        counter.setTitleCenterTextColor(Color.WHITE);

        counter.setCountTime(10000 * 6 * 20);
        counter.setStartAngle(0);
        counter.setTextStyle(CountTimeProgressView.TextStyle.INSTANCE.getCLOCK());
        counter.setClockwise(true);
    }

    //Start session
    public void Start_Session() {
        default_values();
        start_time = System.currentTimeMillis();
    }

    //Store in the SQLite Database
    public void saveSession() {
        ContentValues values = new ContentValues();

        values.put(SessionContract.SessionEntry.COLUMN_ID, start_time);

        values.put(SessionContract.SessionEntry.COLUMN_BREATHES_IN, numberString);

        values.put(SessionContract.SessionEntry.COLUMN_ERROR, errorString);

        if (!numberString.isEmpty() && !errorString.isEmpty()) {
            Uri newUri = getContentResolver().insert(SessionContract.SessionEntry.CONTENT_URI, values);
            if (newUri != null) {
                Toast.makeText(this, "Success to store", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Fail to store", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Reset values
    public void default_values() {
        air_in = 0;
        numberString = "";
        errorString = "";
    }

    //Connect asychronously to the MQTT Broker
    public class ConnectTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            ConnectToMQTT();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            loading_layout.setVisibility(GONE);
            // Init
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (!client.isConnected()) {
                        broker_down_layout.setVisibility(View.VISIBLE);
                        main_layout.setVisibility(GONE);
                    } else {
                        //Subscribe to topics
                        System.out.println("SUBCRIBE");
                        Subscribe("IC.embedded/SMARTasses/data");
                        Subscribe("IC.embedded/SMARTasses/setup");
                        Subscribe("IC.embedded/SMARTasses/date");
                        Subscribe("IC.embedded/SMARTasses/press");
                        //Constantly check if device is connnected
                        Calendar c = Calendar.getInstance();
                        int phone_year = c.get(Calendar.YEAR);
                        int phone_month = c.get(Calendar.MONTH) + 1;
                        int phone_day = c.get(Calendar.DATE);
                        int hours = new Time(System.currentTimeMillis()).getHours();
                        int minutes = new Time(System.currentTimeMillis()).getMinutes();
                        int seconds = new Time(System.currentTimeMillis()).getSeconds();
                        if (!(pi_year == phone_year && pi_month == phone_month && pi_day == phone_day && pi_hour == hours && pi_min == minutes && abs(seconds - pi_second) < 10)) {
                            led.setColor(Color.RED);
                            led_start.setColor(Color.RED);
                            led_breathe.setColor(Color.RED);
                            device.setText("Device is OFF");
                            device_start.setText("Device is OFF");
                            device_breathe.setText("Device is OFF");
                        }
                    }
                    handler.postDelayed(this, 1000);
                }
            };
            //Start
            handler.postDelayed(runnable, 1000);
        }

        //UI in not connected
        @Override
        protected void onPreExecute() {
            loading_layout.setVisibility(View.VISIBLE);
            broker_down_layout.setVisibility(GONE);
            main_layout.setVisibility(GONE);
            start_layout.setVisibility(GONE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }
    }

    //End session
    public void endSession() throws MqttException {
        saveSession();
        Publish_End();
        counter.clearAnimation();
        counter.setTitleCenterText("START SESSION");
        breathe_layout.setVisibility(View.GONE);
        main_layout.setVisibility(View.VISIBLE);
        handler_breathe.removeCallbacksAndMessages(null);
    }

    //Subscribe to topics and handle results
    public void Subscribe(String topic) {
        try {
            if (client.isConnected()) {
                client.subscribe(topic, 1);
                client.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable cause) {

                    }

                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {

                        if (topic.equals("IC.embedded/SMARTasses/press")) {
                            //Keep breathing
                            if (message.toString().equals("1")) {
                                if (client.isConnected()) {
                                    breathe_layout.setVisibility(View.VISIBLE);
                                    start_layout.setVisibility(View.GONE);
                                    // Init
                                    breathe_in = true;
                                    handler_breathe = new Handler();
                                    runnable_breathe = new Runnable() {
                                        @Override
                                        public void run() {
                                            if (breathe_in) {
                                                breathe_in = false;
                                                text_breathe.setVisibility(View.VISIBLE);
                                                text_breathe.setText("Breathe in...");
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                                                } else {
                                                    //deprecated in API 26
                                                    vibrator.vibrate(1000);
                                                }
                                            } else {
                                                text_breathe.setVisibility(View.VISIBLE);
                                                text_breathe.setText("Breathe out...");
                                                text_error.setVisibility(View.GONE);
                                                text_press.setVisibility(View.GONE);
                                                breathe_in = true;
                                            }
                                            handler_breathe.postDelayed(this, 2000);
                                        }
                                    };
                                    //Start
                                    handler_breathe.postDelayed(runnable_breathe, 100);
                                } else {
                                    Toast.makeText(MainActivity.this, "Device not Connected", Toast.LENGTH_LONG);
                                }
                            }
                        }

                        //Setup device correct activate clock
                        if (topic.equals("IC.embedded/SMARTasses/setup")) {
                            if (message.toString().equals("0")) {
                                counter_pressed = false;
                                counter.startCountTimeAnimation();
                            } else {
                                Toast.makeText(MainActivity.this, "Device not Connected", Toast.LENGTH_LONG);
                            }
                        }


                        //Receive breaths and errors and stores them
                        if (topic.equals("IC.embedded/SMARTasses/data")) {
                            JSONObject Json = new JSONObject(message.toString());
                            int error = Json.getInt("error");
                            breathes_in = Json.getInt("breath");
                            air_in++;

                            if (numberString.equals("")) {
                                numberString = String.valueOf(breathes_in);
                            } else {
                                numberString = numberString + "/" + String.valueOf(breathes_in);
                            }

                            if (errorString.equals("")) {
                                errorString = String.valueOf(error);
                            } else {
                                errorString = errorString + "/" + String.valueOf(error);
                            }

                            if (error == 1) {
                                text_breathe.setVisibility(GONE);
                                text_error.setVisibility(View.VISIBLE);
                            } else if (error == 0) {
                                handler_breathe.removeCallbacksAndMessages(null);
                                text_breathe.setVisibility(GONE);
                                text_press.setVisibility(View.VISIBLE);
                            }

                            if (air_in == number_of_presses) {
                                endSession();
                            }

                        }


                        //Parse and check date of the device and of the phone
                        if (topic.equals("IC.embedded/SMARTasses/date")) {
                            JSONObject Json = new JSONObject(message.toString());
                            String date = Json.getString("full");

                            Calendar c = Calendar.getInstance();
                            int phone_year = c.get(Calendar.YEAR);
                            int phone_month = c.get(Calendar.MONTH) + 1;
                            int phone_day = c.get(Calendar.DATE);
                            int hours = new Time(System.currentTimeMillis()).getHours();
                            int minutes = new Time(System.currentTimeMillis()).getMinutes();
                            int seconds = new Time(System.currentTimeMillis()).getSeconds();

                            String[] array = date.split(" ");

                            if (array[1].equals("Jan")) {
                                pi_month = 1;
                            } else if (array[1].equals("Feb")) {
                                pi_month = 2;
                            } else if (array[1].equals("Mar")) {
                                pi_month = 3;
                            } else if (array[1].equals("Apr")) {
                                pi_month = 4;
                            } else if (array[1].equals("May")) {
                                pi_month = 5;
                            } else if (array[1].equals("Jun")) {
                                pi_month = 6;
                            } else if (array[1].equals("Jul")) {
                                pi_month = 7;
                            } else if (array[1].equals("Aug")) {
                                pi_month = 8;
                            } else if (array[1].equals("Sep")) {
                                pi_month = 9;
                            } else if (array[1].equals("Oct")) {
                                pi_month = 10;
                            } else if (array[1].equals("Nov")) {
                                pi_month = 11;
                            } else {
                                pi_month = 12;
                            }

                            pi_year = Integer.parseInt(array[4]);
                            pi_day = Integer.parseInt(array[2]);

                            String[] array1 = array[3].split(":");

                            pi_hour = Integer.parseInt(array1[0]);
                            pi_min = Integer.parseInt(array1[1]);
                            pi_second = Integer.parseInt(array1[2]);

                            if ((pi_year == phone_year && pi_month == phone_month && pi_day == phone_day && pi_hour == hours && pi_min == minutes && abs(seconds - pi_second) < 10)) {
                                led.setColor(Color.GREEN);
                                led_start.setColor(Color.GREEN);
                                led_breathe.setColor(Color.GREEN);
                                device.setText("Device is ON");
                                device_start.setText("Device is ON");
                                device_breathe.setText("Device is ON");
                            } else {
                                led.setColor(Color.RED);
                                led_start.setColor(Color.RED);
                                led_breathe.setColor(Color.RED);
                                device.setText("Device is OFF");
                                device_start.setText("Device is OFF");
                                device_breathe.setText("Device is OFF");
                            }
                        }

                        System.out.println(numberString + errorString);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });

            }
        } catch (Exception e) {
            Log.d("tag", "Error :" + e);
        }
    }

    //Publish to start setup
    public void Publish_Settup() throws MqttException {
        String topic = "IC.embedded/SMARTasses/setup";
        String payload = "1";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    //Publish start of session
    public void Publish_Start() throws MqttException {
        String topic = "IC.embedded/SMARTasses/start";
        String payload = "1";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    //Publish end of session
    public void Publish_End() throws MqttException {
        String topic = "IC.embedded/SMARTasses/start";
        String payload = "0";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    //Attempts to connect to the broker
    public void ConnectToMQTT() {
        String clientId = "SMART_ASSES_IMPERIAL_1";
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://iot.eclipse.org:1883",
                clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("MQTT Connection Success", "onSuccess");
                    if (client.isConnected()) {
                        System.out.println("Broker ok");
                        broker_down_layout.setVisibility(GONE);
                        main_layout.setVisibility(View.VISIBLE);
                        //Count();
                    } else {
                        System.out.println("Broker down");
                        broker_down_layout.setVisibility(View.VISIBLE);
                        main_layout.setVisibility(GONE);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.e("MQTT Connection Fail", "onFailure: " + exception);
                    exception.printStackTrace();
                    if (client.isConnected()) {
                        System.out.println("Broker ok");
                        broker_down_layout.setVisibility(GONE);
                        main_layout.setVisibility(View.VISIBLE);
                    } else {
                        System.out.println("Broker down");
                        broker_down_layout.setVisibility(View.VISIBLE);
                        main_layout.setVisibility(GONE);
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            finish();
            super.onBackPressed();
        }
    }

    //Handle main menus
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.website) {

        }

        return super.onOptionsItemSelected(item);
    }


    //Handle left menu
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_database) {
            Intent myIntent = new Intent(MainActivity.this, DatabaseActivity.class);
            MainActivity.this.startActivity(myIntent);
        } else if (id == R.id.nav_settings) {
            Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(myIntent);
        } else if (id == R.id.nav_air) {
            Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
            MainActivity.this.startActivity(myIntent);
        } else if (id == R.id.nav_send) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(MainActivity.this, notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Present a notification to the user
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            String channelId = "channel-id";
            String channelName = "Inhaler";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(
                        channelId, channelName, importance);
                manager.createNotificationChannel(mChannel);
            }

            //Create action intent
            Intent action = new Intent(this, MainActivity.class);
            action.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            action.setAction("foo");
            PendingIntent operation =
                    PendingIntent.getActivity(this, 1, action, PendingIntent.FLAG_CANCEL_CURRENT);

            Notification note = new NotificationCompat.Builder(this)
                    .setContentTitle("Asthmaway")
                    .setContentText("It's time for your asthma inhaler")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(operation)
                    .setAutoCancel(true)
                    .build();

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(500);
            }

            manager.notify(NOTIFICATION_ID, note);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
