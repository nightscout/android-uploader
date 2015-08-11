package com.nightscout.core.preferences;

import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.model.GlucoseUnit;

import java.util.List;

public interface NightscoutPreferences {
    boolean isRestApiEnabled();

    void setRestApiEnabled(boolean restApiEnabled);

    List<String> getRestApiBaseUris();

    void setRestApiBaseUris(List<String> restApis);

    boolean isCalibrationUploadEnabled();

    void setCalibrationUploadEnabled(boolean calibrationUploadEnabled);

    boolean isSensorUploadEnabled();

    void setSensorUploadEnabled(boolean sensorUploadEnabled);

    boolean isRawEnabled();

    void setRawEnabled(boolean rawUploadEnabled);

    boolean isMongoUploadEnabled();

    void setMongoUploadEnabled(boolean mongoUploadEnabled);

    boolean isDataDonateEnabled();

    void setDataDonateEnabled(boolean toDonate);

    String getMongoClientUri();

    void setMongoClientUri(String client);

    String getMongoCollection();

    void setMongoCollection(String mongoCollection);

    String getMongoDeviceStatusCollection();

    boolean isMqttEnabled();

    void setMqttUploadEnabled(boolean mqttEnabled);

    String getMqttEndpoint();

    void setMqttEndpoint(String endpoint);

    String getMqttUser();

    // TODO: (klee) look into how to securely store this information
    String getMqttPass();

    void setMongoDeviceStatusCollection(String deviceStatusCollection);

    boolean getIUnderstand();

    void setIUnderstand(boolean bool);

    GlucoseUnit getPreferredUnits();

    void setPreferredUnits(GlucoseUnit units);

    String getPwdName();

    void setPwdName(String pwdName);

    boolean hasAskedForData();

    void setAskedForData(boolean askedForData);

    DeviceType getDeviceType();

    void setBluetoothDevice(String btDeviceName, String btAddress);

    String getBtAddress();

    String getShareSerial();

    void setShareSerial(String serialNumber);

    boolean isMeterUploadEnabled();

    void setMeterUploadEnabled(boolean enabled);

    boolean isInsertionUploadEnabled();

    void setInsertionUploadEnabled(boolean enabled);

    long getLastMeterSysTime();

    void setLastMeterSysTime(long meterSysTime);

    long getLastEgvSysTime();

    void setLastEgvSysTime(long egvSysTime);

    boolean isRootEnabled();

    public void setLastEgvMqttUpload(long timestamp);

    public void setLastSensorMqttUpload(long timestamp);

    public void setLastCalMqttUpload(long timestamp);

    public void setLastMeterMqttUpload(long timestamp);

    public void setLastInsMqttUpload(long timestamp);

    public long getLastEgvMqttUpload();

    public long getLastSensorMqttUpload();

    public long getLastCalMqttUpload();

    public long getLastMeterMqttUpload();

    public long getLastInsMqttUpload();

    public void setLastEgvBaseUpload(long timestamp, String postfix);

    public void setLastSensorBaseUpload(long timestamp, String postfix);

    public void setLastCalBaseUpload(long timestamp, String postfix);

    public void setLastMeterBaseUpload(long timestamp, String postfix);

    public void setLastInsBaseUpload(long timestamp, String postfix);

    public long getLastEgvBaseUpload(String postfix);

    public long getLastSensorBaseUpload(String postfix);

    public long getLastCalBaseUpload(String postfix);

    public long getLastMeterBaseUpload(String postfix);

    public long getLastInsBaseUpload(String postfix);
}
