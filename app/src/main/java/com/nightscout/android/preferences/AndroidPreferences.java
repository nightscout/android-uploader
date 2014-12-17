package com.nightscout.android.preferences;

import android.content.SharedPreferences;

import com.google.common.base.Joiner;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.RestUrlUtils;

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
        return RestUrlUtils.splitIntoMultipleUris(preferences.getString(PreferenceKeys.API_URIS, ""));
    }

    @Override
    public boolean isCalibrationUploadEnabled() {
        return preferences.getBoolean(PreferenceKeys.CAL_UPLOAD_ENABLED, false);
    }

    @Override
    public void setCalibrationUploadEnabled(boolean calibrationUploadEnabled) {
        preferences.edit().putBoolean(PreferenceKeys.CAL_UPLOAD_ENABLED, calibrationUploadEnabled).apply();
    }

    @Override
    public boolean isSensorUploadEnabled() {
        return preferences.getBoolean(PreferenceKeys.SENSOR_UPLOAD_ENABLED, false);
    }

    @Override
    public void setSensorUploadEnabled(boolean sensorUploadEnabled) {
        preferences.edit().putBoolean(PreferenceKeys.SENSOR_UPLOAD_ENABLED, sensorUploadEnabled).apply();
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
        return preferences.getString(PreferenceKeys.MONGO_COLLECTION, getDefaultMongoCollection());
    }

    @Override
    public String getMongoDeviceStatusCollection() {
        return preferences.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION, getDefaultMongoDeviceStatusCollection());
    }

    @Override
    public void setMongoUploadEnabled(boolean mongoUploadEnabled){
        preferences.edit().putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,mongoUploadEnabled).apply();
    }

    @Override
    public void setRestApiEnabled(boolean restApiEnabled){
        preferences.edit().putBoolean(PreferenceKeys.API_UPLOADER_ENABLED,restApiEnabled).apply();
    }

    // Can't get Robolectric to read from resources
    @Override
    public String getDefaultMongoCollection() {
        return DEFAULT_MONGO_COLLECTION;
    }

    // Can't get Robolectric to read from resources
    @Override
    public String getDefaultMongoDeviceStatusCollection() {
        return DEFAULT_MONGO_DEVICE_STATUS_COLLECTION;
    }

    @Override
    public void setMongoClientUri(String mongoClientUri) {
        preferences.edit().putString(PreferenceKeys.MONGO_URI, mongoClientUri).apply();
    }

    @Override
    public void setMongoDeviceStatusCollection(String deviceStatusCollection) {
        preferences.edit().putString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION, deviceStatusCollection).apply();
    }

    @Override
    public void setMongoCollection(String sgvCollection) {
        preferences.edit().putString(PreferenceKeys.MONGO_COLLECTION, sgvCollection).apply();
    }

    @Override
    public boolean getIUnderstand() {
        return preferences.getBoolean(PreferenceKeys.I_UNDERSTAND, false);
    }

    @Override
    public void setIUnderstand(boolean bool) {
        preferences.edit().putBoolean(PreferenceKeys.I_UNDERSTAND, bool).apply();
    }

    @Override
    public void setRestApiBaseUris(List<String> uris) {
        preferences.edit().putString(PreferenceKeys.API_URIS, Joiner.on(' ').join(uris)).apply();
    }
}
