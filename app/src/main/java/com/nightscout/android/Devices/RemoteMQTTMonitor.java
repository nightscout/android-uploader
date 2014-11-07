package com.nightscout.android.Devices;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.mqtt.MQTTMgr;
import com.nightscout.android.mqtt.MQTTMgrObserverInterface;
import com.nightscout.android.processors.MQTTUploadProcessor;
import com.nightscout.android.protobuf.SGV;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;


public class RemoteMQTTMonitor extends AbstractPushDevice implements MQTTMgrObserverInterface {
    private static final String TAG = RemoteMQTTMonitor.class.getSimpleName();

    private MQTTMgr mqttMgr;

    public RemoteMQTTDevice(String n, int deviceID, Context appContext) {
        super(n, deviceID, appContext, "RemoteMQTT");
        setDeviceType("Remote MQTT");
        setRemote(true);
    }

    @Override
    public int getDeviceBattery() throws IOException {
        return 0;
    }

    @Override
    public void start() {
        super.start();
        connect();
    }

    @Override
    public void connect() {
        Log.d(TAG,"Connect started");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
        String usr=sharedPref.getString(deviceIDStr+"_mqtt_user","");
        String pw=sharedPref.getString(deviceIDStr+"_mqtt_pass","");
        mqttMgr=new MQTTMgr(context,usr,pw,getDeviceIDStr());

        new Thread(new Runnable() {
            @Override
            public void run() {
                mqttMgr.initConnect(url);
                Log.d(TAG, "Subscribe start");
//        mqttMgr.subscribe("/entries/sgv", "/uploader");
                mqttMgr.subscribe("/entries/sgv", MQTTUploadProcessor.PROTOBUF_DOWNLOAD_TOPIC);
                Log.d(TAG,"Connect ended");
            }
        }).start();

        mqttMgr.registerObserver(this);
    }

    @Override
    public void disconnect() {
        if (mqttMgr==null)
            return;
        mqttMgr.disconnect();
        mqttMgr.close();
        mqttMgr.unregisterObserver(this);
        mqttMgr=null;
    }

    @Override
    public void stop() {
        super.stop();
        disconnect();
    }

    @Override
    public void onMessage(String topic, MqttMessage msg) {
        Log.d(TAG,"Received message for topic "+topic);
        if (topic.equals("/downloads/protobuf")){
            try {
                SGV.CookieMonsterG4Download protoBufDownload=SGV.CookieMonsterG4Download.parseFrom(msg.getPayload());
                G4Download downloadObject= new G4Download(protoBufDownload);

                setLastDownloadObject(downloadObject);
                onDownload(downloadObject);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisconnect() {
        lastDownloadObject.setStatus(DownloadStatus.REMOTEDISCONNECTED);
    }
}