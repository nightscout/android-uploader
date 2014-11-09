package com.nightscout.android.analyzers;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nightscout.android.R;
import com.nightscout.android.devices.EGVLimits;
import com.nightscout.android.devices.EGVThresholds;
import com.nightscout.android.dexcom.DownloadStatus;
import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.dexcom.GlucoseUnit;
import com.nightscout.android.dexcom.Trend;
import com.nightscout.android.dexcom.Utils;
import com.nightscout.android.processors.AlertLevel;
import com.nightscout.android.processors.AlertMessage;

import java.util.Date;

public abstract class CGMDownloadAnalyzer extends AbstractDownloadAnalyzer {
    protected final int UPLOADERBATTERYWARN =30;
    protected final int UPLOADERBATTERYCRITICAL =20;
    protected final int DEVICEBATTERYWARN =30;
    protected final int DEVICEBATTERYCRITICAL =20;
    protected final int MAXRECORDAGE=310000;
    protected EGVLimits egvLimits=new EGVLimits();

    CGMDownloadAnalyzer(G4Download dl,Context context) {
        super(dl);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        EGVThresholds warnThreshold=new EGVThresholds();
        EGVThresholds criticalThreshold=new EGVThresholds();
        Resources res=context.getResources();

        warnThreshold.setLowThreshold(Integer.valueOf(sharedPref.getString("device_" + dl.getDeviceID() + "_low_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_low)))));
        warnThreshold.setHighThreshold(Integer.valueOf(sharedPref.getString("device_" + dl.getDeviceID() + "_high_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_high)))));

        criticalThreshold.setLowThreshold(Integer.valueOf(sharedPref.getString("device_" + dl.getDeviceID() + "_critical_low_threshold", String.valueOf(res.getInteger(R.integer.pref_default_critical_device_low)))));
        criticalThreshold.setHighThreshold(Integer.valueOf(sharedPref.getString("device_" + dl.getDeviceID() + "_critical_high_threshold", String.valueOf(res.getInteger(R.integer.pref_default_critical_device_high)))));

        egvLimits.setWarnThreshold(warnThreshold);
        egvLimits.setCriticalThreshold(criticalThreshold);
        Log.d(TAG,"Critical low threshold: "+egvLimits.getCriticalLow());
        Log.d(TAG,"Warn low threshold: "+egvLimits.getWarnLow());
        Log.d(TAG,"Warn high threshold: "+egvLimits.getWarnHigh());
        Log.d(TAG,"Critical high threshold: "+egvLimits.getCriticalHigh());
    }

    public AnalyzedDownload analyze() {
        super.analyze();
        checkDownloadStatus();
        checkRecordAge();
        checkUploaderBattery();
        checkCGMBattery();
        checkThresholdholds();
        checkLastRecordTime();
        checkGlobalAlerts();
        correlateMessages();
//        downloadObject.deDup();
        return this.downloadObject;
    }

    protected void checkGlobalAlerts(){
        for (AlertMessage alertMessage:StaticAlertMessages.getAlertMessages()){
            downloadObject.addMessage(alertMessage);
        }
    }

    protected void checkUploaderBattery(){
        // FIXME this breaks i18n possibilties
        String verb=(downloadObject.getConditions().contains(Condition.STALEDATA))?"was":"is";
        if (downloadObject.getUploaderBattery() < UPLOADERBATTERYCRITICAL){
            downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL, "Uploader battery " + verb + " critically low: " + downloadObject.getUploaderBattery(), Condition.UPLOADERCRITICALLOW));
        }else if (downloadObject.getUploaderBattery() < UPLOADERBATTERYWARN) {
            downloadObject.addMessage(new AlertMessage(AlertLevel.WARN, "Uploader battery "+verb+" low: " + downloadObject.getUploaderBattery(),Condition.UPLOADERLOW));
        }
    }

    protected void checkCGMBattery(){
        if (downloadObject.getDownloadStatus()== DownloadStatus.SUCCESS) {
            String verb=(downloadObject.getConditions().contains(Condition.STALEDATA))?"was":"is";
            if (downloadObject.getReceiverBattery() < DEVICEBATTERYCRITICAL) {
                downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL, "CGM battery " + verb + " critically low" + downloadObject.getReceiverBattery(), Condition.DEVICECRITICALLOW));
            }else if (downloadObject.getReceiverBattery() < DEVICEBATTERYWARN) {
                downloadObject.addMessage(new AlertMessage(AlertLevel.WARN, "CGM battery "+verb+" low: "+downloadObject.getReceiverBattery(),Condition.DEVICELOW));
            }
        }
    }

    protected void checkRecordAge(){
        Long recordAge= null;
        Long downloadAge=new Date().getTime() - downloadObject.getDownloadTimestamp();
        recordAge = new Date().getTime() - downloadObject.getLastEGVTimestamp();
        // Cutdown on clutter in the notification bar...
        // Only show the message for a missed reading or that the uploader isn't communicating
        if (recordAge > MAXRECORDAGE && downloadAge <= MAXRECORDAGE) {
            //FIXME if the record is over a month old then it will only show the date and it won't make sense to the user. Need to add a special condition.
            downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL, "CGM out of range/missed reading for " + Utils.getTimeString(new Date().getTime()-downloadObject.getLastEGVTimestamp()), Condition.MISSEDREADING));
        }
        if (downloadAge > MAXRECORDAGE)
            downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,"Uploader inactive for "+ Utils.getTimeString(new Date().getTime()-downloadObject.getDownloadTimestamp()),Condition.STALEDATA));
    }

    protected void checkDownloadStatus(){
        DownloadStatus status=downloadObject.getDownloadStatus();
        switch (status){
            case DEVICE_NOT_FOUND:
                downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,"No CGM device found",Condition.DEVICEDISCONNECTED));
                break;
            case IO_ERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,"Unable to read or write to the CGM",Condition.DOWNLOADFAILED));
                break;
            case NO_DATA:
                downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,"No data in download",Condition.NODATA));
                break;
            case APPLICATION_ERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,"Unknown application error",Condition.UNKNOWN));
                break;
            case UNKNOWN:
                downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,"Unknown error while trying to retrieve data from CGM",Condition.UNKNOWN));
                break;
            case REMOTE_DISCONNECTED:
                downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,"Unable to connect to remote devices",Condition.REMOTEDISCONNECTED));
            default:
                break;
        }
    }

    protected void checkThresholdholds(){
        int egv = downloadObject.getLastEGV();
        Trend trend = downloadObject.getLastEGVTrend();
        GlucoseUnit unit=downloadObject.getUnit();
        AlertLevel alertLevel=AlertLevel.INFO;
        Condition condition=Condition.INRANGE;
        if (egv > egvLimits.getCriticalHigh()) {
            condition = Condition.CRITICALHIGH;
            alertLevel = AlertLevel.CRITICAL;
        }else if (egv < egvLimits.getCriticalLow()) {
            condition = Condition.CRITICALLOW;
            alertLevel = AlertLevel.CRITICAL;
        }else if (egv > egvLimits.getWarnHigh()) {
            condition = Condition.WARNHIGH;
            alertLevel = AlertLevel.WARN;
        }else if (egv < egvLimits.getWarnLow()) {
            condition=Condition.WARNLOW;
            alertLevel = AlertLevel.WARN;
        }
        String preamble=(downloadObject.getConditions().contains(Condition.MISSEDREADING) || downloadObject.getConditions().contains(Condition.STALEDATA))?"Last reading: ":"";
        downloadObject.addMessage(new AlertMessage(alertLevel,preamble+egv+" "+unit+" "+trend,condition));
    }

    protected void checkLastRecordTime(){
        String msg=Utils.getTimeString(new Date().getTime()-downloadObject.getLastEGVTimestamp());
        downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL,msg,Condition.READINGTIME));
    }

    @Override
    protected void correlateMessages(){
        if (downloadObject.getConditions().contains(Condition.NODATA) && downloadObject.getConditions().contains(Condition.DEVICEDISCONNECTED))
            downloadObject.removeMessageByCondition(Condition.NODATA);
        if (downloadObject.getConditions().contains(Condition.MISSEDREADING))
            downloadObject.removeMessageByCondition(Condition.READINGTIME);
    }

}