package ru.student.mymeteo.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class DiaryEntry {
    public final long createdAt;
    public final int wellbeing;
    public final String symptoms;
    public final String note;

    public DiaryEntry(long createdAt, int wellbeing, String symptoms, String note) {
        this.createdAt = createdAt;
        this.wellbeing = wellbeing;
        this.symptoms = symptoms;
        this.note = note;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("createdAt", createdAt);
        object.put("wellbeing", wellbeing);
        object.put("symptoms", symptoms);
        object.put("note", note);
        return object;
    }

    public static DiaryEntry fromJson(JSONObject object) {
        return new DiaryEntry(
                object.optLong("createdAt"),
                object.optInt("wellbeing"),
                object.optString("symptoms"),
                object.optString("note")
        );
    }
}
