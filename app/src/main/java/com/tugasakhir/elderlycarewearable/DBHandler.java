package com.tugasakhir.elderlycarewearable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS "+WATCH_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+MSG_TABLE);
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

    //WATCH MESSAGES
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

        return c.getCount();
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
        Log.e("Data List", String.valueOf(arrData));
        dataSend = arrData.getJSONObject(number);
        return dataSend;
    }

    public void deleteCurrentMsg (int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MSG_TABLE, MSG_id + " = " + id, null);

    }
}
