package com.nightscout.android.dexcom.records;

import com.nightscout.android.dexcom.Constants;
import com.nightscout.android.dexcom.Trend;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class EGVRecord extends GenericTimestampRecord {

    private int bGValue;
    private Trend trend;

    public EGVRecord(byte[] packet) {
        // system_time (UInt), display_time (UInt), glucose (UShort), trend_arrow (Byte), crc (UShort))
        super(packet);
        int eGValue = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        bGValue = eGValue & Constants.EGV_VALUE_MASK;
        int trendValue = ByteBuffer.wrap(packet).get(10) & Constants.EGV_TREND_ARROW_MASK;
        trend = Trend.values()[trendValue];
    }

    public EGVRecord(int bGValue,Trend trend,Date displayTime, Date systemTime){
        super(displayTime, systemTime);
        this.bGValue=bGValue;
        this.trend=trend;
    }

    public EGVRecord(int bGValue,Trend trend,long displayTime, long systemTime){
        super(displayTime,systemTime);
        this.bGValue=bGValue;
        this.trend=trend;
    }

    public int getBGValue() {
        return bGValue;
    }

    public Trend getTrend() {
        return trend;
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("sgv", getBGValue());
            obj.put("date", getDisplayTimeSeconds());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
