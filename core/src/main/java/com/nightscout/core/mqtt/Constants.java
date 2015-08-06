package com.nightscout.core.mqtt;

public class Constants {
    public static final int QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )
    public static final int QOS_2 = 2; // QOS Level 2 ( Delivery only once with confirmation with handshake )
    public static final int KEEPALIVE_INTERVAL = 150000;
    public static final byte[] MQTT_KEEP_ALIVE_MESSAGE = {0}; // Keep Alive message to send
    public static final int MQTT_KEEP_ALIVE_QOS = QOS_0; // Default Keepalive QOS
    public static final boolean MQTT_CLEAN_SESSION = true;


    public static final String RECONNECT_INTENT_FILTER = "com.nightscout.android.MQTT_RECONNECT";
    public static final String KEEPALIVE_INTENT_FILTER = "com.nightscout.android.MQTT_KEEPALIVE";
}
