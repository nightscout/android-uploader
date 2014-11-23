package com.nightscout.android.preferences;

import android.content.SharedPreferences;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.common.collect.Lists;
import com.nightscout.android.Nightscout;
import com.nightscout.core.preferences.NightscoutPreferences;

import java.util.List;

public class AndroidPreferences implements NightscoutPreferences {
    private final SharedPreferences preferences;

    public AndroidPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
    }
    @Override
    public boolean isRestApiEnabled() {
        return preferences.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED, false);
    }

    @Override
    public List<String> getRestApiBaseUris() {
        return Lists.newArrayList(preferences.getString(PreferenceKeys.API_URIS, "").split(" "));
    }

    @Override
    public boolean isCalibrationUploadEnabled() {
        return preferences.getBoolean(PreferenceKeys.CAL_UPLOAD_ENABLED, false);
    }

    @Override
    public boolean isSensorUploadEnabled() {
        return preferences.getBoolean(PreferenceKeys.SENSOR_UPLOAD_ENABLED, false);
    }

    @Override
    public boolean isDataDonateEnabled() {
        return preferences.getBoolean(PreferenceKeys.DATA_DONATE, false);
    }

    @Override
    public boolean isMongoUploadEnabled() {
        return preferences.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, false);
    }

    @Override
    public void setDataDonateEnabled(boolean toDonate) {
        preferences.edit().putBoolean(PreferenceKeys.DATA_DONATE, toDonate).apply();
    }

    @Override
    public String getMongoClientUri() {
        return preferences.getString(PreferenceKeys.MONGO_URI, "");
    }

    @Override
    public String getMongoCollection() {
        return preferences.getString(PreferenceKeys.MONGO_COLLECTION, "dexcom");
    }

    @Override
    public String getMongoDeviceStatusCollection() {
        return preferences.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION, "devicestatus");
    }
}
