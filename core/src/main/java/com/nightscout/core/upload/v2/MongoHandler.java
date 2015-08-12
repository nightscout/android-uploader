package com.nightscout.core.upload.v2;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.converters.MongoConverters;
import com.nightscout.core.upload.converters.SensorGlucoseValueAndRawSensorReading;
import com.nightscout.core.utils.DexcomG4Utils;
import com.squareup.wire.Message;

import net.tribe7.common.base.Strings;
import net.tribe7.common.collect.Lists;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class MongoHandler extends G4DataHandler {

  private final NightscoutPreferences preferences;
  private MongoCollection<Document> mongoCollection;
  private MongoClient mongoClient;
  private String currentMongoUrl;

  private final Object mongoClientLock = new Object();

  public MongoHandler(NightscoutPreferences preferences) {
    this.preferences = checkNotNull(preferences);
    handleSettingsRefresh();
  }

  @Override
  protected void handleSettingsRefresh() {
    final boolean preferenceChanged = !Strings.nullToEmpty(currentMongoUrl).equals(preferences.getMongoClientUri());
    if (preferenceChanged) {
      refreshConnection();
    }
  }

  private void refreshConnection() {
    synchronized (mongoClientLock) {
      if (mongoClient != null) {
        mongoClient.close();
        mongoClient = null;
      }
      currentMongoUrl = preferences.getMongoClientUri();
      MongoClientURI mongoClientURI = new MongoClientURI(currentMongoUrl);
      mongoClient = new MongoClient(mongoClientURI);
      mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);
      mongoCollection = mongoClient.getDatabase(mongoClientURI.getDatabase()).getCollection(
          mongoClientURI.getCollection());
    }
  }

  @Override
  protected boolean isEnabled() {
    return preferences.isMongoUploadEnabled();
  }

  @Override
  protected List<Message> handleG4Data(final G4Data download) {
    List<Document> dbObjects = Lists.newArrayList();
    List<SensorGlucoseValueAndRawSensorReading> mergedObjects =
        DexcomG4Utils.mergeRawEntries(download);
    dbObjects.addAll(Lists.transform(mergedObjects, MongoConverters.sensorReadingConverter()));
    dbObjects.addAll(Lists.transform(download.insertions, MongoConverters.insertionConverter()));
    dbObjects
        .addAll(Lists.transform(download.calibrations, MongoConverters.calibrationConverter()));
    dbObjects.addAll(
        Lists.transform(download.manual_meter_entries, MongoConverters.manualMeterEntryConverter()));

    BulkWriteResult result;
    synchronized (mongoClientLock) {
      result = mongoCollection.bulkWrite(toBulkUpsertList(dbObjects));
    }
    List<Message> output = Lists.newArrayList();
    if (result != null && result.wasAcknowledged()) {
      DexcomG4Utils.addAllEntriesAsMessages(download, output);
    }
    return output;
  }

  private List<WriteModel<Document>> toBulkUpsertList(List<Document> dbObjects) {
    List<WriteModel<Document>> output = Lists.newArrayList();
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    for (Document object : dbObjects) {
      Bson filter = Filters.and(Filters.eq("type", object.get("type")),
                                Filters.eq("sysTime", object.get("sysTime")));
      output.add(new ReplaceOneModel<>(filter, object, options));
    }
    return output;
  }
}
