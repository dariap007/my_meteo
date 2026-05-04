package ru.student.mymeteo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyMeteoDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mymeteo.db";
    private static final int DATABASE_VERSION = 1;

    public MyMeteoDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE profile (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1), " +
                "hypertension INTEGER NOT NULL DEFAULT 0, " +
                "hypotension INTEGER NOT NULL DEFAULT 0, " +
                "migraine INTEGER NOT NULL DEFAULT 0, " +
                "joints INTEGER NOT NULL DEFAULT 0, " +
                "magnetic_sensitive INTEGER NOT NULL DEFAULT 0, " +
                "notifications_enabled INTEGER NOT NULL DEFAULT 1, " +
                "notify_pressure INTEGER NOT NULL DEFAULT 1, " +
                "notify_temperature INTEGER NOT NULL DEFAULT 1, " +
                "notify_humidity INTEGER NOT NULL DEFAULT 1, " +
                "notify_geomagnetic INTEGER NOT NULL DEFAULT 1, " +
                "notify_tomorrow INTEGER NOT NULL DEFAULT 1, " +
                "disclaimer_accepted INTEGER NOT NULL DEFAULT 0, " +
                "age_group TEXT NOT NULL DEFAULT '18-30', " +
                "city TEXT NOT NULL DEFAULT 'Москва', " +
                "latitude REAL NOT NULL DEFAULT 55.7558, " +
                "longitude REAL NOT NULL DEFAULT 37.6173" +
                ")");

        db.execSQL("CREATE TABLE diary_entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "created_at INTEGER NOT NULL, " +
                "wellbeing INTEGER NOT NULL, " +
                "symptoms TEXT NOT NULL, " +
                "note TEXT NOT NULL, " +
                "temperature REAL NOT NULL, " +
                "pressure REAL NOT NULL, " +
                "humidity REAL NOT NULL, " +
                "kp_index REAL NOT NULL, " +
                "risk_score INTEGER NOT NULL, " +
                "risk_level TEXT NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS diary_entries");
        db.execSQL("DROP TABLE IF EXISTS profile");
        onCreate(db);
    }
}
