package com.nightscout.core.preferences;

import java.util.List;

public interface NightscoutPreferences {
    boolean isRestApiEnabled();
    List<String> getRestApiBaseUris();
    boolean isCalibrationUploadEnabled();
    boolean isSensorUploadEnabled();
    boolean isMongoUploadEnabled();
    boolean isDataDonateEnabled();
    void setDataDonateEnabled(boolean toDonate);
    String getMongoClientUri();
    String getMongoCollection();
    String getMongoDeviceStatusCollection();
    void setMongoClientUri(String mongoClientUri);
    void setRestApiBaseUris(List<String> restApis);
    void setMongoCollection(String sgvCollection);
    void setMongoDeviceStatusCollection(String deviceStatusCollection);
    void setMongoUploadEnabled(boolean mongoUploadEnabled);
    void setRestApiEnabled(boolean restApiEnabled);
    // Unable to load values from res files for Robolectric tests
    public static final String DEFAULT_MONGO_COLLECTION = "cgm_data";
    public static final String DEFAULT_MONGO_DEVICE_STATUS_COLLECTION = "devicestatus";


    String getDefaultMongoCollection();
    String getDefaultMongoDeviceStatusCollection();
}
