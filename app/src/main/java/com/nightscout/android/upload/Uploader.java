package com.nightscout.android.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.mongodb.MongoClientURI;
import com.nightscout.android.MainActivity;
import com.nightscout.android.R;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.preferences.PreferenceKeys;
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

    public Uploader(Context context) {
        checkNotNull(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        NightscoutPreferences preferences = new AndroidPreferences(prefs);
        uploaders = Lists.newArrayList();
        if (prefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, false)) {
            initializeMongoUploader(context, preferences);
        }
        if (prefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED, false)) {
            initializeRestUploaders(context, preferences);
        }
    }

    private void initializeMongoUploader(Context context, NightscoutPreferences preferences) {
        String dbURI = preferences.getMongoClientUri();
        String collectionName = preferences.getMongoCollection();
        String dsCollectionName = preferences.getMongoDeviceStatusCollection();
        MongoClientURI uri;
        try {
            uri = new MongoClientURI(dbURI);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Error creating mongo client uri for " + dbURI + ".", e);
            Toast.makeText(context, R.string.unknown_mongo_host, Toast.LENGTH_LONG);
            return;
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Error creating mongo client uri for null value.", e);
            Toast.makeText(context, R.string.unknown_mongo_host, Toast.LENGTH_LONG);
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
            baseUris.add(URI.create(baseUriString));
        }

        for (URI baseUri : baseUris) {
            if (baseUri.getPath().contains("v1")) {
                try {
                    uploaders.add(new RestV1Uploader(preferences, baseUri));
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error initializing rest v1 uploader.", e);
                    Toast.makeText(context, R.string.illegal_rest_url, Toast.LENGTH_LONG);
                    continue;
                }
            } else {
                uploaders.add(new RestLegacyUploader(preferences, baseUri));
            }
        }
    }

    public boolean upload(GlucoseDataSet glucoseDataSet, MeterRecord meterRecord, CalRecord calRecord) {
        return upload(Lists.newArrayList(glucoseDataSet), Lists.newArrayList(meterRecord), Lists.newArrayList(calRecord));
    }

    public boolean upload(List<GlucoseDataSet> glucoseDataSets,
                          List<MeterRecord> meterRecords,
                          List<CalRecord> calRecords) {

        DeviceStatus deviceStatus = new DeviceStatus();
        // TODO(trhodeos): make this not static
        deviceStatus.setBatteryLevel(MainActivity.batLevel);

        for (BaseUploader uploader : uploaders) {
            uploader.uploadGlucoseDataSets(glucoseDataSets);
            uploader.uploadMeterRecords(meterRecords);
            uploader.uploadCalRecords(calRecords);
            uploader.uploadDeviceStatus(deviceStatus);
        }
        return true;
    }

    protected List<BaseUploader> getUploaders() {
        return uploaders;
    }
}
