package com.nightscout.android.preferences;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
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
    public String displayLowRange() {
        return preferences.getString(PreferenceKeys.DISPLAY_LOW_RANGE, "80");
    }
    
    @Override
    public String displayHighRange() {
        return preferences.getString(PreferenceKeys.DISPLAY_HIGH_RANGE, "180");
    }
    
    @Override
    public boolean isLogarithmic() {
        return preferences.getBoolean(PreferenceKeys.DISPLAY_VERTICAL_AXIS, false);
    }

    @Override
    public boolean isDisplayOptionsMgdl() {
        return preferences.getBoolean(PreferenceKeys.DISPLAY_OPTIONS_UNITS, false);
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

    /**
     * Getter method to return the mongo device status collection
     *
     * @return MongoDB device status collection name
     */
    @Override
    public String getMongoDeviceStatusCollection() {
        return preferences.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION, getDefaultMongoDeviceStatusCollection());
    }

    /**
     * Enable mongo upload in shared preferences
     *
     * @param mongoUploadEnabled whether or not to upload directly to mongo
     */
    @SuppressLint("CommitPrefEdits")
    @Override
    public void setMongoUploadEnabled(boolean mongoUploadEnabled){
        preferences.edit().putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,mongoUploadEnabled).commit();
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setRestApiEnabled(boolean restApiEnabled){
        preferences.edit().putBoolean(PreferenceKeys.API_UPLOADER_ENABLED,restApiEnabled).commit();
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

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setMongoClientUri(String mongoClientUri) {
        preferences.edit().putString(PreferenceKeys.MONGO_URI, mongoClientUri).commit();
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setRestApiBaseUris(List<String> restApis) {
        preferences.edit().putString(PreferenceKeys.API_URIS, Joiner.on(' ').join(restApis)).commit();
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setMongoDeviceStatusCollection(String deviceStatusCollection) {
        preferences.edit().putString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION,deviceStatusCollection).commit();
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setMongoCollection(String sgvCollection) {
        preferences.edit().putString(PreferenceKeys.MONGO_COLLECTION,sgvCollection).commit();
    }
}
