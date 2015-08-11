package com.nightscout.android;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.mqtt.AndroidMqttPinger;
import com.nightscout.android.mqtt.AndroidMqttTimer;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.upload.Uploader;
import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.mqtt.MqttEventMgr;
import com.nightscout.core.mqtt.MqttPinger;
import com.nightscout.core.mqtt.MqttTimer;
import com.nightscout.core.utils.RestUriUtils;
import com.squareup.otto.Subscribe;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URI;
import java.util.Arrays;

public class ProcessorService extends Service {

    private static final String TAG = ProcessorService.class.getSimpleName();
    private Pebble pebble;
    private MqttEventMgr mqttManager;
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                setupMqtt();
            }
        }).start();
        tracker = ((Nightscout) getApplicationContext()).getTracker();
        reportUploadMethods();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.OnSharedPreferenceChangeListener
            prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    String[] prefKeys = {getApplicationContext().getString(R.string.rest_enable),
                                         getApplicationContext().getString(R.string.rest_uris),
                                         getApplicationContext().getString(R.string.mongo_enable),
                                         getApplicationContext().getString(R.string.mongo_uri),
                                         getApplicationContext()
                                             .getString(R.string.mongo_entries_collection),
                                         getApplicationContext()
                                             .getString(R.string.mongo_devicestatus_collection)};
                    if (Arrays.asList(prefKeys).contains(key)) {
                        uploader = new Uploader(getApplicationContext(), preferences);
                    } else if (key
                        .equals(getApplicationContext().getString(R.string.mqtt_enable))) {
                        // Assume that MQTT already has the information needed and set it up.
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                setupMqtt();
                                if (mqttManager != null && mqttManager.isConnected()) {
                                    mqttManager.setShouldReconnect(false);
                                    mqttManager.disconnect();
                                    mqttManager.setShouldReconnect(true);
                                }
                            }
                        }).start();
                    } else {
                        Log.d(TAG, "Meh... something uninteresting changed");
                    }

                    // Assume that MQTT is already enabled and the MQTT endpoint and credentials are just changing
                    prefKeys =
                        new String[]{getApplicationContext().getString(R.string.mqtt_endpoint),
                                     getApplicationContext().getString(R.string.mqtt_pass),
                                     getApplicationContext().getString(R.string.mqtt_user)};
                    if (preferences.isMqttEnabled() && Arrays.asList(prefKeys).contains(key)) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                setupMqtt();
                                Log.d(TAG, "MQTT change detected. Restarting MQTT");
                                if (mqttManager != null && mqttManager.isConnected()) {
                                    mqttManager.setShouldReconnect(false);
                                    mqttManager.disconnect();
                                    mqttManager.setShouldReconnect(true);
                                }
                            }
                        }).start();
                    }
                    if (key.equals(getString(R.string.preferred_units)) && preferences
                        .isCampingModeEnabled()) {
                        pebble.config(preferences.getPwdName(), preferences.getPreferredUnits(),
                                      getApplicationContext());
                    }
                }
            };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pebble != null) {
            pebble.close();
        }
        if (mqttManager != null) {
            mqttManager.setShouldReconnect(false);
            mqttManager.close();
        }
    }

    public void setupMqtt() {
        if (preferences.isMqttEnabled()) {
            mqttManager = setupMqttConnection(preferences.getMqttUser(), preferences.getMqttPass(), preferences.getMqttEndpoint());
            if (mqttManager != null) {
                Log.d(TAG, "Attempt to connect to MQTT");
                mqttManager.setShouldReconnect(true);
                mqttManager.connect();
            } else {
                Log.d(TAG, "MQTT is NULL");
            }
        }
    }

    public MqttEventMgr setupMqttConnection(String user, String pass, String endpoint) {
        if (user.equals("") || pass.equals("") || endpoint.equals("")) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR, "Unable to setup MQTT. Please check settings");
            return null;
        }
        try {
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(true);
            mqttOptions.setKeepAliveInterval(150000);
            mqttOptions.setUserName(user);
            mqttOptions.setPassword(pass.toCharArray());
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            MemoryPersistence dataStore = new MemoryPersistence();
            MqttClient client = new MqttClient(endpoint, androidId, dataStore);
            MqttPinger pinger = new AndroidMqttPinger(getApplicationContext(), 0, client, 150000);
            MqttTimer timer = new AndroidMqttTimer(getApplicationContext(), 0);
            return new MqttEventMgr(client, mqttOptions, pinger, timer, reporter);
        } catch (MqttException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR, "Unable to setup MQTT. Please check settings");
            return null;
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

    @Subscribe
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

        /*if (preferences.isBroadcastEnabled()) {
            // TODO: make this work for gap sync or a way to request for data (having a db for this would be great)
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("com.nightscout.action.PROCESS_RESPONSE");
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            SensorGlucoseValueEntry sgv = download.sgv.get(download.sgv.size() - 1);
            EGVRecord currentEGVrecord = new EGVRecord(sgv, rcvrTime, refTime);
            broadcastIntent.putExtra("nsSgv", currentEGVrecord.getBgMgdl());
            broadcastIntent.putExtra("nsTimestampMs", currentEGVrecord.getDisplayTime().getMillis());
            sendBroadcast(broadcastIntent);
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
