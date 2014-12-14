package com.nightscout.core.preferences;

import com.google.common.collect.Lists;
import com.nightscout.core.download.GlucoseUnits;

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
    private GlucoseUnits units;
    private String pwdName;
    private boolean understand;

    @Override
    public boolean isRestApiEnabled() {
        return restApiEnabled;
    }

    @Override
    public void setRestApiEnabled(boolean restApiEnabled) {
        this.restApiEnabled = restApiEnabled;
    }

    @Override
    public GlucoseUnits getPreferredUnits() {
        return units;
    }

    @Override
    public void setPreferredUnits(GlucoseUnits units) {
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
    public String getPwdName() {
        return pwdName;
    }
}
