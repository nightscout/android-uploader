package com.nightscout.core.upload;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.records.DeviceStatus;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class MongoUploader extends BaseUploader {

  private final MongoClientURI dbUri;
  private final String collectionName;
  private final String dsCollectionName;
  private DB db;
  private DBCollection collection;
  private DBCollection deviceStatusCollection;
  private MongoClient client;

  public MongoUploader(NightscoutPreferences preferences, MongoClientURI dbURI,
                       String collectionName, String dsCollectionName) {
    super(preferences);
    checkNotNull(dbURI);
    checkNotNull(collectionName);
    checkNotNull(dsCollectionName);
    this.dbUri = dbURI;
    this.collectionName = collectionName.trim();
    this.dsCollectionName = dsCollectionName.trim();
  }

  public MongoClient getClient() throws IOException {
    if (client != null) {
      return client;
    }
    try {
      this.client = new MongoClient(dbUri);
    } catch (UnknownHostException e) {
      throw new IOException("Error connecting to mongo host " + dbUri.getURI(), e);
    }
    return this.client;
  }

  public void setClient(MongoClient client) {
    this.client = client;
  }

  public DB getDB() throws IOException {
    if (db != null) {
      return db;
    }
    db = getClient().getDB(dbUri.getDatabase());
    return db;
  }

  public void setDB(DB db) {
    this.db = db;
  }

  public DBCollection getCollection() throws IOException {
    if (collection != null) {
      return collection;
    }
    collection = getDB().getCollection(collectionName);
    return collection;
  }

  public void setCollection(DBCollection dbCollection) {
    collection = dbCollection;
  }

  public DBCollection getDeviceStatusCollection() throws IOException {
    if (deviceStatusCollection != null) {
      return deviceStatusCollection;
    }
    deviceStatusCollection = getDB().getCollection(dsCollectionName);
    return deviceStatusCollection;
  }

  public void setDeviceStatusCollection(DBCollection dbCollection) {
    deviceStatusCollection = dbCollection;
  }

  private BasicDBObject toBasicDBObject(GlucoseDataSet glucoseDataSet) {
    BasicDBObject output = new BasicDBObject();
    output.put("device", "dexcom");
    output.put("date", glucoseDataSet.getDisplayTime().getTime());
    output.put("dateString", glucoseDataSet.getDisplayTime().toString());
    output.put("sgv", glucoseDataSet.getBGValue());
    output.put("direction", glucoseDataSet.getTrend().friendlyTrendName());
    output.put("type", "sgv");
    if (getPreferences().isSensorUploadEnabled()) {
      output.put("filtered", glucoseDataSet.getFiltered());
      output.put("unfiltered", glucoseDataSet.getUnfiltered());
      output.put("rssi", glucoseDataSet.getRssi());
      output.put("noise", glucoseDataSet.getNoise());
    }
    return output;
  }

  private BasicDBObject toBasicDBObject(MeterRecord meterRecord) {
    BasicDBObject output = new BasicDBObject();
    output.put("device", "dexcom");
    output.put("type", "mbg");
    output.put("date", meterRecord.getDisplayTime().getTime());
    output.put("dateString", meterRecord.getDisplayTime().toString());
    output.put("mbg", meterRecord.getMeterBG());
    return output;
  }

  private BasicDBObject toBasicDBObject(CalRecord calRecord) {
    BasicDBObject output = new BasicDBObject();
    output.put("device", "dexcom");
    output.put("date", calRecord.getDisplayTime().getTime());
    output.put("dateString", calRecord.getDisplayTime().toString());
    output.put("slope", calRecord.getSlope());
    output.put("intercept", calRecord.getIntercept());
    output.put("scale", calRecord.getScale());
    output.put("type", "cal");
    return output;
  }

  private BasicDBObject toBasicDBObject(DeviceStatus deviceStatus) {
    BasicDBObject output = new BasicDBObject();
    output.put("uploaderBattery", deviceStatus.getBatteryLevel());
    output.put("created_at", new Date());
    return output;
  }

  private boolean upsert(BasicDBObject dbObject) throws IOException {
    return upsert(getCollection(), dbObject);
  }

  private boolean upsert(DBCollection collection, DBObject dbObject) {
    WriteResult result = collection.update(dbObject, dbObject, true, false,
                                           WriteConcern.UNACKNOWLEDGED);
    return result.getError() == null;
  }

  @Override
  protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
    return upsert(toBasicDBObject(glucoseDataSet));
  }

  @Override
  protected boolean doUpload(MeterRecord meterRecord) throws IOException {
    return upsert(toBasicDBObject(meterRecord));
  }

  @Override
  protected boolean doUpload(CalRecord calRecord) throws IOException {
    return upsert(toBasicDBObject(calRecord));
  }

  @Override
  protected boolean doUpload(DeviceStatus deviceStatus) throws IOException {
    return upsert(getDeviceStatusCollection(), toBasicDBObject(deviceStatus));
  }

}
