package ru.student.mymeteo.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class DiaryEntry {
    public final long createdAt;
    public final int wellbeing;
    public final String symptoms;
    public final String note;
    public final double temperature;
    public final double pressure;
    public final double humidity;
    public final double kpIndex;
    public final int riskScore;
    public final String riskLevel;

    public DiaryEntry(
            long createdAt,
            int wellbeing,
            String symptoms,
            String note,
            double temperature,
            double pressure,
            double humidity,
            double kpIndex,
            int riskScore,
            String riskLevel
    ) {
        this.createdAt = createdAt;
        this.wellbeing = wellbeing;
        this.symptoms = symptoms;
        this.note = note;
        this.temperature = temperature;
        this.pressure = pressure;
        this.humidity = humidity;
        this.kpIndex = kpIndex;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("createdAt", createdAt);
        object.put("wellbeing", wellbeing);
        object.put("symptoms", symptoms);
        object.put("note", note);
        object.put("temperature", temperature);
        object.put("pressure", pressure);
        object.put("humidity", humidity);
        object.put("kpIndex", kpIndex);
        object.put("riskScore", riskScore);
        object.put("riskLevel", riskLevel);
        return object;
    }

    public static DiaryEntry fromJson(JSONObject object) {
        return new DiaryEntry(
                object.optLong("createdAt"),
                object.optInt("wellbeing"),
                object.optString("symptoms"),
                object.optString("note"),
                object.optDouble("temperature"),
                object.optDouble("pressure"),
                object.optDouble("humidity"),
                object.optDouble("kpIndex"),
                object.optInt("riskScore"),
                object.optString("riskLevel")
        );
    }
}
