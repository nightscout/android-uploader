package com.nightscout.android.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Joiner;
import com.nightscout.android.R;
import com.nightscout.core.drivers.SupportedDevices;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.preferences.NightscoutPreferences;
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
        return preferences.getString(PreferenceKeys.MONGO_COLLECTION,
                getDefaultMongoCollection());
    }

    @Override
    public String getMongoDeviceStatusCollection() {
        String result = preferences.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION,
                getDefaultMongoDeviceStatusCollection());
        return result.equals("") ? getDefaultMongoDeviceStatusCollection() : result;
    }

    @Override
    public boolean isMqttEnabled() {
        return preferences.getBoolean(PreferenceKeys.MQTT_ENABLED, false);
    }

    @Override
    public void setMqttUploadEnabled(boolean mqttUploadEnabled) {
        preferences.edit().putBoolean(PreferenceKeys.MQTT_ENABLED, mqttUploadEnabled).apply();
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

    public void setMqttUser(String mqttUser) {
        preferences.edit().putString(PreferenceKeys.MQTT_USER, mqttUser).apply();
    }

    // TODO: (klee) look into how to securely store this information
    public void setMqttPass(String mqttPass) {
        preferences.edit().putString(PreferenceKeys.MQTT_PASS, mqttPass).apply();
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
    public GlucoseUnit getPreferredUnits() {
        return preferences.getString(PreferenceKeys.PREFERRED_UNITS, "0").equals("0")
                ? GlucoseUnit.MGDL : GlucoseUnit.MMOL;
    }

    @Override
    public void setPreferredUnits(GlucoseUnit units) {
        String unitString = (units == GlucoseUnit.MGDL) ? "0" : "1";
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

    public void setLastEgvMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_EGV_TIME, timestamp).apply();
    }

    public void setLastSensorMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_SENSOR_TIME, timestamp).apply();
    }

    public void setLastCalMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_CAL_TIME, timestamp).apply();
    }

    public void setLastMeterMqttUpload(long timestamp) {
        preferences.edit().putLong(PreferenceKeys.MQTT_LAST_METER_TIME, timestamp).apply();
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

    @Override
    public void setBluetoothDevice(String btDeviceName, String btAddress) {
        preferences.edit().putString(PreferenceKeys.BLUETOOTH_DEVICE, btDeviceName).apply();
        preferences.edit().putString(PreferenceKeys.BLUETOOTH_ADDRESS, btAddress).apply();
    }

    @Override
    public String getBtAddress() {
        return preferences.getString(PreferenceKeys.BLUETOOTH_ADDRESS, "");
    }

    @Override
    public SupportedDevices getDeviceType() {
        String value = preferences.getString(PreferenceKeys.DEXCOM_DEVICE_TYPE, "0");
        if (value.equals("0")){
            return SupportedDevices.DEXCOM_G4;
        }
        if (value.equals("1")){
            return SupportedDevices.DEXCOM_G4_SHARE2;
        }
        return SupportedDevices.UNKNOWN;
//        return preferences.getString(PreferenceKeys.DEXCOM_DEVICE_TYPE, "0").equals("0") ?
//                SupportedDevices.DEXCOM_G4 : SupportedDevices.DEXCOM_G4_SHARE2;
    }

    @Override
    public String getShareSerial() {
        return preferences.getString(PreferenceKeys.SHARE_SERIAL, "");
    }

    @Override
    public void setShareSerial(String serialNumber) {
        preferences.edit().putString(PreferenceKeys.SHARE_SERIAL, serialNumber).apply();
    }

    @Override
    public boolean isMeterUploadEnabled() {
        return preferences.getBoolean(PreferenceKeys.METER_UPLOAD_ENABLED, false);
    }

    @Override
    public void setMeterUploadEnabled(boolean enabled) {
        preferences.edit().putBoolean(PreferenceKeys.METER_UPLOAD_ENABLED, enabled).apply();
    }

    public void setLabsEnabled(boolean enabled) {
        preferences.edit().putBoolean(PreferenceKeys.LABS_ENABLED, enabled).apply();
    }

    public boolean areLabsEnabled() {
        return preferences.getBoolean(PreferenceKeys.LABS_ENABLED, false);

    }

}
