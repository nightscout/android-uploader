package com.nightscout.android.wearables;

import android.content.Context;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.utils.GlucoseReading;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.UUID;

public class Pebble {
    //    CGM_ICON_KEY = 0x0,		// TUPLE_CSTRING, MAX 2 BYTES (10)
    //    CGM_BG_KEY = 0x1,		// TUPLE_CSTRING, MAX 4 BYTES (253 OR 22.2)
    //    CGM_TCGM_KEY = 0x2,		// TUPLE_INT, 4 BYTES (CGM TIME)
    //    CGM_TAPP_KEY = 0x3,		// TUPLE_INT, 4 BYTES (APP / PHONE TIME)
    //    CGM_DLTA_KEY = 0x4,		// TUPLE_CSTRING, MAX 5 BYTES (BG DELTA, -100 or -10.0)
    //    CGM_UBAT_KEY = 0x5,		// TUPLE_CSTRING, MAX 3 BYTES (UPLOADER BATTERY, 100)
    //    CGM_NAME_KEY = 0x6		// TUPLE_CSTRING, MAX 9 BYTES (Christine)
    public static final UUID PEBBLEAPP_UUID = UUID.fromString("2c3f5ab3-7506-44e7-b8d0-2c63de32e1ec");
    public static final int ICON_KEY = 0;
    public static final int BG_KEY = 1;
    public static final int RECORD_TIME_KEY = 2;
    public static final int PHONE_TIME_KEY = 3;
    public static final int BG_DELTA_KEY = 4;
    public static final int UPLOADER_BATTERY_KEY = 5;
    public static final int NAME_KEY = 6;
    private static final String TAG = Pebble.class.getSimpleName();
    private Context context;
    private PebbleDictionary currentReading;
    private GlucoseUnit units = GlucoseUnit.MGDL;
    private String pwdName = "";
    private GlucoseReading lastReading;
    private GlucoseReading lastDelta;
    private TrendArrow lastTrend = TrendArrow.NONE;
    private long lastRecordTime = 0;
    private PebbleKit.PebbleDataReceiver dataReceiver = new PebbleKit.PebbleDataReceiver(PEBBLEAPP_UUID) {
        @Override
        public void receiveData(final Context mContext, final int transactionId, final PebbleDictionary data) {
            Log.d(TAG, "Received query. data: " + data.size());
            PebbleKit.sendAckToPebble(mContext, transactionId);
            sendDownload(currentReading);
        }
    };

    public Pebble(Context context) {
        this.context = context;
        currentReading = null;
        init();
    }

    public void close() {
        context.unregisterReceiver(dataReceiver);
    }

    public PebbleDictionary buildDictionary(TrendArrow trend, String bgValue, int recordTime, int uploaderTimeSec,
                                            String delta, String uploaderBattery, String name) {
        PebbleDictionary dictionary = new PebbleDictionary();
        dictionary.addString(ICON_KEY, String.valueOf(trend.ordinal()));
        dictionary.addString(BG_KEY, bgValue);
        dictionary.addUint32(RECORD_TIME_KEY, recordTime);
        dictionary.addUint32(PHONE_TIME_KEY, uploaderTimeSec);
        dictionary.addString(BG_DELTA_KEY, delta);
        dictionary.addString(UPLOADER_BATTERY_KEY, uploaderBattery);
        // TODO does this need to be set every time?
        dictionary.addString(NAME_KEY, name);
        return dictionary;
    }

    public void sendDownload(GlucoseReading reading, TrendArrow trend, long recordTime, Context cntx) {
        sendDownload(reading, trend, recordTime, cntx, false);
    }

    public void sendDownload(GlucoseReading reading, TrendArrow trend, long recordTime, Context cntx, boolean resend) {
        GlucoseReading delta = new GlucoseReading(0, GlucoseUnit.MGDL);

        if (currentReading != null) {
            delta = reading.subtract(lastReading);
        }

        if (resend) {
            delta = lastDelta;
        }
        String deltaStr = "";
        if (delta.asMgdl() > 0) {
            deltaStr += "+";
        }
        deltaStr += delta.asStr(units);
        String bgStr = reading.asStr(units);

        lastRecordTime = recordTime;
        lastReading = reading;
        lastTrend = trend;
        lastDelta = delta;
        recordTime = DateTimeZone.getDefault().convertUTCToLocal(recordTime);
        int batLevel = AndroidUploaderDevice.getUploaderDevice(cntx).getBatteryLevel();
        PebbleDictionary dictionary = buildDictionary(trend, bgStr, (int) (recordTime / 1000),
                (int) (DateTimeZone.getDefault().convertUTCToLocal(new DateTime().getMillis()) / 1000), deltaStr,
                String.valueOf(batLevel), pwdName);
        currentReading = dictionary;
        sendDownload(dictionary);
    }

    public void resendDownload(Context cntx) {
        if (currentReading != null) {
            sendDownload(lastReading, lastTrend, lastRecordTime, cntx, true);
        }
    }

    public void sendDownload(PebbleDictionary dictionary) {
        if (PebbleKit.isWatchConnected(context)) {
            if (dictionary != null && context != null) {
                Log.d(TAG, "Sending data to pebble");
                PebbleKit.sendDataToPebble(context, PEBBLEAPP_UUID, dictionary);
            }
        }
    }

    private void init() {
        PebbleKit.registerReceivedDataHandler(context, dataReceiver);
    }

    public void config(String pwdName, GlucoseUnit units, Context cntx) {
        boolean changed = !this.pwdName.equals(pwdName) || this.units != units;
        if (changed) {
            setPwdName(pwdName);
            setUnits(units);
            resendDownload(cntx);
        }
    }

    public void setPwdName(String pwdName) {
        this.pwdName = pwdName;
    }

    public void setUnits(GlucoseUnit units) {
        this.units = units;
    }
}
