package com.nightscout.android.upload;

import android.content.Context;

import com.mongodb.MongoClientURI;
import com.nightscout.android.R;
import com.nightscout.android.ToastReceiver;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GenericTimestampRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.InsertionEntry;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.upload.MongoUploader;
import com.nightscout.core.upload.RestLegacyUploader;
import com.nightscout.core.upload.RestV1Uploader;
import com.squareup.wire.Message;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class Uploader {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final List<BaseUploader> uploaders;
    private boolean allUploadersInitalized = true;
    private EventReporter reporter;
    private Context context;
    protected NightscoutPreferences preferences;

    public Uploader(Context context, NightscoutPreferences preferences) {
        checkNotNull(context);
        this.preferences = preferences;
        this.context = context;
        reporter = AndroidEventReporter.getReporter(context);
        uploaders = new ArrayList<>();
        if (preferences.isMongoUploadEnabled()) {
            allUploadersInitalized &= initializeMongoUploader(context, preferences);
        }
        if (preferences.isRestApiEnabled()) {
            allUploadersInitalized &= initializeRestUploaders(context, preferences);
        }
        this.context = context;
    }

    public boolean areAllUploadersInitalized() {
        return allUploadersInitalized;
    }

    private boolean initializeMongoUploader(Context context, NightscoutPreferences preferences) {
        String dbURI = preferences.getMongoClientUri();
        String collectionName = preferences.getMongoCollection();
        String dsCollectionName = preferences.getMongoDeviceStatusCollection();
        checkNotNull(collectionName);
        checkNotNull(dsCollectionName);
        MongoClientURI uri;
        try {
            uri = new MongoClientURI(dbURI);
        } catch (IllegalArgumentException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    context.getString(R.string.unknown_mongo_host));
            log.error("Error creating mongo client uri for {}.{}", dbURI, e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return false;
        } catch (NullPointerException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    context.getString(R.string.unknown_mongo_host));
            log.error("Error creating mongo client uri for null value. {}", e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return false;
        } catch (StringIndexOutOfBoundsException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    context.getString(R.string.unknown_mongo_host));
            log.error("Error creating mongo client uri for null value. {}", e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return false;
        }
        uploaders.add(new MongoUploader(preferences, uri, collectionName, dsCollectionName, reporter));
        return true;
    }

    private boolean initializeRestUploaders(Context context, NightscoutPreferences preferences) {
        List<String> baseUrisSetting = preferences.getRestApiBaseUris();
        List<URI> baseUris = new ArrayList<>();
        boolean allInitialized = true;
        for (String baseURLSetting : baseUrisSetting) {
            String baseUriString = baseURLSetting.trim();
            if (baseUriString.isEmpty()) continue;
            try {
                baseUris.add(URI.create(baseUriString));
            } catch (IllegalArgumentException e) {
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        context.getString(R.string.illegal_rest_url));
                log.error("Error creating rest uri from preferences. {}", e);
                context.sendBroadcast(
                        ToastReceiver.createIntent(context, R.string.illegal_rest_url));
            }
        }

        for (URI baseUri : baseUris) {
            if (baseUri.getPath().contains("v1")) {
                try {
                    uploaders.add(new RestV1Uploader(preferences, baseUri));
                } catch (IllegalArgumentException e) {
                    log.error("Error initializing rest v1 uploader. {}", e);
                    allInitialized &= false;
                    context.sendBroadcast(
                            ToastReceiver.createIntent(context, R.string.illegal_rest_url));
                }
            } else {
                uploaders.add(new RestLegacyUploader(preferences, baseUri));
            }
        }
        return allInitialized;
    }

    public boolean upload(G4Download download, int numRecords) {
        long refTime = DateTime.parse(download.download_timestamp).getMillis();
        List<CalibrationEntry> calList = Utils.filterRecords(numRecords, download.cal);
        List<MeterEntry> meterList = Utils.filterRecords(numRecords, download.meter);
        List<InsertionEntry> insertionList = Utils.filterRecords(numRecords, download.insert);
        List<CalRecord> calRecords = new ArrayList<>();
        List<MeterRecord> meterRecords = new ArrayList<>();
        List<InsertionRecord> insertionRecords = new ArrayList<>();
        if (download.receiver_system_time_sec != null) {
            calRecords = asRecordList(calList, CalRecord.class, download.receiver_system_time_sec, refTime);
            meterRecords = asRecordList(meterList, MeterRecord.class, download.receiver_system_time_sec, refTime);
            insertionRecords = asRecordList(insertionList, InsertionRecord.class, download.receiver_system_time_sec, refTime);
            log.debug("Number of Insertion Records (Uploader): {}", insertionRecords.size());
        }


        List<GlucoseDataSet> glucoseDataSets = new ArrayList<>();
        if (download.sgv.size() > 0) {
            glucoseDataSets = Utils.mergeGlucoseDataRecords(download, numRecords);
        }

        int receiverBattery = (download.receiver_battery != null) ? download.receiver_battery : -1;

        return upload(glucoseDataSets, meterRecords, calRecords, insertionRecords, receiverBattery);
    }

    public <T, U extends Message> List<T> asRecordList(List<U> entryList, Class<T> clazz, long rcvrTime, long refTime) {
        List<T> result = new ArrayList<>();
        for (U entry : entryList) {
            try {
                result.add(clazz.getConstructor(entry.getClass(), long.class, long.class).newInstance(entry, rcvrTime, refTime));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                // nom
                log.error("Exception {}", e);
            }
        }
        return result;
    }

    public boolean upload(G4Download download) {
        long refTime = DateTime.parse(download.download_timestamp).getMillis();
        List<GlucoseDataSet> glucoseDataSets = Utils.mergeGlucoseDataRecords(download.sgv, download.sensor, download.receiver_system_time_sec, refTime);
        List<MeterRecord> meterRecords = asRecordList(download.meter, MeterRecord.class, download.receiver_system_time_sec, refTime);
        List<CalRecord> calRecords = asRecordList(download.cal, CalRecord.class, download.receiver_system_time_sec, refTime);
        List<InsertionRecord> insertionRecords = asRecordList(download.insert, InsertionRecord.class, download.receiver_system_time_sec, refTime);

        return upload(glucoseDataSets, meterRecords, calRecords, insertionRecords, download.receiver_battery);
//        return upload(glucoseDataSets, meterRecords, calRecords, insertionRecords, 0);
    }

    private boolean upload(List<GlucoseDataSet> glucoseDataSets,
                           List<MeterRecord> meterRecords,
                           List<CalRecord> calRecords, List<InsertionRecord> insertionRecords, int rcvrBat) {

        AbstractUploaderDevice deviceStatus = AndroidUploaderDevice.getUploaderDevice(context);

        boolean allSuccessful = true;
        boolean successful = true;
        for (BaseUploader uploader : uploaders) {
            try {
                String id = uploader.getIdentifier();
                long lastGlucoseDataSetUpload = preferences.getLastEgvBaseUpload(id);
                long lastMeterRecordsUpload = preferences.getLastMeterBaseUpload(id);
                long lastCalRecordsUpload = preferences.getLastCalBaseUpload(id);
                long lastInsRecordsUpload = preferences.getLastInsBaseUpload(id);
                List<GlucoseDataSet> filteredGlucoseDataSet = filterRecords(glucoseDataSets, lastGlucoseDataSetUpload);
                List<MeterRecord> filteredMeterRecords = filterRecords(meterRecords, lastMeterRecordsUpload);
                List<CalRecord> filteredCalRecords = filterRecords(calRecords, lastCalRecordsUpload);
                List<InsertionRecord> filteredInsRecords = filterRecords(insertionRecords, lastInsRecordsUpload);
                successful = uploader.uploadRecords(filteredGlucoseDataSet, filteredMeterRecords, filteredCalRecords, filteredInsRecords, deviceStatus, rcvrBat);
                if (successful) {
                    if (filteredGlucoseDataSet.size() > 0) {
                        log.debug("Uploaded {} merged records (EGV and Sensor)", filteredGlucoseDataSet.size());
                        preferences.setLastEgvBaseUpload(filteredGlucoseDataSet.get(filteredGlucoseDataSet.size() - 1).getRawSysemTimeEgv(), id);
                    }
                    if (filteredMeterRecords.size() > 0) {
                        log.debug("Uploaded {} meter records", filteredMeterRecords.size());
                        preferences.setLastMeterBaseUpload(filteredMeterRecords.get(filteredMeterRecords.size() - 1).getRawSystemTimeSeconds(), id);
                    }
                    if (filteredCalRecords.size() > 0) {
                        log.debug("Uploaded {} calibration records", filteredCalRecords.size());
                        preferences.setLastCalBaseUpload(filteredCalRecords.get(filteredCalRecords.size() - 1).getRawSystemTimeSeconds(), id);
                    }
                    if (filteredInsRecords.size() > 0) {
                        log.debug("Uploaded {} insertion records", filteredInsRecords.size());
                        preferences.setLastInsBaseUpload(filteredInsRecords.get(filteredInsRecords.size() - 1).getRawSystemTimeSeconds(), id);
                    }
                }
                allSuccessful &= successful;
            } catch (Exception e) {
                allSuccessful &= false;
            }
            if (successful) {
                reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                        String.format(context.getString(R.string.event_success_upload),
                                uploader.getIdentifier()));
            } else {
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        String.format(context.getString(R.string.event_fail_upload),
                                uploader.getIdentifier()));
            }
        }

        return allSuccessful;
    }

    public List<BaseUploader> getUploaders() {
        return uploaders;
    }

    protected boolean areAllUploadersInitialized() {
        return allUploadersInitalized;
    }

    private <T extends GenericTimestampRecord> List<T> filterRecords(List<? extends GenericTimestampRecord> recordList, long lastSysTime) {
        List<T> results = new ArrayList<>();
        for (GenericTimestampRecord record : recordList) {
            log.error("Comparing record time stamp {} to last recorded {}", record.getRawSystemTimeSeconds(), lastSysTime);
            if (record.getRawSystemTimeSeconds() > lastSysTime) {
                log.error("Comparing: Adding record");
                results.add((T) record);
            } else {
                log.error("Comparing: Not adding record");
            }
        }
        return results;
    }

}
