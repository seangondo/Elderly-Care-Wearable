package com.tugasakhir.elderlycarewearable;

import static com.tugasakhir.elderlycarewearable.MainActivity.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class HeartRateService extends Service implements SensorEventListener {

    public static final String NOTIFICATION_CHANNEL_ID = "10001" ;
    private final static String default_notification_channel_id = "default" ;

    public static String msg;
    public static String getTopic;

    int heartRate = 0;
    int onBody = 0;

    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor, mSens1, mStep;

    @Override
    public void onCreate() {
        super.onCreate();
        startMyOwnForeground();
//        mSens1 = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
//        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        ArrayList<String> arrayList = new ArrayList<String>();
//        for (Sensor sensor : sensors) {
//            arrayList.add(sensor.getName());
//        }
//        arrayList.forEach((n) -> System.out.println(n));

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // SENSOR MANAGER
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mStep = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mSens1 = mSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT);
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSens1, SensorManager.SENSOR_DELAY_NORMAL);
        mqttServices();
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heartRate = (int) event.values[0];
        }
        if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

        }
        if (event.sensor.getType() == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) {
            onBody = (int) event.values[0];
            if(onBody == 1) {
                mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else if(onBody == 0) {
                mSensorManager.unregisterListener(this, mHeartRateSensor);
            }
        }

        DBHandler myDb = new DBHandler(this);
        if(heartRate > 0 && onBody == 1) {
            try {
                client.publish(myDb.getWatchIdInfo() + "/wearable/sensor/heart_rate", String.valueOf(heartRate).getBytes(), 0, false);
                client.publish(myDb.getWatchIdInfo() + "/wearable/sensor/onbody", String.valueOf(onBody).getBytes(), 0, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        if (onBody == 0) {
            try {
                client.publish(myDb.getWatchIdInfo() + "/wearable/sensor/onbody", String.valueOf(onBody).getBytes(), 0, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        Intent intent = new Intent();
        intent.setAction("sensor_data");
        intent.putExtra("heart_rate", heartRate);
        intent.putExtra("onbody_detect", onBody);
        sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
//        Log.e("Sensor", String.valueOf(sensor));
//        Log.e("Accuracy", String.valueOf(accuracy));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mSensorManager.unregisterListener(this);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restart_service");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void mqttServices() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("Connection MQTT", String.valueOf(cause));

            }

            @RequiresApi(api = 33)
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                DBHandler myDb = new DBHandler(getApplicationContext());
                getTopic = topic;
                msg = new String(message.getPayload());
//                Log.d("MQTT Topic", getTopic);
//                Log.d("Mqtt Msg", msg);
                if(topic.contains("messages")) {
                    JSONObject obj = new JSONObject(msg);
                    Log.e("Messages", String.valueOf(obj));
                    myDb.insertMsgs(obj);
                    createNotification(obj.getString("message"));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    @RequiresApi(api = 33)
    public void createNotification (String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("goAct", "Notification");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService( NOTIFICATION_SERVICE ) ;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext() , default_notification_channel_id ) ;
        mBuilder.setContentTitle("Elderly Care")
                .setContentText("Warning!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_heart);
        int importance = NotificationManager. IMPORTANCE_HIGH ;
        NotificationChannel notificationChannel = new NotificationChannel( NOTIFICATION_CHANNEL_ID , "NOTIFICATION_CHANNEL_NAME" , importance) ;
        mBuilder.setChannelId( NOTIFICATION_CHANNEL_ID ) ;
        assert mNotificationManager != null;
        mNotificationManager.createNotificationChannel(notificationChannel) ;
        //        assert mNotificationManager != null;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        mNotificationManager.notify((int)System.currentTimeMillis() , mBuilder.build()) ;
        notificationManager.notify((int)System.currentTimeMillis() , mBuilder.build()) ;
    }
}
