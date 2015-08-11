package com.nightscout.core.preferences;

import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.model.GlucoseUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestPreferences implements NightscoutPreferences {
    private boolean restApiEnabled = false;
    private List<String> restApiBaseUris = new ArrayList<>();
    private boolean calibrationUploadEnabled = false;
    private boolean sensorUploadEnabled = false;
    private boolean rawUploadEnabled = false;
    private boolean mongoUploadEnabled = false;
    private boolean mqttUploadEnabled = false;
    private String mongoClientUri = null;
    private String mongoCollection = "entries";
    private String mongoDeviceStatusCollection = "devicestatus";
    private boolean dataDonateEnabled;
    private GlucoseUnit units;
    private String pwdName;
    private boolean understand;
    private boolean askedForData;
    private String mqttEndpoint;
    private String serialNumber;
    private boolean mgbUploadEnabled = false;
    private String btAddress = null;
    private String btDeviceName;
    private HashMap<String, HashMap<String, Long>> lastUpload = new HashMap<>();
    private DeviceType deviceType = DeviceType.UNKNOWN;
    private boolean insertionEnabled;

    @Override
    public boolean isRestApiEnabled() {
        return restApiEnabled;
    }

    @Override
    public void setRestApiEnabled(boolean restApiEnabled) {
        this.restApiEnabled = restApiEnabled;
    }

    @Override
    public GlucoseUnit getPreferredUnits() {
        return units;
    }

    @Override
    public void setPreferredUnits(GlucoseUnit units) {
        this.units = units;
    }

    @Override
    public List<String> getRestApiBaseUris() {
        return restApiBaseUris;
    }

    @Override
    public void setRestApiBaseUris(List<String> restApiBaseUris) {
        this.restApiBaseUris = restApiBaseUris;
    }

    @Override
    public boolean isCalibrationUploadEnabled() {
        return calibrationUploadEnabled;
    }

    @Override
    public void setCalibrationUploadEnabled(boolean calibrationUploadEnabled) {
        this.calibrationUploadEnabled = calibrationUploadEnabled;
    }

    @Override
    public boolean isSensorUploadEnabled() {
        return sensorUploadEnabled;
    }

    @Override
    public void setSensorUploadEnabled(boolean sensorUploadEnabled) {
        this.sensorUploadEnabled = sensorUploadEnabled;
    }

    @Override
    public boolean isRawEnabled() {
        return rawUploadEnabled;
    }

    @Override
    public void setRawEnabled(boolean rawUploadEnabled) {
        this.rawUploadEnabled = rawUploadEnabled;
    }

    @Override
    public boolean isMongoUploadEnabled() {
        return mongoUploadEnabled;
    }

    @Override
    public boolean isDataDonateEnabled() {
        return dataDonateEnabled;
    }

    @Override
    public void setDataDonateEnabled(boolean toDonate) {
        this.dataDonateEnabled = toDonate;
    }

    @Override
    public void setMongoUploadEnabled(boolean mongoUploadEnabled) {
        this.mongoUploadEnabled = mongoUploadEnabled;
    }

    @Override
    public String getMongoClientUri() {
        return mongoClientUri;
    }

    @Override
    public void setMongoClientUri(String mongoClientUri) {
        this.mongoClientUri = mongoClientUri;
    }

    @Override
    public String getMongoCollection() {
        return mongoCollection;
    }

    @Override
    public void setMongoCollection(String mongoCollection) {
        this.mongoCollection = mongoCollection;
    }

    @Override
    public String getMongoDeviceStatusCollection() {
        return mongoDeviceStatusCollection;
    }

    @Override
    public boolean isMqttEnabled() {
        return mqttUploadEnabled;
    }

    @Override
    public void setMqttUploadEnabled(boolean mqttUploadEnabled) {
        this.mqttUploadEnabled = mqttUploadEnabled;
    }

    @Override
    public String getMqttEndpoint() {
        return mqttEndpoint;
    }

    @Override
    public void setMqttEndpoint(String endpoint) {
        mqttEndpoint = endpoint;
    }

    @Override
    public String getMqttUser() {
        return null;
    }

    @Override
    public String getMqttPass() {
        return null;
    }

    @Override
    public boolean getIUnderstand() {
        return understand;
    }

    @Override
    public void setIUnderstand(boolean bool) {
        understand = bool;
    }

    @Override
    public void setMongoDeviceStatusCollection(String mongoDeviceStatusCollection) {
        this.mongoDeviceStatusCollection = mongoDeviceStatusCollection;
    }

    @Override
    public void setPwdName(String pwdName) {
        this.pwdName = pwdName;
    }

    @Override
    public boolean hasAskedForData() {
        return askedForData;
    }

    @Override
    public void setAskedForData(boolean askedForData) {
        this.askedForData = askedForData;
    }

    @Override
    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    @Override
    public void setBluetoothDevice(String btDeviceName, String btAddress) {
        this.btAddress = btAddress;
        this.btDeviceName = btDeviceName;
    }

    @Override
    public String getBtAddress() {
        return this.btAddress;
    }

    @Override
    public String getShareSerial() {
        return this.serialNumber;
    }

    @Override
    public void setShareSerial(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Override
    public boolean isMeterUploadEnabled() {
        return mgbUploadEnabled;
    }

    @Override
    public void setMeterUploadEnabled(boolean enabled) {
        mgbUploadEnabled = enabled;
    }

    @Override
    public boolean isInsertionUploadEnabled() {
        return insertionEnabled;
    }

    @Override
    public void setInsertionUploadEnabled(boolean enabled) {
        insertionEnabled = enabled;
    }

    @Override
    public long getLastMeterSysTime() {
        return getLastBaseUpload("ui", "meter");
    }

    @Override
    public void setLastMeterSysTime(long meterSysTime) {
        setLastBaseUpload(meterSysTime, "ui", "meter");
    }

    @Override
    public long getLastEgvSysTime() {
        return getLastBaseUpload("ui", "egv");
    }

    @Override
    public void setLastEgvSysTime(long egvSysTime) {
        setLastBaseUpload(egvSysTime, "ui", "egv");
    }

    @Override
    public boolean isRootEnabled() {
        return false;
    }

    @Override
    public void setLastEgvMqttUpload(long timestamp) {
        setLastBaseUpload(timestamp, "mqtt", "egv");
    }

    @Override
    public void setLastSensorMqttUpload(long timestamp) {
        setLastBaseUpload(timestamp, "mqtt", "sensor");
    }

    @Override
    public void setLastCalMqttUpload(long timestamp) {
        setLastBaseUpload(timestamp, "mqtt", "cal");
    }

    @Override
    public void setLastMeterMqttUpload(long timestamp) {
        setLastBaseUpload(timestamp, "mqtt", "meter");
    }

    @Override
    public void setLastInsMqttUpload(long timestamp) {
        setLastBaseUpload(timestamp, "mqtt", "ins");
    }

    @Override
    public long getLastEgvMqttUpload() {
        return getLastBaseUpload("mqtt", "egv");
    }

    @Override
    public long getLastSensorMqttUpload() {
        return getLastBaseUpload("mqtt", "sensor");
    }

    @Override
    public long getLastCalMqttUpload() {
        return getLastBaseUpload("mqtt", "cal");
    }

    @Override
    public long getLastMeterMqttUpload() {
        return getLastBaseUpload("mqtt", "meter");
    }

    @Override
    public long getLastInsMqttUpload() {
        return getLastBaseUpload("mqtt", "ins");
    }

    @Override
    public void setLastEgvBaseUpload(long timestamp, String postfix) {
        setLastBaseUpload(timestamp, postfix, "egv");
    }

    @Override
    public void setLastSensorBaseUpload(long timestamp, String postfix) {
        setLastBaseUpload(timestamp, postfix, "sensor");
    }

    @Override
    public void setLastCalBaseUpload(long timestamp, String postfix) {
        setLastBaseUpload(timestamp, postfix, "cal");
    }

    @Override
    public void setLastMeterBaseUpload(long timestamp, String postfix) {
        setLastBaseUpload(timestamp, postfix, "meter");
    }

    @Override
    public void setLastInsBaseUpload(long timestamp, String postfix) {
        setLastBaseUpload(timestamp, postfix, "ins");
    }

    @Override
    public long getLastEgvBaseUpload(String postfix) {
        return getLastBaseUpload(postfix, "egv");
    }

    @Override
    public long getLastSensorBaseUpload(String postfix) {
        return getLastBaseUpload(postfix, "sensor");
    }

    @Override
    public long getLastCalBaseUpload(String postfix) {
        return getLastBaseUpload(postfix, "cal");
    }

    @Override
    public long getLastMeterBaseUpload(String postfix) {
        return getLastBaseUpload(postfix, "meter");
    }

    @Override
    public long getLastInsBaseUpload(String postfix) {
        return getLastBaseUpload(postfix, "ins");
    }

    private void setLastBaseUpload(long value, String postfix, String recordType) {
        HashMap<String, Long> record = new HashMap<>();
        record.put(recordType, value);
        lastUpload.put(postfix, record);
    }

    private long getLastBaseUpload(String postfix, String recordType) {
        if (lastUpload.containsKey(postfix) && lastUpload.get(postfix).containsKey(recordType)) {
            return lastUpload.get(postfix).get(recordType);
        } else {
            return 0;
        }
    }

    @Override
    public String getPwdName() {
        return pwdName;
    }
}
