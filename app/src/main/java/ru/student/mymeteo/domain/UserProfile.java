package ru.student.mymeteo.domain;

public class UserProfile {
    public boolean hypertension;
    public boolean hypotension;
    public boolean migraine;
    public boolean joints;
    public boolean magneticSensitive;
    public boolean notificationsEnabled;
    public double latitude;
    public double longitude;
    public String city;

    public UserProfile() {
        notificationsEnabled = true;
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
