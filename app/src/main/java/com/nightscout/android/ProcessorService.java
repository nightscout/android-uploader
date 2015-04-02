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
import com.nightscout.android.preferences.PreferenceKeys;
import com.nightscout.android.upload.Uploader;
import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.BusProvider;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.mqtt.MqttEventMgr;
import com.nightscout.core.mqtt.MqttPinger;
import com.nightscout.core.mqtt.MqttTimer;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.utils.RestUriUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Arrays;

public class ProcessorService extends Service {

    public static final String ACTION_UPLOAD = "org.nightscout.uploader.UPLOAD";
    private static final String TAG = ProcessorService.class.getSimpleName();
    private Uploader uploader;
    private Pebble pebble;
    private MqttEventMgr mqttManager;
    private AndroidEventReporter reporter;
    private AndroidPreferences preferences;
    private Bus bus = BusProvider.getInstance();
    private boolean initalized = false;
    private boolean uploadersDefined = false;
    private Tracker tracker;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private final IBinder mBinder = new LocalBinder();
    private boolean lastUploadStatus = false;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Starting processor service");
        preferences = new AndroidPreferences(getApplicationContext());
        reporter = AndroidEventReporter.getReporter(getApplicationContext());
        pebble = new Pebble(getApplicationContext());
        pebble.setUnits(preferences.getPreferredUnits());
        pebble.setPwdName(preferences.getPwdName());
        bus.register(this);
        uploader = new Uploader(getApplicationContext(), preferences);
        setupMqtt();
//        if (preferences.isMqttEnabled()) {
//            mqttManager = setupMqtt(preferences.getMqttUser(), preferences.getMqttPass(), preferences.getMqttEndpoint());
//            if (mqttManager != null) {
//                mqttManager.connect();
//                initalized = true;
//            }
//        }
        tracker = ((Nightscout) getApplicationContext()).getTracker();
        reportUploadMethods();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                String[] prefKeys = {PreferenceKeys.API_UPLOADER_ENABLED, PreferenceKeys.API_URIS, PreferenceKeys.MONGO_UPLOADER_ENABLED,
                        PreferenceKeys.MONGO_URI, PreferenceKeys.MONGO_COLLECTION, PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION};
                if (Arrays.asList(prefKeys).contains(key)) {
                    uploader = new Uploader(getApplicationContext(), preferences);
                    for (BaseUploader ul : uploader.getUploaders()) {
                        Log.d(TAG, "defined: " + ul.getIdentifier());
                    }
                } else {
                    Log.d(TAG, "Meh... something uninteresting changed");
                }
                // Assume that MQTT already has the information needed and set it up.
                if (key.equals(PreferenceKeys.MQTT_ENABLED)) {
                    if (mqttManager != null && mqttManager.isConnected()) {
                        mqttManager.setShouldReconnect(false);
                        mqttManager.close();
                        mqttManager.setShouldReconnect(true);
                    }
                    setupMqtt();
                }

                // Assume that MQTT is already enabled and the MQTT endpoint and credentials are just changing
                prefKeys = new String[]{PreferenceKeys.MQTT_ENDPOINT, PreferenceKeys.MQTT_PASS, PreferenceKeys.MQTT_USER};
                if (preferences.isMqttEnabled() && Arrays.asList(prefKeys).contains(key)) {
                    Log.d(TAG, "MQTT change detected. Restarting MQTT");
                    if (mqttManager.isConnected()) {
                        mqttManager.setShouldReconnect(false);
                        mqttManager.close();
                        mqttManager.setShouldReconnect(true);
                    }
                    setupMqtt();
                }
                if (key.equals(PreferenceKeys.PREFERRED_UNITS)) {
                    pebble.config(preferences.getPwdName(), preferences.getPreferredUnits(), getApplicationContext());
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    public boolean getLastUploadStatus() {
        return lastUploadStatus;
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

    // TODO setup a preferences listener to reconnect to MQTT after a settings change.

    private boolean verifyUploaders() {
        if (!preferences.isMqttEnabled() && !preferences.isMongoUploadEnabled() && !preferences.isRestApiEnabled()) {
            reporter.report(EventType.UPLOADER, EventSeverity.WARN, getApplicationContext().getString(R.string.no_uploaders));
            return false;
//            bus.post(false);
        }
        return true;
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
        bus.unregister(this);
    }

    public void setupMqtt() {
        if (preferences.isMqttEnabled()) {
            mqttManager = setupMqttConnection(preferences.getMqttUser(), preferences.getMqttPass(), preferences.getMqttEndpoint());
            if (mqttManager != null) {
                mqttManager.setShouldReconnect(true);
                mqttManager.connect();
                initalized = true;
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
            mqttOptions.setCleanSession(false);
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
        uploadersDefined = verifyUploaders();
        // TODO - Eventually collapse all of these to a single loop to process the download.
        // Requires a single interface for everything to determine how to process a download.
        boolean uploadSuccess = false;
        if (uploader != null) {
            uploadSuccess = uploader.upload(download, 1);
        }
        if (download.sgv.size() <= 0) {
            return;
        }
        long refTime = DateTime.parse(download.download_timestamp).getMillis();
        long rcvrTime = download.receiver_system_time_sec;
        EGVRecord recentRecord = new EGVRecord(download.sgv.get(download.sgv.size() - 1), rcvrTime, refTime);
        if (pebble != null && pebble.isConnected()) {
            pebble.sendDownload(recentRecord.getReading(), recentRecord.getTrend(), recentRecord.getWallTime().getMillis(), getApplicationContext());
        }
        if (preferences.isMqttEnabled()) {
            if (mqttManager != null && mqttManager.isConnected()) {
                Log.d(TAG, "Publishing");
                mqttManager.publish(download.toByteArray(), "/downloads/protobuf");
                preferences.setLastEgvMqttUpload(download.sgv.get(download.sgv.size() - 1).sys_timestamp_sec);
                preferences.setLastMeterMqttUpload(download.meter.get(download.meter.size() - 1).sys_timestamp_sec);
                preferences.setLastSensorMqttUpload(download.sensor.get(download.sensor.size() - 1).sys_timestamp_sec);
                preferences.setLastCalMqttUpload(download.cal.get(download.cal.size() - 1).sys_timestamp_sec);
            } else {
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR, "Expected MQTT to be connected but it is not");
                uploadSuccess &= false;
            }
        } else {
            initalized = true;
        }
        ProcessorResponse response = new ProcessorResponse();
        Log.d(TAG, "uploadSuccess: " + uploadSuccess);
        Log.d(TAG, "initalized: " + initalized);
        Log.d(TAG, "areAllUploadersInitalized: " + uploader.areAllUploadersInitalized());
        Log.d(TAG, "uploadersDefined: " + uploadersDefined);
        response.success = uploadSuccess && initalized && uploader.areAllUploadersInitalized() && uploadersDefined;
        lastUploadStatus = response.success;
        bus.post(response);
//        bus.post(uploadSuccess && initalized && uploader.areAllUploadersInitalized());
    }

    public class ProcessorResponse {
        public boolean success;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
