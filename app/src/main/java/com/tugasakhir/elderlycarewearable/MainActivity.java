package com.tugasakhir.elderlycarewearable;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.HealthServicesClient;
import androidx.health.services.client.PassiveListenerCallback;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.HealthEvent;
import androidx.health.services.client.data.IntervalDataPoint;
import androidx.health.services.client.data.PassiveGoal;
import androidx.health.services.client.data.PassiveListenerConfig;
import androidx.health.services.client.data.PassiveMonitoringCapabilities;
import androidx.health.services.client.data.UserActivityInfo;

import com.google.gson.Gson;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.tugasakhir.elderlycarewearable.databinding.ActivityMainBinding;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RequiresApi(api = 33)
public class MainActivity extends Activity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{

    // INITIALIZE MQTT CONNECTION AND SERVICES
    public static MqttAndroidClient client;
    public String serverUri;

    String watch_id;
    String clientID, mqttUser, mqttPass;

    private String sensor = "heart rate";

    private LinearLayout hrLayout, stepsLayout, calLayout;

    public TextView w_id, databpm, dataSteps, dataCal;
    private ActivityMainBinding binding;
    private BroadcastReceiver mHeartRateReceiver;
    public static int hrTextData = 0;
    private Button sos, msg;

    //Updater
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000;

    //DB Local
    DBHandler myDb = new DBHandler(this);
    Cursor cursorLog;

    public static String myServer;

    Intent serviceIntent;
    public static HeartRateService hrService;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.BODY_SENSORS,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACTIVITY_RECOGNITION
    };

    View.OnClickListener myClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btnSOS:
                    sosDialog sosDialog = new sosDialog(MainActivity.this);
                    try {
                        client.publish(watch_id + "/wearable/sos/message", (watch_id).getBytes(), 0, false);
                        sosDialog.startDialog();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }
    };


    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDetector = new GestureDetectorCompat(this,this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        myServer = getString(R.string.ipWeb);

        // MQTT INIT
        serverUri = getString(R.string.ipServer);
        clientID = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), serverUri, clientID);
        mqttUser = getString(R.string.mqttUser);
        mqttPass = getString(R.string.mqttPass);

        hrLayout = (LinearLayout) findViewById(R.id.linearHr);
        stepsLayout = (LinearLayout) findViewById(R.id.linearSteps);
        calLayout = (LinearLayout) findViewById(R.id.linearCal);

        dataSteps = (TextView) findViewById(R.id.stepsDataTv);
        dataCal = (TextView) findViewById(R.id.caloriesDataTv);
        w_id = (TextView) findViewById(R.id.watchID);
        databpm = (TextView) findViewById(R.id.hrDataTV);

        if (!hasPermission(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            checkPermission();
        }
    }

    void checkPermission() {

        // Health Services
        HealthServicesClient healthClient = HealthServices.getClient(this);
        PassiveMonitoringClient passiveClient = healthClient.getPassiveMonitoringClient();
        ListenableFuture<PassiveMonitoringCapabilities> capabilitiesFuture = passiveClient.getCapabilitiesAsync();
        Futures.addCallback(capabilitiesFuture, new FutureCallback<PassiveMonitoringCapabilities>() {
            @Override
            public void onSuccess(PassiveMonitoringCapabilities result) {
                boolean supportsSteps = result.getSupportedDataTypesPassiveMonitoring().contains(DataType.STEPS_DAILY);
                boolean SupportCalories = result.getSupportedDataTypesPassiveMonitoring().contains(DataType.CALORIES_DAILY);

                if(!supportsSteps) {
                    stepsLayout.setVisibility(View.GONE);
                    Log.e("Health Service", "Steps is not supported!");
                } else {
                    Log.e("Health Service", "Steps is supported!");
                    sensor += ",steps";
                }
                if(!SupportCalories) {
                    calLayout.setVisibility(View.GONE);
                    Log.e("Health Service", "Calories is not supported!");
                } else {
                    Log.e("Health Service", "Calories is supported!");
                    sensor += ",calories";
                }
                Log.e("Sensor Supported", sensor);

                if(myDb.getWatchIdInfo() == null) {
                    getWatchID();
                }
                else {
                    startPassiveData();
//                    startApps();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // display an error
                Log.e("Health Service", String.valueOf(t));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("SetTextI18n")
    void startApps() {

        // START PROGRAM HERE
        Log.e("Wearable Elder", "Apps Starting!");
        sos = (Button) findViewById(R.id.btnSOS);
        sos.setOnClickListener(myClickListener);

        watch_id = myDb.getWatchIdInfo();
        w_id.setText(watch_id);

        String steps = myDb.getSteps(getDateNow());
        String cals = myDb.getCal(getDateNow());

        dataSteps.setText(steps + " Steps");
        dataCal.setText(cals + " Cals");

        Log.e("Watch ID", watch_id);

        try {
            client.publish(watch_id + "/wearable/sensor/steps", steps.getBytes(), 0, false);
            client.publish(watch_id + "/wearable/sensor/calories", cals.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }

//        if(myDb.getCountStep(getDateNow()) > 0) {
//            steps = myDb.getSteps(getDateNow());
//            dataSteps.setText(steps + " Steps");
//            try {
//                Log.e("Steps", String.valueOf(myDb.getCountStep(getDateNow())));
//                client.publish(watch_id + "/wearable/sensor/steps", steps.getBytes(), 0, false);
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        } else {
//            dataSteps.setText("0 Steps");
//            try {
//                client.publish(watch_id + "/wearable/sensor/steps", steps.getBytes(), 0, false);
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        }
//        if(myDb.getCountCal(getDateNow()) > 0) {
//            cals = myDb.getCal(getDateNow());
//            dataCal.setText(cals + " Cals");
//            try {
//                client.publish(watch_id + "/wearable/sensor/calories", steps.getBytes(), 0, false);
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        } else {
//            dataCal.setText("0 Cals");
//            try {
//                client.publish(watch_id + "/wearable/sensor/calories", steps.getBytes(), 0, false);
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        }
//        dataSteps.setText("0 Steps");
//        dataCal.setText("0 Cals");

    }

    private void startPassiveData() {
        HealthServicesClient healthClient = HealthServices.getClient(this);
        PassiveMonitoringClient passiveClient = healthClient.getPassiveMonitoringClient();

        PassiveListenerConfig passiveListenerConfig = PassiveListenerConfig.builder()
                .setDataTypes(Collections.singleton(DataType.STEPS_DAILY))
                .setDataTypes(Collections.singleton(DataType.STEPS))
                .setDataTypes(Collections.singleton(DataType.CALORIES_DAILY))
                .build();

        PassiveListenerCallback passiveListenerCallback = new PassiveListenerCallback() {
            @Override
            public void onRegistered() {
                Log.e("PassiveListenerCallback", "Regis Success");
                startServices();

            }

            @Override
            public void onRegistrationFailed(@NonNull Throwable throwable) {
                Log.e("PassiveListenerCallback", String.valueOf(throwable));
            }

            @Override
            public void onNewDataPointsReceived(@NonNull DataPointContainer dataPointContainer) {
                Log.e("PassiveListenerCallback", "Receive Data");
                String date = getDateNow();
                List<IntervalDataPoint<Long>> stepsDaily = dataPointContainer.getData(DataType.STEPS_DAILY);
                List<IntervalDataPoint<Long>> steps = dataPointContainer.getData(DataType.STEPS);
                List<IntervalDataPoint<Double>> caloriesDaily = dataPointContainer.getData(DataType.CALORIES_DAILY);
                Log.e("Steps", String.valueOf(steps.get(0).getValue()));
                if(stepsDaily.size() > 0) {
                    String stepValue = String.valueOf(stepsDaily.get(0).getValue());
                    if(myDb.getCountStep(date) > 0) {
                        Log.e("Steps", "Steps Update");
                        myDb.updateSteps(stepValue, date);
                    } else {
                        Log.e("Steps", "Steps Insert");
                        myDb.insertSteps(stepValue, date);
                    }

                    try {
                        client.publish(watch_id + "/wearable/sensor/steps", stepValue.getBytes(), 0, false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Log.e("Step_daily : ", String.valueOf(stepsDaily.get(0).getValue()));
                }

                if(caloriesDaily.size() > 0) {
                    String calValue = String.valueOf(Math.round(caloriesDaily.get(0).getValue()));
                    if(myDb.getCountCal(date) > 0) {
                        Log.e("Cal", "Cal Update");
                        myDb.updateCal(calValue, date);
                    } else {
                        Log.e("Cal", "Cal Insert");
                        myDb.insertCal(calValue, date);
                    }

                    try {
                        client.publish(watch_id + "/wearable/sensor/calories", calValue.getBytes(), 0, false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Log.e("Calories_daily : ", String.valueOf(caloriesDaily.get(0).getValue()));
                }
            }

            @Override
            public void onUserActivityInfoReceived(@NonNull UserActivityInfo userActivityInfo) {
                Log.e("PassiveListenerCallback", String.valueOf(userActivityInfo));

            }

            @Override
            public void onGoalCompleted(@NonNull PassiveGoal passiveGoal) {
                Log.e("PassiveListenerCallback", String.valueOf(passiveGoal));

            }

            @Override
            public void onHealthEventReceived(@NonNull HealthEvent healthEvent) {
                Log.e("PassiveListenerCallback", String.valueOf(healthEvent));

            }

            @Override
            public void onPermissionLost() {
                Log.e("PassiveListenerCallback", "Permission has lost!");

            }
        };
        passiveClient.setPassiveListenerCallback(passiveListenerConfig, passiveListenerCallback);
    }

    private void startServices() {
        startMqtt();
        hrService = new HeartRateService();
        serviceIntent = new Intent(this, hrService.getClass());
        if(!isMyServiceRunning(hrService.getClass())) {
            startForegroundService(new Intent(MainActivity.this, HeartRateService.class));
        }

        mHeartRateReceiver = new BroadcastReceiver() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("sensor_data")) {
                    int heartRate = intent.getIntExtra("heart_rate", 0);
                    int onBody = intent.getIntExtra("onbody_detect", 0);
                    if(heartRate > 0 && onBody == 1) {
                        databpm.setText(heartRate + " Bpm");
                    } else {
                        databpm.setText("Measuring...");
                    }
                    if (onBody == 0) {
                        databpm.setText("Please use watch!");
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("sensor_data");
        registerReceiver(mHeartRateReceiver, filter);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("Service status", "Running");
                return true;
            }
        }
        Log.i ("Service status", "Not running");
        return false;
    }

    @Override
    protected void onDestroy() {
        //stopService(mServiceIntent);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restart_service");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }


    private void startMqtt() {
        try {
            Log.d("Token", String.valueOf(client.isConnected()));
            if(!client.isConnected()){
                MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                mqttConnectOptions.setConnectionTimeout(3000);
                mqttConnectOptions.setAutomaticReconnect(true);
                mqttConnectOptions.setCleanSession(true);
                mqttConnectOptions.setUserName(mqttUser);
                mqttConnectOptions.setPassword(mqttPass.toCharArray());
                IMqttToken token = client.connect(mqttConnectOptions);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.e("MQTT Connect", "Success");
                        startApps();
                        subscribeToTopic(watch_id);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        if(String.valueOf(exception).contains("failed to connect")) {
                            Toast.makeText(MainActivity.this, "Server unavailable! Try again in few moment!", Toast.LENGTH_LONG).show();
                            Log.e("Login Failed!", "Server unavailable!");
                        } else if(String.valueOf(exception).contains("Not authorized to connect")) {
                            Toast.makeText(MainActivity.this, "Contact admin! Something wrong with this apps!", Toast.LENGTH_LONG).show();
                            Log.e("Login Failed!", "Wrong user/password");
                        }
                    }
                });
            }
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void subscribeToTopic(String topic) {
        try {
            client.subscribe(topic + "/#", 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("Mqtt","Subscribed : " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void getWatchID() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(myServer+":8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);
        int min = 40001;
        int max = 49999;
        int randId = new Random().nextInt((max-min)+1) + min;
        Log.e("Sensor Supported", sensor);
        Call<Object> getWatchId = retrofitAPI.getWatchId(String.valueOf(Math.round(randId)), sensor);
        getWatchId.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.e("Wearable Retrofit", "Success");
                String res = new Gson().toJson(response.body());
                try {
                    Log.e("response", res);
                    JSONObject obj = new JSONObject(res);
                    if(obj.getInt("available") > 0) {
                        getWatchID();
                    } else {
                        myDb.insertWatchID(obj.getString("watch_id"));
                        startApps();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e("Wearable Retrofit", String.valueOf(t));
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == 0) {
            boolean permissionGranted = true;
            for (int i = 0; i < permissions.length; ++i) {
                if (grantResults[i] == PERMISSION_DENIED) {
                    //User denied permissions twice - permanent denial:
                    if (!shouldShowRequestPermissionRationale(permissions[i])) {
                        Log.e("permissions", permissions[i]);
                        Toast.makeText(getApplicationContext(), "Grant permission from settings!", Toast.LENGTH_LONG).show();
                        //User denied permissions once:
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Permission need for measure!", Toast.LENGTH_LONG).show();
                    }
                    permissionGranted = false;
                    break;
                }
            }
            if (permissionGranted) {
//                if(myDb.getWatchIdInfo() != null) {
//                    startApps();
//                } else {
//                    getWatchID();
//                }
                checkPermission();
            }
//        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static boolean hasPermission(Context context, String... permissions) {
        if(context != null && permissions != null) {
            for(String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
            }
        }
        return true;
    }

    public void onDetails(View view) {
        Intent intent = new Intent(this, WatchDetails.class);
        intent.putExtra("watch_id", myDb.getWatchIdInfo());
        startActivity(intent);
    }

    public void onMsg(View view) {
        Intent intent = new Intent(this, messages_activity.class);
        intent.putExtra("start", 0);
        startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
//        Log.d(DEBUG_TAG,"onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.e(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
        Log.e(DEBUG_TAG, "x: " + String.valueOf(velocityX) + "; y: " + String.valueOf(velocityY));
        if(velocityX < -500) {
            Intent intent = new Intent(this, messages_activity.class);
            intent.putExtra("start", 0);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
//        Log.d(DEBUG_TAG, "onScroll: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
//        Log.e(DEBUG_TAG, "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        sosDialog sosDialog = new sosDialog(MainActivity.this);
        try {
            client.publish(watch_id + "/wearable/sos/message", (myDb.getWatchIdInfo()).getBytes(), 0, false);
            sosDialog.startDialog();
        } catch (MqttException e) {
            e.printStackTrace();
        }
//        Log.e(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
//        Log.e(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }


    @Override
    public void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                setData();
                handler.postDelayed(runnable, delay);
            }
        }, delay);
        super.onResume();
    }

    private void setData() {
        if(myDb.getCountStep(getDateNow()) > 0) {
            dataSteps.setText(myDb.getSteps(getDateNow()) + " Steps");
        } else {
            dataSteps.setText("0 Steps");
        }
        if(myDb.getCountCal(getDateNow()) > 0) {
            dataCal.setText(myDb.getCal(getDateNow()) + " Cals");
        } else {
            dataCal.setText("0 Cals");
        }
    }

    private String getDateNow() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd-MM-yyyy");
        return simpleDate.format(date);
    }
}