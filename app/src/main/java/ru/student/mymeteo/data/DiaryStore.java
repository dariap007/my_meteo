package ru.student.mymeteo.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.student.mymeteo.domain.DiaryEntry;

public class DiaryStore {
    private static final String NAME = "diary";
    private static final String KEY_ENTRIES = "entries";
    private final SharedPreferences preferences;

    public DiaryStore(Context context) {
        preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public List<DiaryEntry> load() {
        List<DiaryEntry> entries = new ArrayList<>();
        JSONArray array = new JSONArray();
        try {
            array = new JSONArray(preferences.getString(KEY_ENTRIES, "[]"));
        } catch (Exception ignored) {
        }
        for (int index = 0; index < array.length(); index++) {
            if (array.optJSONObject(index) != null) {
                entries.add(DiaryEntry.fromJson(array.optJSONObject(index)));
            }
        }
        Collections.reverse(entries);
        return entries;
    }

    public void add(DiaryEntry entry) {
        try {
            JSONArray array = new JSONArray(preferences.getString(KEY_ENTRIES, "[]"));
            array.put(entry.toJson());
            preferences.edit().putString(KEY_ENTRIES, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }
}
