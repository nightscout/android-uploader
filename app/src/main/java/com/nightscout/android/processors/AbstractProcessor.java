package com.nightscout.android.processors;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.GenericTimestampRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.dexcom.records.SensorRecord;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract public class AbstractProcessor implements ProcessorInterface {
    protected String name;
    protected Context context;
    protected SharedPreferences sharedPref;
    public static final String LAST_EGV_REC_SHAREDPREF="last_egv_systime";
    public static final String LAST_SENSOR_REC_SHAREDPREF="last_sensor_systime";
    public static final String LAST_METER_REC_SHAREDPREF="last_meter_systime";
    public static final String LAST_CAL_REC_SHAREDPREF="last_cal_systime";


    public AbstractProcessor(Context context,String name){
        this.name=name;
        this.context=context;
        sharedPref=PreferenceManager.getDefaultSharedPreferences(context);
    }

    protected <T> T filterRecords(List<? extends GenericTimestampRecord> list,String sharedKey){
        long lastRecord=sharedPref.getLong(name+sharedKey,0);
        ArrayList<? extends GenericTimestampRecord> result=new ArrayList<GenericTimestampRecord>(list);
        Iterator<? extends GenericTimestampRecord> iter=result.iterator();
        while (iter.hasNext()){
            GenericTimestampRecord record=iter.next();
            if (record.getSystemTimeSeconds() <= lastRecord) {
                iter.remove();
            }
        }
        SharedPreferences.Editor editor=sharedPref.edit();
        editor.putLong(name+sharedKey,list.get(list.size()-1).getSystemTimeSeconds());
        editor.apply();
        return (T) result;
    }

    protected G4Download filterDownload(G4Download download){
        G4Download.G4DownloadBuilder dlCopyBuilder=new G4Download.G4DownloadBuilder();
        G4Download dlCopy=dlCopyBuilder.setDownloadStatus(download.getDownloadStatus())
                .setEGVRecords((ArrayList<EGVRecord>) filterRecords(download.getEGVRecords(), LAST_EGV_REC_SHAREDPREF))
                .setSensorRecords((ArrayList<SensorRecord>) filterRecords(download.getSensorRecords(), LAST_SENSOR_REC_SHAREDPREF))
                .setMeterRecords((ArrayList<MeterRecord>) filterRecords(download.getMeterRecords(), LAST_METER_REC_SHAREDPREF))
                .setCalRecords((ArrayList<CalRecord>) filterRecords(download.getCalRecords(), LAST_CAL_REC_SHAREDPREF))
                .setDownloadTimestamp(download.getDownloadTimestamp())
                .setUnits(download.getUnits())
                .setReceiverBattery(download.getReceiverBattery())
                .setUploaderBattery(download.getUploaderBattery())
                .build();
        return dlCopy;
    }

    @Override
    public void stop() {

    }

    @Override
    public void start() {

    }

}