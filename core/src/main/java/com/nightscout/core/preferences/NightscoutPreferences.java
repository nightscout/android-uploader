package com.nightscout.core.preferences;

import java.util.List;

public interface NightscoutPreferences {
    boolean isRestApiEnabled();
    List<String> getRestApiBaseUris();
    boolean isCalibrationUploadEnabled();
    boolean isSensorUploadEnabled();
    boolean isMongoUploadEnabled();
    String getMongoClientUri();
    String getMongoCollection();
    String getMongoDeviceStatusCollection();
    void setMongoClientUri(String mongoClientUri);
    void setRestApiBaseUris(List<String> restApis);
    void setMongoCollection(String sgvCollection);
    void setMongoDeviceStatusCollection(String deviceStatusCollection);
    void setMongoUploadEnabled(boolean mongoUploadEnabled);
    void setRestApiEnabled(boolean restApiEnabled);
}
