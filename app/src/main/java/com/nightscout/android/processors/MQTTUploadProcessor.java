package com.nightscout.android.processors;

import android.content.Context;

import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.mqtt.MQTTMgr;
import com.nightscout.android.mqtt.MQTTMgrObserverInterface;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTUploadProcessor extends AbstractProcessor implements MQTTMgrObserverInterface {
    private static final String TAG = MQTTUploadProcessor.class.getSimpleName();
    public static final String PROTOBUF_DOWNLOAD_TOPIC="/downloads/protobuf";

    protected MQTTMgr mqttMgr;

    public MQTTUploadProcessor(Context context) {
        super(context,"mqtt_uploader");
        String url=sharedPref.getString("cloud_storage_mqtt_endpoint","");
        String usr=sharedPref.getString("cloud_storage_mqtt_user","");
        String pw=sharedPref.getString("cloud_storage_mqtt_pass","");
        mqttMgr=new MQTTMgr(this.context,usr,pw);
        mqttMgr.initConnect(url);
        mqttMgr.registerObserver(this);
    }

    @Override
    public boolean process(G4Download d) {
        try {
            mqttMgr.publish(d.toCookieProtobuf().toByteArray(), PROTOBUF_DOWNLOAD_TOPIC);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    @Override
    public void stop() {
        mqttMgr.disconnect();
        mqttMgr.unregisterObserver(this);
    }

    @Override
    public void start() {

    }

    @Override
    public void onMessage(String topic, MqttMessage message) {

    }

    @Override
    public void onDisconnect() {
    }
}
