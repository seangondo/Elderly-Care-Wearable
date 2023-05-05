package com.tugasakhir.elderlycarewearable;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.constraintlayout.widget.ConstraintLayout;

public class sosDialog {
    private Activity activity;
    private AlertDialog dialog;

    public sosDialog(Activity myActivity) {
        activity = myActivity;
    }

    public void startDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.sos_dialog, null));
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#f22b1d")));
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                dismissDialog();
            }
        }, 5000);
    }

    public void dismissDialog(){
        dialog.dismiss();
    }
}
