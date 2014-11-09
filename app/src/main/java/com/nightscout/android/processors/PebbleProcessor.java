package com.nightscout.android.processors;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.nightscout.android.R;
import com.nightscout.android.devices.Download;
import com.nightscout.android.dexcom.G4Download;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class PebbleProcessor extends AbstractProcessor {
    private static final String TAG = PebbleProcessor.class.getSimpleName();
    private static final UUID PEBBLEAPP_UUID = UUID.fromString("e5664826-0ab7-4a2a-9a81-1220e78bae5c");
    private static final int ICON_KEY=0;
    private static final int BG_KEY=1;
    private static final int READTIME=2;
    private static final int ALERT=3;
    private static final int TIME=4;
    private static final int DELTA=5;
    private static final int BATT=6;
    private static final int NAME=7;
    private ArrayList<G4Download> downloadObjects=new ArrayList<G4Download>();
    private int lowThreshold;
    private int highThreshold;

    public PebbleProcessor(String n, int deviceID, Context context) {
        super(deviceID, context, "pebble_monitor");
        init();
        Resources res=context.getResources();

        lowThreshold=Integer.valueOf(sharedPref.getString("device_" + deviceID + "_warn_low_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_low))));
        highThreshold=Integer.valueOf(sharedPref.getString("device_" + deviceID + "_warn_high_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_high))));
    }

    protected void init(){
        PebbleKit.registerReceivedDataHandler(context, new PebbleKit.PebbleDataReceiver(PEBBLEAPP_UUID) {
            @Override
            public void receiveData(final Context mContext, final int transactionId, final PebbleDictionary data) {
                Log.i(TAG, "Received value=" + data.getString(0) + " for key: 0");
                Log.d(TAG," Data size="+data.size());
                PebbleKit.sendAckToPebble(mContext, transactionId);
                if (data.getString(0) !=null && data.getString(0).equals("lastdownload")) {
                    if (downloadObjects.size() > 0){
                        PebbleDictionary out=buildMsg(downloadObjects.get(downloadObjects.size()-1));
                        PebbleKit.sendDataToPebble(context,PEBBLEAPP_UUID,out);
                    }

                }

            }
        });
    }

    @Override
    public boolean process(G4Download d) {
        downloadObjects.add(d);
        if (downloadObjects.size()>2)
            downloadObjects.remove(0);

        boolean connected = PebbleKit.isWatchConnected(context);
        Log.i(TAG, "Pebble is " + (connected ? "connected" : "not connected"));
        if (connected){
            PebbleDictionary data=buildMsg(d);
            Log.d(TAG,"Trying to send message to pebble.. Here goes nothing");
            PebbleKit.sendDataToPebble(context,PEBBLEAPP_UUID,data);
        }
        return true;
    }

    protected PebbleDictionary buildMsg(G4Download dl){
        String delta="";
        if (downloadObjects.size()>1) {
            int deltaInt=downloadObjects.get(1).getLastEGV() - downloadObjects.get(0).getLastEGV();
            if (deltaInt>0)
                delta="+";
            delta += String.valueOf(deltaInt);
        }
        PebbleDictionary data=new PebbleDictionary();
        Log.d(TAG,"Building the dictionary");
        byte alert=0x00;
        Log.d(TAG, "Trend arrow: " + dl.getLastEGVTrend().friendlyTrendName());
        data.addString(ICON_KEY, dl.getLastEGVTrend().friendlyTrendName());
        data.addString(BG_KEY, String.valueOf(dl.getLastEGV()));
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        int rawOffset=tz.getRawOffset()/1000;
        if (tz.inDaylightTime(new Date()))
            rawOffset+=3600; // 1 hour for daylight time if it is observed
        long readTimeUTC=dl.getLastEGVTimestamp()/1000;
        long readTimeLocal=readTimeUTC+rawOffset;

        data.addString(READTIME, String.valueOf(readTimeLocal));
        if (dl.getLastEGV() > highThreshold)
            alert = 0x02;
        if (dl.getLastEGV() < lowThreshold)
            alert = 0x01;
        data.addUint8(ALERT, alert);
        long epochUTC=new Date().getTime()/1000;
        long epochlocalSeconds=(epochUTC+rawOffset);
        String tm = String.valueOf(epochlocalSeconds);
        Log.d(TAG, "tm=" + tm);
        data.addString(TIME, tm);
        Log.d(TAG, "Delta=" + delta);
        data.addString(DELTA, delta);
        data.addString(BATT, String.valueOf(dl.getUploaderBattery()));
        data.addString(NAME, getName());
        return data;
    }
}