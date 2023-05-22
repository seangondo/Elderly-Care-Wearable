package com.tugasakhir.elderlycarewearable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

import com.tugasakhir.elderlycarewearable.databinding.ActivityMessagesBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class messages_activity extends Activity implements GestureDetector.OnGestureListener {

    private TextView tvDate, tvTime, tvMessages;
    private ActivityMessagesBinding binding;

    private int curVal;

    DBHandler myDb;

    private GestureDetectorCompat mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDetector = new GestureDetectorCompat(this,this);

        myDb = new DBHandler(this);

        binding = ActivityMessagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tvDate = (TextView) findViewById(R.id.dateText);
        tvTime = (TextView) findViewById(R.id.timeText);
        tvMessages = (TextView) findViewById(R.id.messagesText);

        Intent intent = getIntent();
        curVal = intent.getIntExtra("start", 0);

        if(myDb.getDataCount() == 0) {
            tvDate.setVisibility(View.INVISIBLE);
            tvTime.setVisibility(View.INVISIBLE);
            tvMessages.setText("No Messages!");
        } else {
            try {
                JSONObject data = myDb.getCurrentMsg(curVal);
                tvDate.setVisibility(View.VISIBLE);
                tvTime.setVisibility(View.VISIBLE);

                tvDate.setText(data.getString("date"));
                tvTime.setText(data.getString("time"));
                tvMessages.setText(data.getString("message"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



    }

    public void backToHome(View view) {
//        Intent intent = null;
//        intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(@NonNull MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
        Log.e("FLING", "x: " + String.valueOf(v) + "; y: " + String.valueOf(v1));
        if(v1 > 300) {
            if(curVal > 0) {
                try {
                    curVal -= 1;
                    JSONObject data = myDb.getCurrentMsg(curVal);
                    Log.e("Data", String.valueOf(curVal));
                    Log.e("Data", String.valueOf(data));
                    tvDate.setVisibility(View.VISIBLE);
                    tvTime.setVisibility(View.VISIBLE);

                    tvDate.setText(data.getString("date"));
                    tvTime.setText(data.getString("time"));
                    tvMessages.setText(data.getString("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (v1 < -300) {
            if(myDb.getDataCount()-1 > curVal) {
                try {
                    curVal += 1;
                    JSONObject data = myDb.getCurrentMsg(curVal);
                    Log.e("Data", String.valueOf(curVal));
                    Log.e("Data", String.valueOf(data));
                    tvDate.setVisibility(View.VISIBLE);
                    tvTime.setVisibility(View.VISIBLE);

                    tvDate.setText(data.getString("date"));
                    tvTime.setText(data.getString("time"));
                    tvMessages.setText(data.getString("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (v < -500) {
            try {
                JSONObject data = myDb.getCurrentMsg(curVal);
                myDb.deleteCurrentMsg(data.getInt("id"));
                if(curVal <= 0) {
                    curVal = 0;
                    if(myDb.getDataCount() == 0) {
                        tvDate.setVisibility(View.INVISIBLE);
                        tvTime.setVisibility(View.INVISIBLE);
                        tvMessages.setText("No Messages!");
                    } else {
                        JSONObject data1 = myDb.getCurrentMsg(curVal);
                        tvDate.setVisibility(View.VISIBLE);
                        tvTime.setVisibility(View.VISIBLE);

                        tvDate.setText(data1.getString("date"));
                        tvTime.setText(data1.getString("time"));
                        tvMessages.setText(data1.getString("message"));
                    }
                } else {
                    curVal -= 1;
                    JSONObject data1 = myDb.getCurrentMsg(curVal);
                    tvDate.setVisibility(View.VISIBLE);
                    tvTime.setVisibility(View.VISIBLE);

                    tvDate.setText(data1.getString("date"));
                    tvTime.setText(data1.getString("time"));
                    tvMessages.setText(data1.getString("message"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return false;
    }
}