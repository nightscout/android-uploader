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
}
