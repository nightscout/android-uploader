package com.nightscout.core.upload;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.preferences.NightscoutPreferences;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import static net.tribe7.common.base.Preconditions.checkNotNull;


public class MongoUploader extends BaseUploader {

    private final MongoClientURI dbUri;
    private final String collectionName;
    private final String dsCollectionName;
    private DB db;
    private DBCollection collection;
    private DBCollection deviceStatusCollection;
    private MongoClient client;
    private EventReporter reporter;
    protected ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",
            Locale.getDefault());

    public MongoUploader(NightscoutPreferences preferences, MongoClientURI dbURI,
                         String collectionName, String dsCollectionName, EventReporter reporter) {
        super(preferences);
        checkNotNull(dbURI);
        checkNotNull(collectionName);
        checkNotNull(dsCollectionName);
        this.identifier = "MongoDB";
        this.dbUri = dbURI;
        this.collectionName = collectionName.trim();
        this.dsCollectionName = dsCollectionName.trim();
        this.reporter = reporter;
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

    public DB getDB() throws IOException {
        if (db != null) {
            return db;
        }
        db = getClient().getDB(dbUri.getDatabase());
        return db;
    }

    public DBCollection getCollection() throws IOException {
        if (collection != null) {
            return collection;
        }
        collection = getDB().getCollection(collectionName);
        return collection;
    }

    public DBCollection getDeviceStatusCollection() throws IOException {
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

    private BasicDBObject toBasicDBObject(GlucoseDataSet glucoseDataSet) {
        BasicDBObject output = new BasicDBObject();
        output.put("device", deviceStr);
        output.put("sysTime", glucoseDataSet.getRawDisplayTimeEgv());
        output.put("date", glucoseDataSet.getWallTime().getMillis());
        output.put("dateString", glucoseDataSet.getWallTime().toString());
        output.put("sgv", glucoseDataSet.getBgMgdl());
        output.put("direction", glucoseDataSet.getTrend().friendlyTrendName());
        output.put("type", "sgv");
        if (getPreferences().isRawEnabled()) {
            output.put("filtered", glucoseDataSet.getFiltered());
            output.put("unfiltered", glucoseDataSet.getUnfiltered());
            output.put("rssi", glucoseDataSet.getRssi());
            output.put("noise", glucoseDataSet.getNoise());
        }
        return output;
    }

    private BasicDBObject toBasicDBObject(MeterRecord meterRecord) {
        BasicDBObject output = new BasicDBObject();
        output.put("device", deviceStr);
        output.put("type", "mbg");
        output.put("sysTime", meterRecord.getRawSystemTimeSeconds());
        output.put("date", meterRecord.getWallTime().getMillis());
        output.put("dateString", meterRecord.getWallTime().toString());
        output.put("mbg", meterRecord.getBgMgdl());
        return output;
    }

    private BasicDBObject toBasicDBObject(CalRecord calRecord) {
        BasicDBObject output = new BasicDBObject();
        output.put("device", deviceStr);
        output.put("date", calRecord.getWallTime().getMillis());
        output.put("sysTime", calRecord.getRawSystemTimeSeconds());
        output.put("dateString", calRecord.getWallTime().toString());
        output.put("slope", calRecord.getSlope());
        output.put("intercept", calRecord.getIntercept());
        output.put("scale", calRecord.getScale());
        output.put("type", "cal");
        return output;
    }

    private BasicDBObject toBasicDBObject(InsertionRecord insertionRecord) {
        BasicDBObject output = new BasicDBObject();
        output.put("sysTime", insertionRecord.getSystemTime());
        output.put("date", insertionRecord.getWallTime());
        output.put("dateString", insertionRecord.getWallTime().toString());
        output.put("state", insertionRecord.getState().name());
        output.put("type", insertionRecord.getRecordType());
        return output;
    }

    private BasicDBObject toBasicDBObject(AbstractUploaderDevice deviceStatus, int rcvrBat) {
        BasicDBObject output = new BasicDBObject();
        output.put("uploaderBattery", deviceStatus.getBatteryLevel());
        output.put("receiverBattery", rcvrBat);
        output.put("device", deviceStr);
        output.put("created_at", new Date());
        return output;
    }

    private boolean upsert(BasicDBObject query, BasicDBObject dbObject) throws IOException {
        return upsert(getCollection(), query, dbObject);
    }

    private boolean upsert(DBCollection collection, DBObject query, DBObject dbObject) {
        log.warn("Query: " + query.toString());
        log.warn("Data: " + dbObject.toString());
        log.error("Collection: {}", collection.toString());
        try {
            WriteResult result = collection.update(query, dbObject, true, false,
                    WriteConcern.UNACKNOWLEDGED);
            return result.getError() == null;
        } catch (MongoException e) {
            // Credentials error - user name or password is incorrect.
            if (e.getCode() == 18) {
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        messages.getString("event_mongo_invalid_credentials"));
            }
            log.error("Exception({}): {}", e.getCode(), e.getMessage());
            return false;

        }
    }

    private BasicDBObject toBasicDBObjectQuery(String recordType, long systemTime) {
        BasicDBObject output = new BasicDBObject();
        output.put("type", recordType);
        output.put("sysTime", systemTime);
        return output;
    }

    protected BasicDBObject toBasicDBObjectSgv(GlucoseDataSet glucoseDataSet) {
        BasicDBObject output = new BasicDBObject();
        output.put("device", deviceStr);
        output.put("sysTime", glucoseDataSet.getRawSysemTimeEgv());
        output.put("date", glucoseDataSet.getWallTime().getMillis());
        output.put("dateString", glucoseDataSet.getWallTime().toString());
        output.put("sgv", glucoseDataSet.getBgMgdl());
        output.put("direction", glucoseDataSet.getTrend().friendlyTrendName());
        output.put("type", "sgv");
        return output;
    }

    protected BasicDBObject toBasicDBObjectSensor(GlucoseDataSet glucoseDataSet, BasicDBObject output) {
        output.put("filtered", glucoseDataSet.getFiltered());
        output.put("unfiltered", glucoseDataSet.getUnfiltered());
        output.put("rssi", glucoseDataSet.getRssi());
        output.put("noise", glucoseDataSet.getNoise());
        return output;
    }

    @Override
    protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
        BasicDBObject query = toBasicDBObjectQuery("sgv", glucoseDataSet.getRawSysemTimeEgv());
        BasicDBObject data = toBasicDBObjectSgv(glucoseDataSet);
        if (preferences.isRawEnabled()) {
            if (glucoseDataSet.areRecordsMatched()) {
                return upsert(query, toBasicDBObjectSensor(glucoseDataSet, data));
            } else {
                boolean results;
                results = upsert(query, data);
                data.put("type", "sgv");
                return results & upsert(query, toBasicDBObjectSensor(glucoseDataSet, data));
            }
        } else {
            return upsert(query, data);
        }
    }

    @Override
    protected boolean doUpload(MeterRecord meterRecord) throws IOException {
        BasicDBObject query = toBasicDBObjectQuery("mbg", meterRecord.getRawSystemTimeSeconds());
        return upsert(query, toBasicDBObject(meterRecord));
    }

    @Override
    protected boolean doUpload(CalRecord calRecord) throws IOException {
        BasicDBObject query = toBasicDBObjectQuery("cal", calRecord.getRawSystemTimeSeconds());
        return upsert(query, toBasicDBObject(calRecord));
    }

    @Override
    protected boolean doUpload(InsertionRecord insertionRecord) throws IOException {
        BasicDBObject query = toBasicDBObjectQuery(insertionRecord.getRecordType(), insertionRecord.getRawSystemTimeSeconds());
        return upsert(query, toBasicDBObject(insertionRecord));
    }

    @Override
    protected boolean doUpload(AbstractUploaderDevice deviceStatus, int rcvrBat) throws IOException {
        BasicDBObject dbObject = toBasicDBObject(deviceStatus, rcvrBat);
        return upsert(getDeviceStatusCollection(), dbObject, dbObject);
    }

}
