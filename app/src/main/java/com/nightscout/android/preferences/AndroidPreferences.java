package com.nightscout.android.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nightscout.android.R;
import com.nightscout.core.drivers.SupportedDevices;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.RestUriUtils;

import net.tribe7.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class AndroidPreferences implements NightscoutPreferences {
    protected final Logger log = LoggerFactory.getLogger(AndroidPreferences.class);

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
        return preferences.getBoolean(context.getString(R.string.rest_enable), false);
    }

    @Override
    public List<String> getRestApiBaseUris() {
        return RestUriUtils.splitIntoMultipleUris(preferences.getString(context.getString(R.string.rest_uris), ""));
    }

    @Override
    public boolean isCalibrationUploadEnabled() {
        return preferences.getBoolean(context.getString(R.string.enable_raw_upload), false);
    }

    @Override
    public void setCalibrationUploadEnabled(boolean calibrationUploadEnabled) {
        preferences.edit().putBoolean(context.getString(R.string.enable_raw_upload), calibrationUploadEnabled).apply();
    }

    @Override
    public boolean isSensorUploadEnabled() {
        return preferences.getBoolean(context.getString(R.string.enable_raw_upload), false);
    }

    @Override
    public void setSensorUploadEnabled(boolean sensorUploadEnabled) {
        preferences.edit().putBoolean(context.getString(R.string.enable_raw_upload), sensorUploadEnabled).apply();
    }

    @Override
    public boolean isRawEnabled() {
        return preferences.getBoolean(context.getString(R.string.enable_raw_upload), false);
    }

    @Override
    public void setRawEnabled(boolean rawUploadEnabled) {
        preferences.edit().putBoolean(context.getString(R.string.enable_raw_upload), rawUploadEnabled).apply();
    }

    @Override
    public boolean isDataDonateEnabled() {
        return preferences.getBoolean(context.getString(R.string.data_donate), false);
    }

    @Override
    public boolean isMongoUploadEnabled() {
        return preferences.getBoolean(context.getString(R.string.mongo_enable), false);
    }

    @Override
    public void setDataDonateEnabled(boolean toDonate) {
        preferences.edit().putBoolean(context.getString(R.string.data_donate), toDonate).apply();
    }

    @Override
    public String getMongoClientUri() {
        return preferences.getString(context.getString(R.string.mongo_uri), "");
    }

    @Override
    public String getMongoCollection() {
        return preferences.getString(context.getString(R.string.mongo_entries_collection),
                getDefaultMongoCollection());
    }

    @Override
    public String getMongoDeviceStatusCollection() {
        String result = preferences.getString(context.getString(R.string.mongo_devicestatus_collection),
                getDefaultMongoDeviceStatusCollection());
        return result.equals("") ? getDefaultMongoDeviceStatusCollection() : result;
    }

    @Override
    public boolean isMqttEnabled() {
        return preferences.getBoolean(context.getString(R.string.mqtt_enable), false);
    }

    @Override
    public void setMqttUploadEnabled(boolean mqttUploadEnabled) {
        preferences.edit().putBoolean(context.getString(R.string.mqtt_enable), mqttUploadEnabled).apply();
    }

    @Override
    public String getMqttEndpoint() {
        return preferences.getString(context.getString(R.string.mqtt_endpoint), "");
    }

    @Override
    public void setMqttEndpoint(String endpoint) {
        preferences.edit().putString(context.getString(R.string.mqtt_endpoint), endpoint).apply();
    }

    @Override
    public String getMqttUser() {
        return preferences.getString(context.getString(R.string.mqtt_user), "");
    }

    @Override
    public String getMqttPass() {
        return preferences.getString(context.getString(R.string.mqtt_pass), "");
    }

    public void setMqttUser(String mqttUser) {
        preferences.edit().putString(context.getString(R.string.mqtt_user), mqttUser).apply();
    }

    // TODO: (klee) look into how to securely store this information
    public void setMqttPass(String mqttPass) {
        preferences.edit().putString(context.getString(R.string.mqtt_pass), mqttPass).apply();
    }


    /**
     * Enable mongo upload in shared preferences
     *
     * @param mongoUploadEnabled whether or not to upload directly to mongo
     */
    @Override
    public void setMongoUploadEnabled(boolean mongoUploadEnabled) {
        preferences.edit().putBoolean(context.getString(R.string.mongo_enable), mongoUploadEnabled).apply();
    }

    @Override
    public void setRestApiEnabled(boolean restApiEnabled) {
        preferences.edit().putBoolean(context.getString(R.string.rest_enable), restApiEnabled).apply();
    }

    private String getDefaultMongoCollection() {
        return context.getString(R.string.pref_default_mongodb_collection);
    }

    private String getDefaultMongoDeviceStatusCollection() {
        return context.getString(R.string.pref_default_mongodb_device_status_collection);
    }

    @Override
    public GlucoseUnit getPreferredUnits() {
        return preferences.getString(context.getString(R.string.preferred_units), "0").equals("0")
                ? GlucoseUnit.MGDL : GlucoseUnit.MMOL;
    }

    @Override
    public void setPreferredUnits(GlucoseUnit units) {
        String unitString = (units == GlucoseUnit.MGDL) ? "0" : "1";
        preferences.edit().putString(context.getString(R.string.preferred_units), unitString).apply();
    }

    @Override
    public String getPwdName() {
        return preferences.getString(context.getString(R.string.pwd_name), context.getString(R.string.default_pwd_name));
    }

    @Override
    public void setPwdName(String pwdName) {
        preferences.edit().putString(context.getString(R.string.pwd_name), pwdName).apply();
    }

    @Override
    public boolean hasAskedForData() {
        return preferences.getBoolean(context.getString(R.string.donate_data_query), false);
    }

    @Override
    public void setAskedForData(boolean askedForData) {
        preferences.edit().putBoolean(context.getString(R.string.donate_data_query), askedForData).apply();
    }

    @Override
    public void setMongoClientUri(String mongoClientUri) {
        preferences.edit().putString(context.getString(R.string.mongo_uri), mongoClientUri).apply();
    }

    @Override
    public void setMongoDeviceStatusCollection(String deviceStatusCollection) {
        preferences.edit().putString(context.getString(R.string.mongo_devicestatus_collection), deviceStatusCollection).apply();
    }

    @Override
    public void setMongoCollection(String sgvCollection) {
        preferences.edit().putString(context.getString(R.string.mongo_entries_collection), sgvCollection).apply();
    }

    @Override
    public boolean getIUnderstand() {
        return preferences.getBoolean(context.getString(R.string.i_understand), false);
    }

    @Override
    public void setIUnderstand(boolean bool) {
        preferences.edit().putBoolean(context.getString(R.string.i_understand), bool).apply();
    }

    @Override
    public void setRestApiBaseUris(List<String> uris) {
        preferences.edit().putString(context.getString(R.string.rest_uris), Joiner.on(' ').join(uris)).apply();
    }

    public boolean isRootEnabled() {
        return preferences.getBoolean(context.getString(R.string.root_enable), false);
    }

    public void setRootEnabled(boolean enabled) {
        preferences.edit().putBoolean(context.getString(R.string.root_enable), enabled).apply();
    }

    @Override
    public void setLastEgvMqttUpload(long timestamp) {
        preferences.edit().putLong(context.getString(R.string.last_mqtt_egv_time), timestamp).apply();
    }

    @Override
    public void setLastSensorMqttUpload(long timestamp) {
        preferences.edit().putLong(context.getString(R.string.last_mqtt_sensor_time), timestamp).apply();
    }

    @Override
    public void setLastCalMqttUpload(long timestamp) {
        preferences.edit().putLong(context.getString(R.string.last_mqtt_cal_time), timestamp).apply();
    }

    @Override
    public void setLastMeterMqttUpload(long timestamp) {
        preferences.edit().putLong(context.getString(R.string.last_mqtt_meter_time), timestamp).apply();
    }

    @Override
    public void setLastInsMqttUpload(long timestamp) {
        preferences.edit().putLong(context.getString(R.string.last_mqtt_ins_time), timestamp).apply();
    }

    @Override
    public long getLastEgvMqttUpload() {
        return preferences.getLong(context.getString(R.string.last_mqtt_egv_time), 0);
    }

    @Override
    public long getLastSensorMqttUpload() {
        return preferences.getLong(context.getString(R.string.last_mqtt_sensor_time), 0);
    }

    @Override
    public long getLastCalMqttUpload() {
        return preferences.getLong(context.getString(R.string.last_mqtt_cal_time), 0);
    }

    @Override
    public long getLastMeterMqttUpload() {
        return preferences.getLong(context.getString(R.string.last_mqtt_meter_time), 0);
    }

    @Override
    public long getLastInsMqttUpload() {
        return preferences.getLong(context.getString(R.string.last_mqtt_ins_time), 0);
    }

    @Override
    public void setLastEgvBaseUpload(long timestamp, String postfix) {
        preferences.edit().putLong(context.getString(R.string.last_uploader_egv_time_prefix) + postfix, timestamp).apply();
    }

    @Override
    public void setLastSensorBaseUpload(long timestamp, String postfix) {
        preferences.edit().putLong(context.getString(R.string.last_uploader_sensor_time_prefix) + postfix, timestamp).apply();
    }

    @Override
    public void setLastCalBaseUpload(long timestamp, String postfix) {
        preferences.edit().putLong(context.getString(R.string.last_uploader_cal_time_prefix) + postfix, timestamp).apply();
    }

    @Override
    public void setLastMeterBaseUpload(long timestamp, String postfix) {
        preferences.edit().putLong(context.getString(R.string.last_uploader_meter_time_prefix) + postfix, timestamp).apply();
    }

    @Override
    public void setLastInsBaseUpload(long timestamp, String postfix) {
        preferences.edit().putLong(context.getString(R.string.last_uploader_ins_time_prefix) + postfix, timestamp).apply();
    }

    @Override
    public long getLastEgvBaseUpload(String postfix) {
        return preferences.getLong(context.getString(R.string.last_uploader_egv_time_prefix) + postfix, 0);
    }

    @Override
    public long getLastSensorBaseUpload(String postfix) {
        return preferences.getLong(context.getString(R.string.last_uploader_sensor_time_prefix) + postfix, 0);
    }

    @Override
    public long getLastCalBaseUpload(String postfix) {
        return preferences.getLong(context.getString(R.string.last_uploader_cal_time_prefix) + postfix, 0);
    }

    @Override
    public long getLastMeterBaseUpload(String postfix) {
        return preferences.getLong(context.getString(R.string.last_uploader_meter_time_prefix) + postfix, 0);
    }

    @Override
    public long getLastInsBaseUpload(String postfix) {
        return preferences.getLong(context.getString(R.string.last_uploader_ins_time_prefix) + postfix, 0);
    }

    public long getLastRecordTime(String recType, String uploadType) {
        String key = "last_" + uploadType + "_" + recType + "_time";
        return preferences.getLong(key, 0);
    }

    public void setLastRecordTime(String recType, String uploadType, long timestamp) {
        String key = "last_" + uploadType + "_" + recType + "_time";
        preferences.edit().putLong(key, timestamp).apply();
    }

    @Override
    public void setBluetoothDevice(String btDeviceName, String btAddress) {
        preferences.edit().putString(context.getString(R.string.share_bluetooth_device), btDeviceName).apply();
        preferences.edit().putString(context.getString(R.string.share_bluetooth_address), btAddress).apply();
    }

    @Override
    public String getBtAddress() {
        return preferences.getString(context.getString(R.string.share_bluetooth_address), "");
    }

    @Override
    public SupportedDevices getDeviceType() {
        String value = preferences.getString(context.getString(R.string.dexcom_device_type), "0");
        if (value.equals("0")) {
            return SupportedDevices.DEXCOM_G4;
        }
        if (value.equals("1")) {
            return SupportedDevices.DEXCOM_G4_SHARE2;
        }
        return SupportedDevices.UNKNOWN;
    }

    @Override
    public String getShareSerial() {
        return preferences.getString(context.getString(R.string.share2_serial), "");
    }

    @Override
    public void setShareSerial(String serialNumber) {
        preferences.edit().putString(context.getString(R.string.share2_serial), serialNumber).apply();
    }

    @Override
    public boolean isMeterUploadEnabled() {
        return preferences.getBoolean(context.getString(R.string.cloud_mbg_data), false);
    }

    @Override
    public void setMeterUploadEnabled(boolean enabled) {
        preferences.edit().putBoolean(context.getString(R.string.cloud_mbg_data), enabled).apply();
    }

    @Override
    public boolean isInsertionUploadEnabled() {
//        return preferences.getBoolean(PreferenceKeys.INSERTION_UPLOAD_ENABLED, false);
        return preferences.getBoolean(context.getString(R.string.insert_data_enabled), false);
    }

    @Override
    public void setInsertionUploadEnabled(boolean enabled) {
        preferences.edit().putBoolean(context.getString(R.string.insert_data_enabled), enabled).apply();
    }

    public void setLabsEnabled(boolean enabled) {
        preferences.edit().putBoolean(context.getString(R.string.labs_enable), enabled).apply();
    }

    public boolean areLabsEnabled() {
        return preferences.getBoolean(context.getString(R.string.labs_enable), false);
    }

    public boolean isCampingModeEnabled() {
        return preferences.getBoolean(context.getString(R.string.camping_enable), false);
    }

    @Override
    public long getLastMeterSysTime() {
        return preferences.getLong(context.getString(R.string.last_download_meter_sys_time), 0);
    }

    @Override
    public void setLastMeterSysTime(long meterSysTime) {
        preferences.edit().putLong(context.getString(R.string.last_download_meter_sys_time), meterSysTime).apply();
    }

    @Override
    public long getLastEgvSysTime() {
        return preferences.getLong(context.getString(R.string.last_download_egv_sys_time), 0);
    }

    @Override
    public void setLastEgvSysTime(long egvSysTime) {
        preferences.edit().putLong(context.getString(R.string.last_download_egv_sys_time), egvSysTime).apply();
    }

    public void deleteKey(String key) {
        preferences.edit().remove(key).apply();
    }

    public boolean saveSharedPreferencesToFile(File dst) {
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences pref =
                    PreferenceManager.getDefaultSharedPreferences(context);
            output.writeObject(pref.getAll());

            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }

    @SuppressWarnings({"unchecked"})
    public boolean loadSharedPreferencesFromFile(File src) {
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
//            prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                    log.debug("Restoring {}: {}", key, ((Boolean) v).booleanValue());
                } else if (v instanceof Float) {
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                    log.debug("Restoring {}: {}", key, ((Float) v).floatValue());
                } else if (v instanceof Integer) {
                    prefEdit.putInt(key, ((Integer) v).intValue());
                    log.debug("Restoring {}: {}", key, ((Integer) v).intValue());
                } else if (v instanceof Long) {
                    prefEdit.putLong(key, ((Long) v).longValue());
                    log.debug("Restoring {}: {}", key, ((Long) v).longValue());
                } else if (v instanceof String) {
                    prefEdit.putString(key, ((String) v));
                    log.debug("Restoring {}: {}", key, v);
                }
            }
            prefEdit.apply();
            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }

}
