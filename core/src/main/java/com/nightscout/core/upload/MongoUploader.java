package com.nightscout.core.upload;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.MongoSecurityException;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.preferences.NightscoutPreferences;

import net.tribe7.common.base.Function;
import net.tribe7.common.collect.Lists;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class MongoUploader extends BaseUploader {

    private final MongoClientURI dbUri;
    private final String collectionName;
    private final String dsCollectionName;
    private final EventReporter reporter;
    private MongoDatabase db;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> deviceStatusCollection;
    private MongoClient client;
    protected ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",
                                                                 Locale.getDefault());

    public MongoUploader(NightscoutPreferences preferences, MongoClientURI dbURI,
                         String collectionName, String dsCollectionName, EventReporter reporter) {
        super(preferences);

        this.identifier = "MongoDB";
        this.dbUri = checkNotNull(dbURI);
        this.collectionName = checkNotNull(collectionName).trim();
        this.dsCollectionName = checkNotNull(dsCollectionName).trim();
        this.reporter = reporter;
    }

    public MongoClient getClient() {
        if (client != null) {
            return client;
        }
        try {
            this.client = new MongoClient(dbUri);
        } catch (MongoSecurityException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                            messages.getString("event_mongo_invalid_credentials"));
        } catch (MongoException e) {
            throw new RuntimeException("Error connecting to mongo.");
        }
        return this.client;
    }

    public MongoDatabase getDB() {
        if (db != null) {
            return db;
        }
        db = getClient().getDatabase(dbUri.getDatabase()).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        return db;
    }

    public MongoCollection<Document> getCollection() {
        if (collection != null) {
            return collection;
        }
        collection = getDB().getCollection(collectionName);
        return collection;
    }

    public MongoCollection<Document> getDeviceStatusCollection() {
        if (deviceStatusCollection != null) {
            return deviceStatusCollection;
        }
        deviceStatusCollection = getDB().getCollection(dsCollectionName);
        return deviceStatusCollection;
    }

    public void setClient(MongoClient client) {
        this.client = client;
    }

    public void setDB(MongoDatabase db) {
        this.db = db;
    }

    public void setCollection(MongoCollection<Document> dbCollection) {
        collection = dbCollection;
    }

    public void setDeviceStatusCollection(MongoCollection<Document> dbCollection) {
        deviceStatusCollection = dbCollection;
    }

    private Document toDocument(GlucoseDataSet glucoseDataSet) {
        Document output = new Document();
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

    private Document toDocument(MeterRecord meterRecord) {
        Document output = new Document();
        output.put("device", deviceStr);
        output.put("type", "mbg");
        output.put("sysTime", meterRecord.getRawSystemTimeSeconds());
        output.put("date", meterRecord.getWallTime().getMillis());
        output.put("dateString", meterRecord.getWallTime().toString());
        output.put("mbg", meterRecord.getBgMgdl());
        return output;
    }

    private Document toDocument(CalRecord calRecord) {
        Document output = new Document();
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

    private Document toDocument(InsertionRecord insertionRecord) {
        Document output = new Document();
        output.put("sysTime", insertionRecord.getSystemTime());
        output.put("date", insertionRecord.getWallTime());
        output.put("dateString", insertionRecord.getWallTime().toString());
        output.put("state", insertionRecord.getState().name());
        output.put("type", insertionRecord.getRecordType());
        return output;
    }

    private Document toDocument(AbstractUploaderDevice deviceStatus, int rcvrBat) {
        Document output = new Document();
        output.put("uploaderBattery", deviceStatus.getBatteryLevel());
        output.put("receiverBattery", rcvrBat);
        output.put("device", deviceStr);
        output.put("created_at", new Date());
        return output;
    }

    private WriteModel<Document> toUpsertModel(Document document) {
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        Bson filter = Filters.and(Filters.eq("type", document.get("type")),
                                  Filters.eq("sysTime", document.get("sysTime")));
        return new ReplaceOneModel<>(filter, document, options);
    }

    @Override
    protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
        // TODO(trhodeos): never called because we bulk upsert instead. this is terrible..
        // Hopefully will be resolved soon.
        return false;
    }

    @Override
    public boolean uploadRecords(List<GlucoseDataSet> glucoseDataSets, List<MeterRecord> meterRecords, List<CalRecord> calRecords, List<InsertionRecord> insertionRecords, AbstractUploaderDevice deviceStatus, int rcvrBat) {
        List<WriteModel<Document>> allUpserts = Lists.newArrayList();
        List<WriteModel<Document>> deviceStatusUpserts = Lists.newArrayList();

        allUpserts.addAll(Lists.transform(glucoseDataSets,
                                          new Function<GlucoseDataSet, WriteModel<Document>>() {
                                              @Override
                                              public WriteModel<Document> apply(
                                                  GlucoseDataSet input) {
                                                  return toUpsertModel(toDocument(input));
                                              }
                                          }));
        allUpserts.addAll(Lists.transform(meterRecords,
                                          new Function<MeterRecord, WriteModel<Document>>() {
                                              @Override
                                              public WriteModel<Document> apply(MeterRecord input) {
                                                  return toUpsertModel(toDocument(input));
                                              }
                                          }));
        allUpserts.addAll(Lists.transform(calRecords,
                                          new Function<CalRecord, WriteModel<Document>>() {
                                              @Override
                                              public WriteModel<Document> apply(CalRecord input) {
                                                  return toUpsertModel(toDocument(input));
                                              }
                                          }));
        allUpserts.addAll(Lists.transform(insertionRecords,
                                          new Function<InsertionRecord, WriteModel<Document>>() {
                                              @Override
                                              public WriteModel<Document> apply(
                                                  InsertionRecord input) {
                                                  return toUpsertModel(toDocument(input));
                                              }
                                          }));


        BulkWriteResult result = getCollection().bulkWrite(allUpserts);
        getDeviceStatusCollection().insertOne(toDocument(deviceStatus, rcvrBat));

        return result.wasAcknowledged();
    }
}
