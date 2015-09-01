package com.nightscout.android;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.events.reporters.AndroidEventReporter;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.model.G4Download;

public class ProcessorService extends Service {

    private static final String TAG = ProcessorService.class.getSimpleName();
    private Pebble pebble;
    private AndroidEventReporter reporter;
    private AndroidPreferences preferences;
    private Tracker tracker;
    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Starting processor service");
        preferences = new AndroidPreferences(getApplicationContext());
        reporter = AndroidEventReporter.getReporter(getApplicationContext());
        if (preferences.isCampingModeEnabled()) {
            pebble = new Pebble(getApplicationContext());
            pebble.setUnits(preferences.getPreferredUnits());
            pebble.setPwdName(preferences.getPwdName());
        }
        tracker = ((Nightscout) getApplicationContext()).getTracker();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.OnSharedPreferenceChangeListener
            prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                                        if (key.equals(getString(R.string.preferred_units)) && preferences
                        .isCampingModeEnabled()) {
                        pebble.config(preferences.getPwdName(), preferences.getPreferredUnits(),
                                      getApplicationContext());
                    }
                }
            };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    /*
    public void reportUploadMethods() {
        if (preferences.isRestApiEnabled()) {
            for (String url : preferences.getRestApiBaseUris()) {
                String apiVersion = (RestUriUtils.isV1Uri(URI.create(url))) ? "WebAPIv1" : "Legacy WebAPI";
                tracker.send(new HitBuilders.EventBuilder("Upload", apiVersion).build());
            }
        }
        if (preferences.isMongoUploadEnabled()) {
            tracker.send(new HitBuilders.EventBuilder("Upload", "Mongo").build());
        }
        if (preferences.isMqttEnabled()) {
            tracker.send(new HitBuilders.EventBuilder("Upload", "MQTT").build());
        }
    }*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pebble != null) {
            pebble.close();
        }
    }

    private void reportDeviceTypes() {
        tracker.send(new HitBuilders.EventBuilder("sync", preferences.getDeviceType().name()).build());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public class LocalBinder extends Binder {
        public ProcessorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ProcessorService.this;
        }
    }

    public void incomingData(G4Download download) {
        reportDeviceTypes();
        if (download.sgv.size() <= 0) {
            return;
        }

        /*if (pebble != null && pebble.isConnected()) {
            pebble.sendDownload(recentRecord.getReading(), recentRecord.getTrend(), recentRecord.getWallTime().getMillis(), getApplicationContext());
        }
        if (preferences.isMqttEnabled()) {
            if (mqttManager != null && mqttManager.isConnected()) {
                Log.d(TAG, "Publishing");
                mqttManager.publish(filteredDownload.toByteArray(), "/downloads/protobuf");
                if (filteredDownload.sgv.size() > 0) {
                    Log.d(TAG, "Publishing " + filteredDownload.sgv.size() + " sgv records");
                    preferences.setLastEgvMqttUpload(filteredDownload.sgv.get(filteredDownload.sgv.size() - 1).sys_timestamp_sec);
                }
                if (filteredDownload.meter.size() > 0) {
                    Log.d(TAG, "Publishing " + filteredDownload.meter.size() + " meter records");
                    preferences.setLastMeterMqttUpload(filteredDownload.meter.get(filteredDownload.meter.size() - 1).sys_timestamp_sec);
                }
                if (filteredDownload.sensor.size() > 0) {
                    Log.d(TAG, "Publishing " + filteredDownload.sensor.size() + " sensor records");
                    preferences.setLastSensorMqttUpload(filteredDownload.sensor.get(filteredDownload.sensor.size() - 1).sys_timestamp_sec);
                }
                if (filteredDownload.cal.size() > 0) {
                    Log.d(TAG, "Publishing " + filteredDownload.cal.size() + " cal records");
                    preferences.setLastCalMqttUpload(filteredDownload.cal.get(filteredDownload.cal.size() - 1).sys_timestamp_sec);
                }
                if (filteredDownload.insert.size() > 0) {
                    Log.d(TAG, "Publishing " + filteredDownload.insert.size() + " insert records");
                    preferences.setLastInsMqttUpload(filteredDownload.cal.get(filteredDownload.insert.size() - 1).sys_timestamp_sec);
                }
            } else {
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR, "Expected MQTT to be connected but it is not");
                uploadSuccess &= false;
            }*/
    }


    public class ProcessorResponse {
        public boolean success;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
