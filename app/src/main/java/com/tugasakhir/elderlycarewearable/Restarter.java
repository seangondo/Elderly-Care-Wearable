package com.tugasakhir.elderlycarewearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class Restarter extends BroadcastReceiver {
    @RequiresApi(api = 33)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Broadcast Listened", "Service tried to stop");

        context.startForegroundService(new Intent(context, HeartRateService.class));
//        context.startService(new Intent(context, HeartRateService.class));

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(new Intent(context, HeartRateService.class));
//        } else {
//            context.startService(new Intent(context, HeartRateService.class));
//        }
    }
}
