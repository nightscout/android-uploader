package com.nightscout.core.upload;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.records.DeviceStatus;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;

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

    public MongoClient getClient() {
        if (client != null) {
            return client;
        }
        try {
            this.client = new MongoClient(dbUri);
        } catch (UnknownHostException e) {
            log.error("Error connection to mongo uri {}.", dbUri.toString());
        }
        return this.client;
    }

    public DB getDB() {
        if (db != null) {
            return db;
        }
        db = getClient().getDB(dbUri.getDatabase());
        return db;
    }

    public DBCollection getCollection() {
        if (collection != null) {
            return collection;
        }
        collection = getDB().getCollection(collectionName);
        return collection;
    }

    public DBCollection getDeviceStatusCollection() {
        if (deviceStatusCollection != null) {
            return deviceStatusCollection;
        }
        deviceStatusCollection = getDB().getCollection(dsCollectionName);
        return deviceStatusCollection;
    }

    public void setClient(MongoClient client) {
        this.client = client;
    }

    public void setDB(DB db) {
        this.db = db;
    }

    public void setCollection(DBCollection dbCollection) {
        collection = dbCollection;
    }

    public void setDeviceStatusCollection(DBCollection dbCollection) {
        deviceStatusCollection = dbCollection;
    }

    private BasicDBObject toBasicDBObject(GlucoseDataSet glucoseDataSet)  {
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

    private void upsert(BasicDBObject dbObject) {
        upsert(getCollection(), dbObject);
    }

    private void upsert(DBCollection collection, DBObject dbObject) {
        collection.update(dbObject, dbObject, true, false, WriteConcern.UNACKNOWLEDGED);
    }

    @Override
    protected void doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
        upsert(toBasicDBObject(glucoseDataSet));
    }

    @Override
    protected void doUpload(MeterRecord meterRecord) throws IOException {
        upsert(toBasicDBObject(meterRecord));
    }

    @Override
    protected void doUpload(CalRecord calRecord) throws IOException {
        upsert(toBasicDBObject(calRecord));
    }

    @Override
    protected void doUpload(DeviceStatus deviceStatus) {
        upsert(getDeviceStatusCollection(), toBasicDBObject(deviceStatus));
    }

}
