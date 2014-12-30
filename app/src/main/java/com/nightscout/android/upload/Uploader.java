package com.nightscout.android.upload;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.Lists;
import com.mongodb.MongoClientURI;
import com.nightscout.android.R;
import com.nightscout.android.ToastReceiver;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.CookieMonsterDownload;
import com.nightscout.core.model.CookieMonsterG4Cal;
import com.nightscout.core.model.CookieMonsterG4Meter;
import com.nightscout.core.model.CookieMonsterG4SGV;
import com.nightscout.core.model.CookieMonsterG4Sensor;
import com.nightscout.core.model.DownloadResults;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.upload.MongoUploader;
import com.nightscout.core.upload.RestLegacyUploader;
import com.nightscout.core.upload.RestV1Uploader;
import com.squareup.wire.Message;

import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Uploader {
    private static final String LOG_TAG = Uploader.class.getSimpleName();
    private final List<BaseUploader> uploaders;
    private boolean allUploadersInitalized = true;
    private EventReporter reporter;
    private Context context;

    public Uploader(Context context, NightscoutPreferences preferences) {
        checkNotNull(context);
        this.context = context;
        reporter = AndroidEventReporter.getReporter(context);
        uploaders = Lists.newArrayList();
        if (preferences.isMongoUploadEnabled()) {
            allUploadersInitalized &= initializeMongoUploader(context, preferences);
        }
        if (preferences.isRestApiEnabled()) {
            allUploadersInitalized &= initializeRestUploaders(context, preferences);
        }
        this.context = context;
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
            Log.e(LOG_TAG, "Error creating mongo client uri for " + dbURI + ".", e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return false;
        } catch (NullPointerException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    context.getString(R.string.unknown_mongo_host));
            Log.e(LOG_TAG, "Error creating mongo client uri for null value.", e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return false;
        } catch (StringIndexOutOfBoundsException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    context.getString(R.string.unknown_mongo_host));
            Log.e(LOG_TAG, "Error creating mongo client uri for null value.", e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return false;
        }
        uploaders.add(new MongoUploader(preferences, uri, collectionName, dsCollectionName));
        return true;
    }

    private boolean initializeRestUploaders(Context context, NightscoutPreferences preferences) {
        List<String> baseUrisSetting = preferences.getRestApiBaseUris();
        List<URI> baseUris = Lists.newArrayList();
        boolean allInitialized = true;
        for (String baseURLSetting : baseUrisSetting) {
            String baseUriString = baseURLSetting.trim();
            if (baseUriString.isEmpty()) continue;
            try {
                baseUris.add(URI.create(baseUriString));
            } catch (IllegalArgumentException e) {
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        context.getString(R.string.illegal_rest_url));
                Log.e(LOG_TAG, "Error creating rest uri from preferences.", e);
                context.sendBroadcast(
                        ToastReceiver.createIntent(context, R.string.illegal_rest_url));
            }
        }

        for (URI baseUri : baseUris) {
            if (baseUri.getPath().contains("v1")) {
                try {
                    uploaders.add(new RestV1Uploader(preferences, baseUri));
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error initializing rest v1 uploader.", e);
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

    public boolean upload(DownloadResults downloadResults, int numRecords) {
        CookieMonsterDownload download = downloadResults.getDownload();
        List<CookieMonsterG4SGV> sgvList = filterRecords(numRecords, download.sgv);
        List<CookieMonsterG4Cal> calList = filterRecords(numRecords, download.cal);
        List<CookieMonsterG4Meter> meterList = filterRecords(numRecords, download.meter);
        List<CookieMonsterG4Sensor> sensorList = filterRecords(numRecords, download.sensor);

        List<GlucoseDataSet> glucoseDataSets = Utils.mergeGlucoseDataRecords(sgvList, sensorList);

        return upload(glucoseDataSets, meterList, calList);
    }

    private <T extends Message> List<T> filterRecords(int numRecords, List<T> records) {
        int recordIndexToStop = Math.max(records.size() - numRecords, 0);
        List<T> results = Lists.newArrayList();
        for (int i = records.size(); i > recordIndexToStop; i--) {
            results.add(records.get(i - 1));
        }
        return results;
    }

    public boolean upload(DownloadResults downloadResults) {
        CookieMonsterDownload download = downloadResults.getDownload();
        List<GlucoseDataSet> glucoseDataSets = Utils.mergeGlucoseDataRecords(download.sgv, download.sensor);
        return upload(glucoseDataSets, download.meter, download.cal);
    }

    public boolean upload(List<GlucoseDataSet> glucoseDataSets,
                          List<CookieMonsterG4Meter> meterRecords,
                          List<CookieMonsterG4Cal> calRecords) {

        AbstractUploaderDevice deviceStatus = AndroidUploaderDevice.getUploaderDevice(context);

        boolean allSuccessful = true;
        for (BaseUploader uploader : uploaders) {
            // TODO(klee): capture any exceptions here so that all configured uploaders will attempt
            // to upload
            allSuccessful &= uploader.uploadGlucoseDataSets(glucoseDataSets);
            allSuccessful &= uploader.uploadMeterRecords(meterRecords);
            allSuccessful &= uploader.uploadCalRecords(calRecords);
            allSuccessful &= uploader.uploadDeviceStatus(deviceStatus);
            reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                    String.format(context.getString(R.string.event_success_upload),
                            uploader.getIdentifier()));
        }

        // Force a failure if an uploader was not properly initialized, but only after the other
        // uploaders were executed.
        return allUploadersInitalized && allSuccessful && (uploaders.size() != 0);
    }

    protected List<BaseUploader> getUploaders() {
        return uploaders;
    }

    protected boolean areAllUploadersInitialized() {
        return allUploadersInitalized;
    }
}
