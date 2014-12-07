package com.nightscout.core.preferences;

import com.google.common.collect.Lists;

import java.util.List;

public class TestPreferences implements NightscoutPreferences {
    private boolean restApiEnabled = false;
    private List<String> restApiBaseUris = Lists.newArrayList();
    private boolean calibrationUploadEnabled = false;
    private boolean sensorUploadEnabled = false;
    private boolean mongoUploadEnabled = false;
    private String mongoClientUri = null;
    private String mongoCollection = null;
    private String mongoDeviceStatusCollection = null;
    private boolean dataDonateEnabled;

    public boolean isRestApiEnabled() {
        return restApiEnabled;
    }

    public void setRestApiEnabled(boolean restApiEnabled) {
        this.restApiEnabled = restApiEnabled;
    }

    @Override
    public String getDefaultMongoCollection() {
        return DEFAULT_MONGO_COLLECTION;
    }

    @Override
    public String getDefaultMongoDeviceStatusCollection() {
        return TestPreferences.DEFAULT_MONGO_DEVICE_STATUS_COLLECTION;
    }

    public List<String> getRestApiBaseUris() {
        return restApiBaseUris;
    }

    public void setRestApiBaseUris(List<String> restApiBaseUris) {
        this.restApiBaseUris = restApiBaseUris;
    }

    public boolean isCalibrationUploadEnabled() {
        return calibrationUploadEnabled;
    }

    public void setCalibrationUploadEnabled(boolean calibrationUploadEnabled) {
        this.calibrationUploadEnabled = calibrationUploadEnabled;
    }

    public boolean isSensorUploadEnabled() {
        return sensorUploadEnabled;
    }

    public void setSensorUploadEnabled(boolean sensorUploadEnabled) {
        this.sensorUploadEnabled = sensorUploadEnabled;
    }

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

    public void setMongoUploadEnabled(boolean mongoUploadEnabled) {
        this.mongoUploadEnabled = mongoUploadEnabled;
    }

    public String getMongoClientUri() {
        return mongoClientUri;
    }

    public void setMongoClientUri(String mongoClientUri) {
        this.mongoClientUri = mongoClientUri;
    }

    public String getMongoCollection() {
        return mongoCollection;
    }

    public void setMongoCollection(String mongoCollection) {
        this.mongoCollection = mongoCollection;
    }

    public String getMongoDeviceStatusCollection() {
        return mongoDeviceStatusCollection;
    }

    public void setMongoDeviceStatusCollection(String mongoDeviceStatusCollection) {
        this.mongoDeviceStatusCollection = mongoDeviceStatusCollection;
    }
}
