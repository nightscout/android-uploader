package com.nightscout.android.analyzers;

import android.content.Context;

import com.nightscout.android.devices.Constants;
import com.nightscout.android.dexcom.G4Constants;
import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.dexcom.SpecialBGValue;
import com.nightscout.android.processors.AlertLevel;
import com.nightscout.android.processors.AlertMessage;

public class G4DownloadAnalyzer extends CGMDownloadAnalyzer {

    public G4DownloadAnalyzer(G4Download dl, Context context) {
        super(dl,context);
    }

    @Override
    public AnalyzedDownload analyze() {
        super.analyze();
        checkSpecialValues();
        return this.downloadObject;
    }


    protected void checkSpecialValues() {
        int egvValue = downloadObject.getLastEGV();

        if (egvValue< G4Constants.MINEGV){
            SpecialBGValue specialValue=SpecialBGValue.getEGVSpecialValue(egvValue);
            AlertMessage alertMessage;
            if (specialValue!=null) {
                alertMessage=new AlertMessage(AlertLevel.WARN, specialValue.toString(),Condition.SPECIALVALUE);
            }else{
                alertMessage=new AlertMessage(AlertLevel.WARN,"Unknown special value received from G4",Condition.DEVICEMSGS);
            }
            downloadObject.addMessage(alertMessage);
        }
    }

    @Override
    protected void checkThresholdholds(){
        int egv=downloadObject.getLastEGV();
        if (egv>= G4Constants.MAXEGV) {
            downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL, "EGV is too high to read",Condition.CRITICALHIGH));
            return;
        } else if (egv<=G4Constants.MINEGV && ! SpecialBGValue.isSpecialValue(egv)) {
            downloadObject.addMessage(new AlertMessage(AlertLevel.CRITICAL, "EGV is too low to read",Condition.CRITICALLOW));
            return;
        }

        super.checkThresholdholds();
    }

    @Override
    protected void correlateMessages(){
        // Special values are < 39.
        Condition[] conditions={Condition.CRITICALLOW,Condition.WARNLOW};
        if (downloadObject.getConditions().contains(Condition.SPECIALVALUE)){
            downloadObject.removeMessageByCondition(Condition.CRITICALLOW,Condition.WARNLOW);
        }
        super.correlateMessages();
    }

}