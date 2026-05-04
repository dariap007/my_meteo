package ru.student.mymeteo.domain;

public class UserProfile {
    public boolean hypertension;
    public boolean hypotension;
    public boolean migraine;
    public boolean joints;
    public boolean magneticSensitive;
    public boolean notificationsEnabled;
    public boolean notifyPressure;
    public boolean notifyTemperature;
    public boolean notifyHumidity;
    public boolean notifyGeomagnetic;
    public boolean notifyTomorrow;
    public boolean disclaimerAccepted;
    public String ageGroup;
    public double latitude;
    public double longitude;
    public String city;

    public UserProfile() {
        notificationsEnabled = true;
        notifyPressure = true;
        notifyTemperature = true;
        notifyHumidity = true;
        notifyGeomagnetic = true;
        notifyTomorrow = true;
        disclaimerAccepted = false;
        ageGroup = "18-30";
        latitude = 55.7558;
        longitude = 37.6173;
        city = "Москва";
    }

    public int riskWeightBonus() {
        int bonus = 0;
        if (hypertension || hypotension) {
            bonus += 8;
        }
        if (migraine) {
            bonus += 6;
        }
        if (joints) {
            bonus += 5;
        }
        if (magneticSensitive) {
            bonus += 6;
        }
        return bonus;
    }
}
