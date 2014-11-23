package com.nightscout.android.preferences;

import android.content.SharedPreferences;
import android.util.Log;

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
    public boolean isMongoUploadEnabled() {
        return preferences.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, false);
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

    @Override
    public void setMongoClientUri(String mongoClientUri) {
        preferences.edit().putString(PreferenceKeys.MONGO_URI,mongoClientUri).apply();
    }

    @Override
    public void setRestApiBaseUris(List<String> restApis) {
        StringBuilder sb = new StringBuilder();
        for (String restEndpoint:restApis){
            sb.append(restEndpoint);
            sb.append(" ");
        }
        Log.d("XXX","Rest String: "+sb.toString()+"|  "+restApis.size());
        preferences.edit().putString(PreferenceKeys.API_URIS,sb.toString().trim()).apply();
    }

    @Override
    public void setMongoDeviceStatusCollection(String deviceStatusCollection) {
        preferences.edit().putString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION,deviceStatusCollection).apply();
    }

    @Override
    public void setMongoCollection(String sgvCollection) {
        preferences.edit().putString(PreferenceKeys.MONGO_COLLECTION,sgvCollection).apply();
    }

    @Override
    public void setMongoUploadEnabled(boolean mongoUploadEnabled){
        preferences.edit().putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,mongoUploadEnabled).apply();
    }

    @Override
    public void setRestApiEnabled(boolean restApiEnabled){
        preferences.edit().putBoolean(PreferenceKeys.API_UPLOADER_ENABLED,restApiEnabled).apply();
    }

}
