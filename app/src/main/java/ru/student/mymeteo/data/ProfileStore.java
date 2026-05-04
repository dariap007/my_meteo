package ru.student.mymeteo.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import ru.student.mymeteo.domain.UserProfile;

public class ProfileStore {
    private final MyMeteoDatabase database;

    public ProfileStore(Context context) {
        database = new MyMeteoDatabase(context);
    }

    public UserProfile load() {
        UserProfile profile = new UserProfile();
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = db.query("profile", null, "id = 1", null, null, null, null);
        try {
            if (!cursor.moveToFirst()) {
                save(profile);
                return profile;
            }
            profile.hypertension = readBoolean(cursor, "hypertension");
            profile.hypotension = readBoolean(cursor, "hypotension");
            profile.migraine = readBoolean(cursor, "migraine");
            profile.joints = readBoolean(cursor, "joints");
            profile.magneticSensitive = readBoolean(cursor, "magnetic_sensitive");
            profile.notificationsEnabled = readBoolean(cursor, "notifications_enabled");
            profile.notifyPressure = readBoolean(cursor, "notify_pressure");
            profile.notifyTemperature = readBoolean(cursor, "notify_temperature");
            profile.notifyHumidity = readBoolean(cursor, "notify_humidity");
            profile.notifyGeomagnetic = readBoolean(cursor, "notify_geomagnetic");
            profile.notifyTomorrow = readBoolean(cursor, "notify_tomorrow");
            profile.disclaimerAccepted = readBoolean(cursor, "disclaimer_accepted");
            profile.ageGroup = readString(cursor, "age_group", "18-30");
            profile.city = readString(cursor, "city", "Москва");
            profile.latitude = readDouble(cursor, "latitude", 55.7558);
            profile.longitude = readDouble(cursor, "longitude", 37.6173);
            return profile;
        } finally {
            cursor.close();
        }
    }

    public void save(UserProfile profile) {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("hypertension", bool(profile.hypertension));
        values.put("hypotension", bool(profile.hypotension));
        values.put("migraine", bool(profile.migraine));
        values.put("joints", bool(profile.joints));
        values.put("magnetic_sensitive", bool(profile.magneticSensitive));
        values.put("notifications_enabled", bool(profile.notificationsEnabled));
        values.put("notify_pressure", bool(profile.notifyPressure));
        values.put("notify_temperature", bool(profile.notifyTemperature));
        values.put("notify_humidity", bool(profile.notifyHumidity));
        values.put("notify_geomagnetic", bool(profile.notifyGeomagnetic));
        values.put("notify_tomorrow", bool(profile.notifyTomorrow));
        values.put("disclaimer_accepted", bool(profile.disclaimerAccepted));
        values.put("age_group", profile.ageGroup);
        values.put("city", profile.city);
        values.put("latitude", profile.latitude);
        values.put("longitude", profile.longitude);
        db.replace("profile", null, values);
    }

    private int bool(boolean value) {
        return value ? 1 : 0;
    }

    private boolean readBoolean(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 && cursor.getInt(index) == 1;
    }

    private String readString(Cursor cursor, String column, String fallback) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return fallback;
        }
        return cursor.getString(index);
    }

    private double readDouble(Cursor cursor, String column, double fallback) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return fallback;
        }
        return cursor.getDouble(index);
    }
}
