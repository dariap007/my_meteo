package ru.student.mymeteo.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import ru.student.mymeteo.domain.DiaryEntry;

public class DiaryStore {
    private final MyMeteoDatabase database;

    public DiaryStore(Context context) {
        database = new MyMeteoDatabase(context);
    }

    public List<DiaryEntry> load() {
        List<DiaryEntry> entries = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = db.query(
                "diary_entries",
                null,
                null,
                null,
                null,
                null,
                "created_at DESC"
        );
        try {
            while (cursor.moveToNext()) {
                entries.add(new DiaryEntry(
                        readLong(cursor, "created_at"),
                        readInt(cursor, "wellbeing"),
                        readString(cursor, "symptoms"),
                        readString(cursor, "note"),
                        readDouble(cursor, "temperature"),
                        readDouble(cursor, "pressure"),
                        readDouble(cursor, "humidity"),
                        readDouble(cursor, "kp_index"),
                        readInt(cursor, "risk_score"),
                        readString(cursor, "risk_level")
                ));
            }
        } finally {
            cursor.close();
        }
        return entries;
    }

    public void add(DiaryEntry entry) {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("created_at", entry.createdAt);
        values.put("wellbeing", entry.wellbeing);
        values.put("symptoms", entry.symptoms);
        values.put("note", entry.note);
        values.put("temperature", entry.temperature);
        values.put("pressure", entry.pressure);
        values.put("humidity", entry.humidity);
        values.put("kp_index", entry.kpIndex);
        values.put("risk_score", entry.riskScore);
        values.put("risk_level", entry.riskLevel);
        db.insert("diary_entries", null, values);
    }

    private long readLong(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
    }

    private int readInt(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(column));
    }

    private double readDouble(Cursor cursor, String column) {
        return cursor.getDouble(cursor.getColumnIndexOrThrow(column));
    }

    private String readString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }
}
