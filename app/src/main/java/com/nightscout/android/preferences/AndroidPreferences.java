package com.nightscout.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.common.base.Joiner;
import com.nightscout.android.R;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.protobuf.G4Download;
import com.nightscout.core.utils.RestUriUtils;

import java.util.List;

public class AndroidPreferences implements NightscoutPreferences {
    private final SharedPreferences preferences;
    private Context context;

    public AndroidPreferences(Context context) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public AndroidPreferences(Context context, SharedPreferences prefs) {
        this.context = context;
        this.preferences = prefs;
    }


    @Override
    public boolean isRestApiEnabled() {
        return preferences.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED, false);
    }

    @Override
    public List<String> getRestApiBaseUris() {
        return RestUriUtils.splitIntoMultipleUris(preferences.getString(PreferenceKeys.API_URIS, ""));
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
        return preferences.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION, "devicestatus");
    }

    @Override
    public boolean isMqttEnabled() {
        return preferences.getBoolean(PreferenceKeys.MQTT_ENABLED, false);
    }

    @Override
    public String getMqttEndpoint() {
        return preferences.getString(PreferenceKeys.MQTT_ENDPOINT, "");
    }

    @Override
    public void setMqttEndpoint(String endpoint) {
        preferences.edit().putString(PreferenceKeys.MQTT_ENDPOINT, endpoint).apply();
    }

    @Override
    public String getMqttUser() {
        return preferences.getString(PreferenceKeys.MQTT_USER, "");
    }

    @Override
    public String getMqttPass() {
        return preferences.getString(PreferenceKeys.MQTT_PASS, "");
    }

    /**
     * Enable mongo upload in shared preferences
     *
     * @param mongoUploadEnabled whether or not to upload directly to mongo
     */
    @Override
    public void setMongoUploadEnabled(boolean mongoUploadEnabled) {
        preferences.edit().putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, mongoUploadEnabled).apply();
    }

    @Override
    public void setRestApiEnabled(boolean restApiEnabled) {
        preferences.edit().putBoolean(PreferenceKeys.API_UPLOADER_ENABLED, restApiEnabled).apply();
    }

    private String getDefaultMongoCollection() {
        return context.getString(R.string.pref_default_mongodb_collection);
    }

    private String getDefaultMongoDeviceStatusCollection() {
        return context.getString(R.string.pref_default_mongodb_device_status_collection);
    }

    @Override
    public G4Download.GlucoseUnit getPreferredUnits() {
        return preferences.getString(PreferenceKeys.PREFERRED_UNITS, "0").equals("0")
                ? G4Download.GlucoseUnit.MGDL : G4Download.GlucoseUnit.MMOL;
    }

    @Override
    public void setPreferredUnits(G4Download.GlucoseUnit units) {
        String unitString = (units == G4Download.GlucoseUnit.MGDL) ? "0" : "1";
        preferences.edit().putString(PreferenceKeys.PREFERRED_UNITS, unitString).apply();
    }

    @Override
    public String getPwdName() {
        return preferences.getString(PreferenceKeys.PWD_NAME, context.getString(R.string.default_pwd_name));
    }

    @Override
    public void setPwdName(String pwdName) {
        preferences.edit().putString(PreferenceKeys.PWD_NAME, pwdName).apply();
    }

    @Override
    public boolean hasAskedForData() {
        return preferences.getBoolean(PreferenceKeys.DONATE_DATA_QUERY, false);
    }

    @Override
    public void setAskedForData(boolean askedForData) {
        preferences.edit().putBoolean(PreferenceKeys.DONATE_DATA_QUERY, askedForData).apply();
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

    public boolean isRootEnabled() {
        return preferences.getBoolean(PreferenceKeys.ROOT_ENABLED, false);
    }

    public void setRootEnabled(boolean enabled) {
        preferences.edit().putBoolean(PreferenceKeys.ROOT_ENABLED, enabled).apply();
    }

    @SuppressLint("CommitPrefEdits")
    public void setLastEgvMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_EGV_TIME, timestamp).commit();
    }

    @SuppressLint("CommitPrefEdits")
    public void setLastSensorMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_SENSOR_TIME, timestamp).commit();
    }

    @SuppressLint("CommitPrefEdits")
    public void setLastCalMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_CAL_TIME, timestamp).commit();
    }

    @SuppressLint("CommitPrefEdits")
    public void setLastMeterMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_METER_TIME, timestamp).commit();
    }

    public long getLastEgvMqttUpload() {
        return preferences.getLong(PreferenceKeys.MQTT_LAST_EGV_TIME, 0);
    }

    public long getLastSensorMqttUpload() {
        return preferences.getLong(PreferenceKeys.MQTT_LAST_SENSOR_TIME, 0);
    }

    public long getLastCalMqttUpload() {
        return preferences.getLong(PreferenceKeys.MQTT_LAST_CAL_TIME, 0);
    }

    public long getLastMeterMqttUpload() {
        return preferences.getLong(PreferenceKeys.MQTT_LAST_METER_TIME, 0);
    }
}
