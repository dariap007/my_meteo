package ru.student.mymeteo.data;

import android.content.Context;
import android.content.SharedPreferences;

import ru.student.mymeteo.domain.UserProfile;

public class ProfileStore {
    private static final String NAME = "profile";
    private final SharedPreferences preferences;

    public ProfileStore(Context context) {
        preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public UserProfile load() {
        UserProfile profile = new UserProfile();
        profile.hypertension = preferences.getBoolean("hypertension", false);
        profile.hypotension = preferences.getBoolean("hypotension", false);
        profile.migraine = preferences.getBoolean("migraine", false);
        profile.joints = preferences.getBoolean("joints", false);
        profile.magneticSensitive = preferences.getBoolean("magneticSensitive", false);
        profile.notificationsEnabled = preferences.getBoolean("notificationsEnabled", true);
        profile.city = preferences.getString("city", "Москва");
        profile.latitude = Double.longBitsToDouble(preferences.getLong("latitude", Double.doubleToLongBits(55.7558)));
        profile.longitude = Double.longBitsToDouble(preferences.getLong("longitude", Double.doubleToLongBits(37.6173)));
        return profile;
    }

    public void save(UserProfile profile) {
        preferences.edit()
                .putBoolean("hypertension", profile.hypertension)
                .putBoolean("hypotension", profile.hypotension)
                .putBoolean("migraine", profile.migraine)
                .putBoolean("joints", profile.joints)
                .putBoolean("magneticSensitive", profile.magneticSensitive)
                .putBoolean("notificationsEnabled", profile.notificationsEnabled)
                .putString("city", profile.city)
                .putLong("latitude", Double.doubleToLongBits(profile.latitude))
                .putLong("longitude", Double.doubleToLongBits(profile.longitude))
                .apply();
    }
}
