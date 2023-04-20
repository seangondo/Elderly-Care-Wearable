package com.tugasakhir.elderlycarewearable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONObject;

public class DBHandler extends SQLiteOpenHelper {

    static int DB_ver = 1;
    static String DB_name = "elderlyWatch_db";

    static String WATCH_TABLE = "watch_id";
    static String WATCH_id = "id";

    public DBHandler (Context c) {
        super(c, DB_name, null, DB_ver);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String tbUser = "CREATE TABLE " + WATCH_TABLE + " ("
                + WATCH_id + " VARCHAR(50)); ";
        db.execSQL(tbUser);
        Log.e("Database", tbUser);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS "+WATCH_TABLE);
        onCreate(db);
    }

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
}
