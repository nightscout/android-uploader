package com.nightscout.android.upload;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.Lists;
import com.mongodb.MongoClientURI;
import com.nightscout.android.MainActivity;
import com.nightscout.android.R;
import com.nightscout.android.ToastReceiver;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.records.DeviceStatus;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.upload.MongoUploader;
import com.nightscout.core.upload.RestLegacyUploader;
import com.nightscout.core.upload.RestV1Uploader;

import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Uploader {
    private static final String LOG_TAG = Uploader.class.getSimpleName();
    private final List<BaseUploader> uploaders;
    private int uploaderCount=0;

    public Uploader(Context context, NightscoutPreferences preferences) {
        checkNotNull(context);
        uploaders = Lists.newArrayList();
        if (preferences.isMongoUploadEnabled()) {
            initializeMongoUploader(context, preferences);
        }
        if (preferences.isRestApiEnabled()) {
            initializeRestUploaders(context, preferences);
        }
    }

    private void initializeMongoUploader(Context context, NightscoutPreferences preferences) {
        String dbURI = preferences.getMongoClientUri();
        String collectionName = preferences.getMongoCollection();
        String dsCollectionName = preferences.getMongoDeviceStatusCollection();
        MongoClientURI uri;
        try {
            uploaderCount+=1;
            uri = new MongoClientURI(dbURI);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Error creating mongo client uri for " + dbURI + ".", e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return;
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Error creating mongo client uri for null value.", e);
            context.sendBroadcast(ToastReceiver.createIntent(context, R.string.unknown_mongo_host));
            return;
        }
        uploaders.add(new MongoUploader(preferences, uri, collectionName, dsCollectionName));
    }

    private void initializeRestUploaders(Context context, NightscoutPreferences preferences) {
        List<String> baseUrisSetting = preferences.getRestApiBaseUris();
        List<URI> baseUris = Lists.newArrayList();
        for (String baseURLSetting : baseUrisSetting) {
            String baseUriString = baseURLSetting.trim();
            if (baseUriString.isEmpty()) continue;
            try {
                baseUris.add(URI.create(baseUriString));
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "Error creating rest uri from preferences.", e);
                context.sendBroadcast(
                        ToastReceiver.createIntent(context, R.string.illegal_rest_url));
            }
        }

        uploaderCount += baseUris.size();
        for (URI baseUri : baseUris) {
            if (baseUri.getPath().contains("v1")) {
                try {
                    uploaders.add(new RestV1Uploader(preferences, baseUri));
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error initializing rest v1 uploader.", e);
                    context.sendBroadcast(
                            ToastReceiver.createIntent(context, R.string.illegal_rest_url));
                }
            } else {
                uploaders.add(new RestLegacyUploader(preferences, baseUri));
            }
        }
    }

    public boolean upload(GlucoseDataSet glucoseDataSet, MeterRecord meterRecord,
                          CalRecord calRecord) {
        return upload(Lists.newArrayList(glucoseDataSet), Lists.newArrayList(meterRecord),
                Lists.newArrayList(calRecord));
    }

    public boolean upload(List<GlucoseDataSet> glucoseDataSets,
                          List<MeterRecord> meterRecords,
                          List<CalRecord> calRecords) {

        DeviceStatus deviceStatus = new DeviceStatus();
        // TODO(trhodeos): make this not static
        deviceStatus.setBatteryLevel(MainActivity.batLevel);

        boolean allSuccessful = true;
        for (BaseUploader uploader : uploaders) {
            // TODO(klee): capture any exceptions here so that all configured uploaders will attempt
            // to upload
            allSuccessful &= uploader.uploadGlucoseDataSets(glucoseDataSets);
            allSuccessful &= uploader.uploadMeterRecords(meterRecords);
            allSuccessful &= uploader.uploadCalRecords(calRecords);
            allSuccessful &= uploader.uploadDeviceStatus(deviceStatus);
        }

        // Force a failure if an uploader was not properly initialized, but only after the other
        // uploaders were executed.
        Log.d("DEBUGING","returning: " + (uploaders.size() == uploaderCount && allSuccessful));
        return uploaders.size() == uploaderCount && allSuccessful;
    }

    protected List<BaseUploader> getUploaders() {
        return uploaders;
    }

    protected int getUploaderCount(){
        return uploaderCount;
    }
}
