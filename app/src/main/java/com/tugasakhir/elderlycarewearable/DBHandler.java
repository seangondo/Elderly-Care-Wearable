package com.tugasakhir.elderlycarewearable;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.StrictMode;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DBHandler extends SQLiteOpenHelper {
    static int DB_ver = 1;
    static String DB_name = "elderlyWatch_db";

    static String WATCH_TABLE = "watch_id";
    static String WATCH_id = "id";

    static String MSG_TABLE = "tb_message";
    static String MSG_id = "id";
    static String MSG_date = "date";
    static String MSG_time = "time";
    static String MSG_messages = "messages";

    static String CAL_TABLE = "tb_calories";
    static String CAL_id = "id";
    static String CAL_date = "date";
    static String CAL_Value = "value";


    static String STEPS_TABLE = "tb_steps";
    static String STEPS_id = "id";
    static String STEPS_date = "date";
    static String STEPS_Value = "value";

    public DBHandler (Context c) {
        super(c, DB_name, null, DB_ver);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String tbUser = "CREATE TABLE " + WATCH_TABLE + " ("
                + WATCH_id + " VARCHAR(50)); ";
        db.execSQL(tbUser);
        Log.e("Database", tbUser);

        String tbMsg = "CREATE TABLE " + MSG_TABLE + " ("
                + MSG_id + " INTEGER PRIMARY KEY, "
                + MSG_date + " VARCHAR(50), "
                + MSG_time + " VARCHAR(50), "
                + MSG_messages + " VARCHAR(50));";
        db.execSQL(tbMsg);
        Log.e("Database", tbMsg);


        String tbCal = "CREATE TABLE " + CAL_TABLE + " ("
                + CAL_date + " VARCHAR(50), "
                + CAL_Value + " VARCHAR(50));";
        db.execSQL(tbCal);
        Log.e("Database", tbCal);


        String tbSteps = "CREATE TABLE " + STEPS_TABLE + " ("
                + STEPS_date + " VARCHAR(50), "
                + STEPS_Value + " VARCHAR(50));";
        db.execSQL(tbSteps);
        Log.e("Database", tbSteps);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS "+WATCH_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+MSG_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+CAL_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+STEPS_TABLE);
        onCreate(db);
    }

    // WATCH ID
    public void insertWatchID(String watchid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(WATCH_id, watchid);
        db.insert(WATCH_TABLE, null, cv);
    }

    public String getWatchIdInfo() {
        SQLiteDatabase db = this.getReadableDatabase();
        String watchId = null;
        Cursor c = db.rawQuery("SELECT * FROM " + WATCH_TABLE, null);
        if(c.moveToFirst()) {
            do {
                watchId = c.getString(0);

            } while (c.moveToNext());
        }
        return watchId;
    }

    // WATCH MESSAGES
    public void insertMsgs(JSONObject messages) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        try {
            cv.put(MSG_date, messages.getString("date"));
            cv.put(MSG_time, messages.getString("time"));
            cv.put(MSG_messages, messages.getString("message"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        db.insert(MSG_TABLE, null, cv);
    }

    public int getDataCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + MSG_TABLE, null);
        int count = c.getCount();
        c.close();
        return count;
    }

    public JSONObject getCurrentMsg (int number) throws JSONException{
        SQLiteDatabase db = this.getReadableDatabase();
        JSONObject dataSend = new JSONObject();
        JSONArray arrData = new JSONArray();
        Cursor c = db.rawQuery("SELECT * FROM " + MSG_TABLE, null);

        if(c.moveToFirst()) {
            do {
                JSONObject data = new JSONObject();
                data.put("id", c.getInt(0));
                data.put("date", c.getString(1));
                data.put("time", c.getString(2));
                data.put("message", c.getString(3));
                arrData.put(data);
            } while (c.moveToNext());
        }
        c.close();
        Log.e("Data List", String.valueOf(arrData));
        dataSend = arrData.getJSONObject(number);
        return dataSend;
    }

    public void deleteCurrentMsg (int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MSG_TABLE, MSG_id + " = " + id, null);

    }

    // --------------------------------------------------- < WATCH STEPS > --------------------------------------------------- //
    public void insertSteps(String value, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(STEPS_date, date);
        cv.put(STEPS_Value, value);
        db.insert(STEPS_TABLE, null, cv);
    }

    public void updateSteps(String value, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(STEPS_Value, value);
        db.update(STEPS_TABLE, cv, STEPS_date + " = '" + date + "'", null);
    }

    public int getCountStep(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + STEPS_TABLE + " WHERE date='" + date + "'", null);
        int count = c.getCount();
        c.close();
        return count;
    }

    public String getSteps(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String steps = "0";
        @SuppressLint("Recycle") Cursor c = db.rawQuery("SELECT * FROM " + STEPS_TABLE + " WHERE date='" + date + "'", null);

        if(c.moveToFirst()) {
            do {
                steps = c.getString(1);
            } while (c.moveToNext());
        }
        c.close();
        return steps;
    }

    public void deleteSteps(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(STEPS_TABLE, STEPS_date + " = " + date, null);
    }


    // --------------------------------------------------- < WATCH CALORIES > --------------------------------------------------- //
    public void insertCal(String value, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(CAL_date, date);
        cv.put(CAL_Value, value);
        db.insert(CAL_TABLE, null, cv);
    }

    public void updateCal(String value, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(CAL_Value, value);
        db.update(CAL_TABLE, cv, CAL_date + " = '" + date + "'", null);

    }

    public int getCountCal(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor c = db.rawQuery("SELECT * FROM " + CAL_TABLE + " WHERE date='" + date + "'", null);
        int count = c.getCount();
        c.close();
        return count;
    }

    public String getCal(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String steps = "0";
        @SuppressLint("Recycle") Cursor c = db.rawQuery("SELECT * FROM " + CAL_TABLE + " WHERE date='" + date + "'", null);

        if(c.moveToFirst()) {
            do {
                steps = c.getString(1);
            } while (c.moveToNext());
        }
        c.close();
        return steps;
    }

    public void deleteCal(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(CAL_TABLE, CAL_date + " = " + date, null);
    }
}
