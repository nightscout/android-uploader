package com.nightscout.android.processors;

import android.content.Context;
import android.util.Log;

import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.mqtt.MQTTMgr;
import com.nightscout.android.mqtt.MQTTMgrObserverInterface;
import com.nightscout.android.protobuf.SGV;

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
            G4Download dl=filterDownload(new G4Download(d));
            mqttMgr.publish(dl.toCookieProtobuf().toByteArray(), PROTOBUF_DOWNLOAD_TOPIC);
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

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = " ".toCharArray()[0];
        }
        return new String(hexChars);
    }

}
