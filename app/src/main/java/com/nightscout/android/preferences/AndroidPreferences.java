package com.nightscout.android.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.common.base.Joiner;
import com.nightscout.android.R;
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
//        String result = preferences.getString(PreferenceKeys.MONGO_COLLECTION,
//                getDefaultMongoCollection());
//        return result.equals("")?getDefaultMongoCollection():result;
        return preferences.getString(PreferenceKeys.MONGO_COLLECTION,
                getDefaultMongoCollection());
    }

    @Override
    public String getMongoDeviceStatusCollection() {
        String result = preferences.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION,
                getDefaultMongoDeviceStatusCollection());
        return result.equals("") ? getDefaultMongoDeviceStatusCollection() : result;
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
}
