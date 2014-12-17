package com.nightscout.core.preferences;

import com.nightscout.core.download.GlucoseUnits;

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

  void setMongoDeviceStatusCollection(String deviceStatusCollection);

  boolean getIUnderstand();

  void setIUnderstand(boolean bool);

  GlucoseUnits getPreferredUnits();

  void setPreferredUnits(GlucoseUnits units);

  String getPwdName();

  void setPwdName(String pwdName);

  boolean hasAskedForData();

  void setAskedForData(boolean askedForData);
}
