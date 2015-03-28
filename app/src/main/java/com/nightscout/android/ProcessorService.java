package com.nightscout.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.mqtt.AndroidMqttPinger;
import com.nightscout.android.mqtt.AndroidMqttTimer;
import com.nightscout.android.preferences.AndroidPreferences;
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
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;

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


    @Override
    public void onCreate() {
        super.onCreate();
        preferences = new AndroidPreferences(getApplicationContext());
        reporter = AndroidEventReporter.getReporter(getApplicationContext());
        pebble = new Pebble(getApplicationContext());
        pebble.setUnits(preferences.getPreferredUnits());
        pebble.setPwdName(preferences.getPwdName());

        if (!preferences.isMqttEnabled() && !preferences.isMongoUploadEnabled() && !preferences.isRestApiEnabled()) {
            reporter.report(EventType.UPLOADER, EventSeverity.WARN, getApplicationContext().getString(R.string.no_uploaders));
            bus.post(false);
        }

        bus.register(this);
        uploader = new Uploader(getApplicationContext(), preferences);
        if (preferences.isMqttEnabled()) {
            mqttManager = setupMqtt(preferences.getMqttUser(), preferences.getMqttPass(), preferences.getMqttEndpoint());
            if (mqttManager != null) {
                mqttManager.connect();
                initalized = true;
            }
        }
    }
    // TODO setup a preferences listener to reconnect to MQTT after a settings change.


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pebble != null) {
            pebble.close();
        }
        if (mqttManager != null) {
            mqttManager.close();
        }
    }

    public MqttEventMgr setupMqtt(String user, String pass, String endpoint) {
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


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Subscribe
    public void incomingData(G4Download download) {

        // TODO - Eventually collapse all of these to a single loop to process the download.
        // Requires a single interface for everything to determine how to process a download.
        boolean uploadSuccess = false;
        if (uploader != null) {
            uploadSuccess = uploader.upload(download);
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
        }
        bus.post(uploadSuccess && initalized && uploader.areAllUploadersInitalized());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
