package com.nightscout.core.preferences;

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

    boolean isMedtronicEnabled( );
    void setMedtronicSerial(String serialnum);
    String getMedtronicSerial( );
}
