package com.nightscout.core.upload;

import com.google.common.annotations.VisibleForTesting;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MongoUploader extends BaseUploader {

  private DB db;
  private DBCollection collection;
  private DBCollection deviceStatusCollection;
  private MongoClient client;

  public MongoUploader(NightscoutPreferences preferences) {
    super(checkNotNull(preferences));
    this.identifier = "MongoDB";
  }

  public MongoClient getClient() throws IOException {
    if (client != null) {
      return client;
    }
    MongoClientURI dbUri = getUri();
    try {
      this.client = new MongoClient(dbUri);
    } catch (UnknownHostException e) {
      throw new IOException("Error connecting to mongo host " + dbUri, e);
    }
    return this.client;
  }

  private MongoClientURI getUri() {
    return new MongoClientURI(getPreferences().getMongoClientUri());
  }

  @VisibleForTesting
  DB getDB() throws IOException {
    if (db != null) {
      return db;
    }
    db = getClient().getDB(getUri().getDatabase());
    return db;
  }

  @VisibleForTesting
  DBCollection getCollection() throws IOException {
    if (collection != null) {
      return collection;
    }
    collection = getDB().getCollection(getPreferences().getMongoCollection());
    return collection;
  }

  @VisibleForTesting
  DBCollection getDeviceStatusCollection() throws IOException {
    if (deviceStatusCollection != null) {
      return deviceStatusCollection;
    }
    deviceStatusCollection =
        getDB().getCollection(getPreferences().getMongoDeviceStatusCollection());
    return deviceStatusCollection;
  }

  @VisibleForTesting
  void setClient(MongoClient client) {
    this.client = client;
  }

  @VisibleForTesting
  void setDB(DB db) {
    this.db = db;
  }

  @VisibleForTesting
  void setCollection(DBCollection dbCollection) {
    collection = dbCollection;
  }

  @VisibleForTesting
  void setDeviceStatusCollection(DBCollection dbCollection) {
    deviceStatusCollection = dbCollection;
  }

  private BasicDBObject toBasicDBObject(GlucoseDataSet glucoseDataSet) {
    BasicDBObject output = new BasicDBObject();
    output.put("device", "dexcom");
    output.put("date", glucoseDataSet.getDisplayTime().getTime());
    output.put("dateString", glucoseDataSet.getDisplayTime().toString());
    output.put("sgv", glucoseDataSet.getBgMgdl());
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

  private BasicDBObject toBasicDBObject(MeterEntry meterRecord) {
    BasicDBObject output = new BasicDBObject();
    Date timestamp = Utils.receiverTimeToDate(meterRecord.disp_timestamp_sec);
    output.put("device", "dexcom");
    output.put("type", "mbg");
    output.put("date", timestamp.getTime());
    output.put("dateString", timestamp.toString());
    output.put("mbg", meterRecord.meter_bg_mgdl);
    return output;
  }

  private BasicDBObject toBasicDBObject(CalibrationEntry calRecord) {
    BasicDBObject output = new BasicDBObject();
    Date timestamp = Utils.receiverTimeToDate(calRecord.disp_timestamp_sec);
    output.put("device", "dexcom");
    output.put("date", timestamp.getTime());
    output.put("dateString", timestamp.toString());
    output.put("slope", calRecord.slope);
    output.put("intercept", calRecord.intercept);
    output.put("scale", calRecord.scale);
    output.put("type", "cal");
    return output;
  }

  private BasicDBObject toBasicDBObject(AbstractUploaderDevice deviceStatus) {
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

  @Subscribe
  public void handleG4Download(G4Download download) {
    List<GlucoseDataSet> glucoseDataSets =
        Utils.mergeGlucoseDataRecords(download.sgv, download.sensor);
    uploadGlucoseDataSets(glucoseDataSets);
    uploadMeterRecords(download.meter);
    uploadCalRecords(download.cal);
  }

  @Override
  protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
    return upsert(toBasicDBObject(glucoseDataSet));
  }

  @Override
  protected boolean doUpload(MeterEntry meterRecord) throws IOException {
    return upsert(toBasicDBObject(meterRecord));
  }

  @Override
  protected boolean doUpload(CalibrationEntry calRecord) throws IOException {
    return upsert(toBasicDBObject(calRecord));
  }

  @Override
  protected boolean doUpload(AbstractUploaderDevice deviceStatus) throws IOException {
    return upsert(getDeviceStatusCollection(), toBasicDBObject(deviceStatus));
  }

}
