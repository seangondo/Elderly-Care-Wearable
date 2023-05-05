package com.tugasakhir.elderlycarewearable;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class WatchDetails extends Activity {

    TextView watchId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_details);

        watchId = (TextView) findViewById(R.id.watch_id_details);

        Intent intent = getIntent();
        String watchID = intent.getStringExtra("watch_id");

        watchId.setText(watchID);

    }

    public void backToHome(View view) {
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
        finish();
    }
}