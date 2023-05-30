package com.tugasakhir.elderlycarewearable;

import static com.tugasakhir.elderlycarewearable.MainActivity.client;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.IntervalDataPoint;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PassiveListenerService extends androidx.health.services.client.PassiveListenerService {

    DBHandler myDb = new DBHandler(this);
    String watch_id = myDb.getWatchIdInfo();

    private String getDateNow() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd-MM-yyyy");
        return simpleDate.format(date);
    }

    @Override
    public void onNewDataPointsReceived(@NonNull DataPointContainer dataPoints) {

        Log.e("PassiveListenerCallback", "Receive Data");
        String date = getDateNow();
        List<IntervalDataPoint<Long>> stepsDaily = dataPoints.getData(DataType.STEPS_DAILY);
        List<IntervalDataPoint<Double>> caloriesDaily = dataPoints.getData(DataType.CALORIES_DAILY);
//                for(int i = 0; i < stepsDaily.size(); i++) {
//                    Log.e("Steps", String.valueOf(stepsDaily.get(i).getValue()));
//                }
        if (stepsDaily.size() > 0) {
            String stepValue = String.valueOf(stepsDaily.get(0).getValue());
            if (myDb.getCountStep(date) > 0) {
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

        if (caloriesDaily.size() > 0) {
            String calValue = String.valueOf(Math.round(caloriesDaily.get(0).getValue()));
            if (myDb.getCountCal(date) > 0) {
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
            super.onNewDataPointsReceived(dataPoints);
        }
    }
}
