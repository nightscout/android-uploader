package com.nightscout.android.processors;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.MainActivity;
import com.nightscout.android.analyzers.AnalyzedDownload;
import com.nightscout.android.analyzers.Condition;
import com.nightscout.android.dexcom.G4Download;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

/**
 Copyright (c) 2014, Kevin Lee (klee24@gmail.com)
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:
 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice, this
 list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class AndroidNotificationProcessor extends AbstractProcessor {
    private static final String TAG = AndroidNotificationProcessor.class.getSimpleName();
    protected NotificationCompat.Builder notifBuilder;
    protected NotificationManagerCompat mNotifyMgr;
    final protected String monitorType="android notification";
    protected boolean isSilenced=false;
    protected Date timeSilenced;
    protected SnoozeReceiver snoozeReceiver;
    protected tickReceiver tickReceiver;
    protected screenStateReceiver screenStateReceiver;
    protected G4Download lastDownload;
    protected ArrayList<G4Download> previousDownloads=new ArrayList<G4Download>();
    protected final int MAXPREVIOUS=3;
    private PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
    //    private Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
    private Bitmap bm;
    private final int SNOOZEDURATION=1800000;
    private SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    // Good is defined as one that has all data that we need to convey our message
    private G4Download lastKnownGood;
    protected String phoneNum=null;
    protected AnalyzedDownload analyzedDownload;
    protected int tickCounter=0;
    protected boolean isTickCounterSyncd=false;
    protected boolean isScreenOn=true;
    protected WearableExtender wearableExtender;
    protected boolean runInit =true;
//    protected boolean firstReading = true;
//    protected NotificationCompat.WearableExtender wearableExtender;


    public void setNotifBuilder(NotificationCompat.Builder notifBuilder) {
        this.notifBuilder = notifBuilder;
    }

    AndroidNotificationMonitor(String name,int devID,Context contxt){
        super(name, devID, contxt, "android_notification");
        wearableExtender =
                new WearableExtender();

        Uri uri=Uri.parse(sharedPref.getString(deviceIDStr+Constants.CONTACTDATAURISUFFIX,Uri.EMPTY.toString()));

        if (! uri.equals(Uri.EMPTY)) {
            InputStream inputStream = openDisplayPhoto(uri);
            bm = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream),200,200,true);
        }
        else {
            bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
        }
        wearableExtender.setBackground(bm)
                .setHintHideIcon(true);
        init();
    }

    public void init(){
        Log.d(TAG,"Android notification monitor init called");
//        mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr = NotificationManagerCompat.from(context);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
        this.setNotifBuilder(new NotificationCompat.Builder(context)
                .setContentTitle(name)
                .setContentText("Monitor started. No data yet")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setSmallIcon(R.drawable.sandclock)
                .extend(wearableExtender)
                .setLargeIcon(bm));
        Notification notification = notifBuilder.build();
        mNotifyMgr.notify(deviceID, notification);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        this.setAllowVirtual(true);
        snoozeReceiver = new SnoozeReceiver();
        screenStateReceiver= new screenStateReceiver();
        tickReceiver = new tickReceiver();
        context.registerReceiver(snoozeReceiver, new IntentFilter(Constants.SNOOZE_INTENT));
        context.registerReceiver(screenStateReceiver,new IntentFilter(Intent.ACTION_SCREEN_ON));
        context.registerReceiver(screenStateReceiver,new IntentFilter(Intent.ACTION_SCREEN_OFF));
        context.registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        runInit =false;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    @Override
    public boolean process(G4Download dl) {
        if (previousDownloads!=null) {
            if (previousDownloads.size() > 0 && previousDownloads.get(previousDownloads.size() - 1).equals(dl)) {
                Log.i(TAG, "Received a duplicate reading. Ignoring it");
                return;
            } else {
                Log.d(TAG,"Download determined to be a new reading");
            }
            previousDownloads.add(dl);
            if (previousDownloads.size()>MAXPREVIOUS)
                previousDownloads.remove(0);
            Log.d(TAG,"Previous download size: "+previousDownloads.size());
        } else {
            Log.w(TAG, "No previous downloads?");
        }
        if (dl.getEgvArrayListRecords().size()>0)
            lastKnownGood = dl;
        // FIXME think this violates the open/closed principle... have to change this class if any new devices are added. Look into DI
        if (dl.getDriver().equals(G4Constants.DRIVER)) {
            AbstractDownloadAnalyzer downloadAnalyzer = new G4DownloadAnalyzer(dl, context);
            analyzedDownload = downloadAnalyzer.analyze();
        } else {
            Log.w(TAG,"Driver unknown by the analyzer: "+dl.getDriver());
        }

        if (isSilenced){
            long duration=new Date().getTime()-timeSilenced.getTime();
            // Snooze for 30 minutes at a time
            if (duration>SNOOZEDURATION) {
                Log.v(TAG,"Resetting snooze timer for "+deviceIDStr);
                isSilenced = false;
            }
            Log.v(TAG,"Alarm "+getName()+"("+deviceIDStr+"/"+monitorType+") is snoozed");
        }
        for (Condition condition:analyzedDownload.getConditions()){
            Log.v(TAG,"Condition: "+condition);
        }

        mNotifyMgr.notify(deviceID, buildNotification(analyzedDownload));
        if (! isTickCounterSyncd) {
            syncTickCounter();
            // Not sure this is what we want to do but I don't want to spend valuable CPU time/battery recalc'ing the sync when it will rarely if ever change.
            isTickCounterSyncd=true;
        }
        try {
            savelastSuccessDate(dl.getLastRecordReadingDate().getTime());
        } catch (NoDataException e) {
            Log.d(TAG,"No data in reading to get the last date");
        }
    }

    private void syncTickCounter(){
        try {
            // tickCounter represents the number of minutes since the previous analyzed download
            if (previousDownloads != null && previousDownloads.size() > 0) {
                int timeSinceDownload = (int) (new Date().getTime() - previousDownloads.get(previousDownloads.size() - 1).getLastRecordReadingDate().getTime());
                tickCounter = (timeSinceDownload / 1000) / 60;
                Log.d(TAG, "Setting tickCounter to " + tickCounter);
            }
        } catch (NoDataException e) {
//            e.printStackTrace();
        }
    }

    private Notification buildNotification(AnalyzedDownload dl){
        setDefaults(dl);
        setSound(dl);
        setTicker(dl);
        setActions(dl);
        setContent(dl);
        setIcon(dl);
//        notifBuilder.extend(wearableExtender);

        return notifBuilder.build();
    }
    protected void setSound(AnalyzedDownload dl){
        if (isSilenced)
            return;
        ArrayList<Condition> conditions=dl.getConditions();
        if ( !conditions.contains(Condition.CRITICALHIGH)
                && !conditions.contains(Condition.WARNHIGH)
                && !conditions.contains(Condition.WARNLOW)
                && !conditions.contains(Condition.CRITICALLOW)) {
            return;
        }

        Uri uri = Uri.EMPTY;
        // allows us to give some sounds higher precedence than others
        // I'm thinking I'll need to set a priority to the enums to break ties but this should work for now
        // If the loop isn't broken then the last condition in the queue wins
        boolean breakloop=false;
        for (Condition condition:conditions) {
            switch (condition) {
                case CRITICALHIGH:
                    uri = Uri.parse(sharedPref.getString(deviceIDStr + "_critical_high_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case WARNHIGH:
                    uri = Uri.parse(sharedPref.getString(deviceIDStr + "_high_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case INRANGE:
                    break;
                case WARNLOW:
                    uri = Uri.parse(sharedPref.getString(deviceIDStr + "_low_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case CRITICALLOW:
                    uri=Uri.parse(sharedPref.getString(deviceIDStr + "_critical_low_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case DOWNLOADFAILED:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICEDISCONNECTED:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case NODATA:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case STALEDATA:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case UPLOADERCRITICALLOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case UPLOADERLOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICECRITICALLOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICELOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICEMSGS:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case UNKNOWN:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                default:
                    break;
            }
            if (breakloop)
                break;
        }
        notifBuilder.setSound(uri);
    }

    public void setTicker(AnalyzedDownload dl){
        ArrayList<Condition> conditions=dl.getConditions();
        String message="";
        for (Condition condition:conditions) {
            try {
                if (condition == Condition.CRITICALHIGH || condition == Condition.WARNHIGH ||
                        condition == Condition.INRANGE || condition == Condition.WARNLOW ||
                        condition == Condition.CRITICALLOW) {
                    if (!message.equals(""))
                        message += "\n";
                    message += dl.getLastReading() + " " + dl.getUnit() + " " + dl.getLastTrend().toString();
                }
                switch (condition) {
                    case DOWNLOADFAILED:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Download failed";
                        break;
                    case DEVICEDISCONNECTED:
                        if (!message.equals(""))
                            message += "\n";
                        message += "CGM appears to be disconnected";
                        break;
                    case NODATA:
                        if (!message.equals(""))
                            message += "\n";
                        message += "No data available in download";
                        break;
                    case STALEDATA:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Data in download is over " + ((new Date().getTime() - dl.getLastRecordReadingDate().getTime()) / 1000) / 60;
                        break;
                    case UPLOADERCRITICALLOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Uploader is critically low: " + dl.getUploaderBattery();
                        break;
                    case UPLOADERLOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Uploader is low: " + dl.getUploaderBattery();
                        break;
                    case DEVICECRITICALLOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "CGM is critically low: " + dl.getUploaderBattery();
                        break;
                    case DEVICELOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "CGM is low: " + dl.getUploaderBattery();
                        break;
                    case DEVICEMSGS:
                        break;
                    case UNKNOWN:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Unidentified condition";
                        break;
                    case REMOTEDISCONNECTED:
                        if (!message.equals(""))
                            message+="\n";
                        message+= Condition.REMOTEDISCONNECTED.toString();
                        break;
                    case NONE:
                        break;
                }
            } catch (NoDataException e) {
                if (!message.equals(""))
                    message += "\n";
                message += "No data available in download";
            }
            notifBuilder.setTicker(message);
        }

    }

    private void setDefaults(AnalyzedDownload dl){
//        this.notifBuilder=new NotificationCompat.Builder(context)
        ArrayList<Condition> conditions=dl.getConditions();
        notifBuilder = new NotificationCompat.Builder(context);
        if ( conditions.contains(Condition.CRITICALHIGH)
                || conditions.contains(Condition.WARNHIGH)
                || conditions.contains(Condition.WARNLOW)
                || conditions.contains(Condition.CRITICALLOW)) {

            notifBuilder.setPriority(Notification.PRIORITY_MAX);
//            firstReading=false;
        } else {
            notifBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        }
        notifBuilder
                .setContentTitle(name)
                .setContentText("Default text")
                .setContentIntent(contentIntent)
//                .setOngoing(true)
                .extend(wearableExtender)
                .setSmallIcon(R.drawable.sandclock)
                .setLargeIcon(bm);
    }

    protected void setActions(AnalyzedDownload dl){
        ArrayList<Condition> conditions=dl.getConditions();
        for (Condition condition:conditions){
            if (!isSilenced) {
                if (condition == Condition.CRITICALHIGH
                        || condition == Condition.WARNHIGH
                        || condition == Condition.WARNLOW
                        || condition == Condition.CRITICALLOW) {
                    Intent snoozeIntent = new Intent(Constants.SNOOZE_INTENT);
                    snoozeIntent.putExtra("device", deviceIDStr);
                    PendingIntent snoozePendIntent = PendingIntent.getBroadcast(context, deviceID, snoozeIntent, 0);
                    // TODO make the snooze time configurable
                    // TODO dont hardcode this value - use i18n
                    String snoozeActionText="Snooze";
                    notifBuilder.addAction(R.drawable.ic_snooze, snoozeActionText, snoozePendIntent);
                }
            }
        }
        if (phoneNum!=null) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNum));
            // TODO switch over messages to standard i18n for localization.
            // TODO add tracking to this feature
            PendingIntent callPendingIntent = PendingIntent.getActivity(context, 30+deviceID, callIntent, 0);
            notifBuilder.addAction(android.R.drawable.sym_action_call, "Call", callPendingIntent);

            Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms",phoneNum,null));
            PendingIntent smsPendingIntent = PendingIntent.getActivity(context, 40 + deviceID, smsIntent, 0);
            notifBuilder.addAction(android.R.drawable.sym_action_chat,"Text",smsPendingIntent);
        }

    }

    protected void setContent(AnalyzedDownload dl){
        String msg="";
        for (AlertMessage message:dl.getMessages()){
            if (!msg.equals(""))
                msg+="\n";
            msg+=message.getMessage();
        }
        notifBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg);
    }

    protected void setIcon(AnalyzedDownload dl){
        int iconLevel=60;
        int state=0;
        int range=0;

        ArrayList<Condition> conditions=dl.getConditions();
        if (conditions.contains(Condition.DEVICELOW) || conditions.contains(Condition.DEVICECRITICALLOW) ||
                conditions.contains(Condition.UPLOADERLOW) || conditions.contains(Condition.UPLOADERCRITICALLOW) ||
                conditions.contains(Condition.DOWNLOADFAILED) || conditions.contains(Condition.DEVICEDISCONNECTED) ||
                conditions.contains(Condition.NODATA) || conditions.contains(Condition.STALEDATA) ||
                conditions.contains(Condition.UNKNOWN) || conditions.contains(Condition.REMOTEDISCONNECTED)){
            state=1;
        }
        if (conditions.contains(Condition.CRITICALHIGH) || conditions.contains(Condition.WARNHIGH)){
            range=1;
        }
        if (conditions.contains(Condition.CRITICALLOW) || conditions.contains(Condition.WARNLOW)){
            range=2;
        }
        try {
            Trend trend = dl.getLastTrend();
            iconLevel = trend.getVal() + (state * 10) + (range * 20);
        } catch (NoDataException e) {
            iconLevel=60;
        }
        int icon;
        switch (iconLevel){
            case (0):
                icon=R.drawable.smnoneinrange;
                break;
            case (1):
                icon=R.drawable.smdoubleupinrange;
                break;
            case (2):
                icon=R.drawable.smnoneinrange;
                break;
            case (3):
                icon=R.drawable.smfortyfiveupinrange;
                break;
            case (4):
                icon=R.drawable.smflatinrange;
                break;
            case (5):
                icon=R.drawable.smfortyfivedowninrange;
                break;
            case (6):
                icon=R.drawable.smdowninrange;
                break;
            case (7):
                icon=R.drawable.smdoubledowninrange;
                break;
            case (8):
                icon=R.drawable.smnoneinrange;
                break;
            case (9):
                icon=R.drawable.smnoneinrange;
                break;
            case (10):
                icon=R.drawable.smnoneerrorinrange;
                break;
            case (11):
                icon=R.drawable.smdoubleuperrorinrange;
                break;
            case (12):
                icon=R.drawable.smuperrorinrange;
                break;
            case (13):
                icon=R.drawable.smfortyfiveuperrorinrange;
                break;
            case (14):
                icon=R.drawable.smflaterrorinrange;
                break;
            case (15):
                icon=R.drawable.smfortyfivedownerrorinrange;
                break;
            case (16):
                icon=R.drawable.smdownerrorinrange;
                break;
            case (17):
                icon=R.drawable.smdoubledownerrorinrange;
                break;
            case (18):
                icon=R.drawable.smnoneerrorinrange;
                break;
            case (19):
                icon=R.drawable.smnoneerrorinrange;
                break;
            case (20):
                icon=R.drawable.smnonehigh;
                break;
            case (21):
                icon=R.drawable.smdoubleuphigh;
                break;
            case (22):
                icon=R.drawable.smuphigh;
                break;
            case (23):
                icon=R.drawable.smfortyfiveuphigh;
                break;
            case (24):
                icon=R.drawable.smflathigh;
                break;
            case (25):
                icon=R.drawable.smfortyfivedownhigh;
                break;
            case (26):
                icon=R.drawable.smdownhigh;
                break;
            case (27):
                icon=R.drawable.smdoubledownhigh;
                break;
            case (28):
                icon=R.drawable.smnonehigh;
                break;
            case (29):
                icon=R.drawable.smnonehigh;
                break;
            case (30):
                icon=R.drawable.smnoneerrorhigh;
                break;
            case (31):
                icon=R.drawable.smdoubleuperrorhigh;
                break;
            case (32):
                icon=R.drawable.smuperrorhigh;
                break;
            case (33):
                icon=R.drawable.smfortyfiveuperrorhigh;
                break;
            case (34):
                icon=R.drawable.smflaterrorhigh;
                break;
            case (35):
                icon=R.drawable.smfortyfivedownerrorhigh;
                break;
            case (36):
                icon=R.drawable.smdownerrorhigh;
                break;
            case (37):
                icon=R.drawable.smdoubledownerrorhigh;
                break;
            case (38):
                icon=R.drawable.smnoneerrorhigh;
                break;
            case (39):
                icon=R.drawable.smnoneerrorhigh;
                break;
            case (40):
                icon=R.drawable.smnonelow;
                break;
            case (41):
                icon=R.drawable.smdoubleuplow;
                break;
            case (42):
                icon=R.drawable.smuplow;
                break;
            case (43):
                icon=R.drawable.smfortyfiveuplow;
                break;
            case (44):
                icon=R.drawable.smflatlow;
                break;
            case (45):
                icon=R.drawable.smfortyfivedownlow;
                break;
            case (46):
                icon=R.drawable.smdownlow;
                break;
            case (47):
                icon=R.drawable.smdoubledownlow;
                break;
            case (48):
                icon=R.drawable.smnonelow;
                break;
            case (49):
                icon=R.drawable.smnonelow;
                break;
            case (50):
                icon=R.drawable.smnoneerrorlow;
                break;
            case (51):
                icon=R.drawable.smdoubleuperrorlow;
                break;
            case (52):
                icon=R.drawable.smuperrorlow;
                break;
            case (53):
                icon=R.drawable.smfortyfiveuperrorlow;
                break;
            case (54):
                icon=R.drawable.smflaterrorlow;
                break;
            case (55):
                icon=R.drawable.smfortyfivedownerrorlow;
                break;
            case (56):
                icon=R.drawable.smdownerrorlow;
                break;
            case (57):
                icon=R.drawable.smdoubledownerrorlow;
                break;
            case (58):
                icon=R.drawable.smnoneerrorlow;
                break;
            case (59):
                icon=R.drawable.smnoneerrorlow;
                break;
            default:
                icon=R.drawable.questionmarkicon;
                break;
        }
        wearableExtender.setBackground(bm)
                .setContentIcon(icon)
                .setHintHideIcon(true);
        notifBuilder.extend(wearableExtender);
        notifBuilder.setSmallIcon(icon);
//        notifBuilder.setSmallIcon(R.drawable.smicons, iconLevel);
    }

    @Override
    public void start() {
        super.start();
//        init();
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping monitor " + monitorType + " for " + name);
        mNotifyMgr.cancel(deviceID);
        if (context !=null) {
            if (snoozeReceiver != null)
                context.unregisterReceiver(snoozeReceiver);
            if (screenStateReceiver !=null)
                context.unregisterReceiver(screenStateReceiver);
            if (tickReceiver != null)
                context.unregisterReceiver(tickReceiver);
        }

    }

    public class SnoozeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context mContext, Intent intent) {
            if (intent.getAction().equals(Constants.SNOOZE_INTENT)) {
                if (intent.getExtras().get("device").equals(deviceIDStr)) {
                    Tracker tracker = ((BGScout) context.getApplicationContext()).getTracker();
                    tracker.send(new HitBuilders.EventBuilder("Snooze", "pressed").build());
                    Log.d(TAG, deviceIDStr + ": Received a request to snooze alarm on " + intent.getExtras().get("device"));
                    // Only capture the first snooze operation.. ignore others until it is reset
                    if (!isSilenced) {
                        isSilenced = true;
                        timeSilenced = new Date();
                    }
                    if (analyzedDownload != null)
                        mNotifyMgr.notify(deviceID, buildNotification(analyzedDownload));
                } else {
                    Log.d(TAG, deviceIDStr + ": Ignored a request to snooze alarm on " + intent.getExtras().get("device"));
                }
            }
        }
    }

    public void updateNotification(){
        Log.d(TAG, "Reanalyzing the download");
        if (previousDownloads.size() > 0) {
            AbstractDownloadAnalyzer downloadAnalyzer = new G4DownloadAnalyzer(previousDownloads.get(previousDownloads.size() - 1), context);
            analyzedDownload = downloadAnalyzer.analyze();
            setDefaults(analyzedDownload);
            setActions(analyzedDownload);
            setContent(analyzedDownload);
            setIcon(analyzedDownload);
            notifBuilder.setPriority(Notification.PRIORITY_LOW);
            Notification notification=notifBuilder.build();
            mNotifyMgr.notify(deviceID, notification);
        }
    }

    public class screenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context mContext, Intent intent) {
            Log.d(TAG,"Intent=>"+intent.getAction()+" received");
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                updateNotification();
                isScreenOn=true;
                Log.d(TAG, "Kicking off tick timer");
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                isScreenOn=false;
            }
        }
    }

    public class tickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context mContext, Intent intent) {
            if (intent.getAction().equals("android.intent.action.TIME_TICK")) {
                if (analyzedDownload==null)
                    return;
                try {
                    Log.d(TAG, "Comparing "+(new Date().getTime()-analyzedDownload.getLastRecordReadingDate().getTime()));
                    if (new Date().getTime()-analyzedDownload.getLastRecordReadingDate().getTime()< 310000 && ! isScreenOn) {
                        Log.d(TAG,"tickReceiver is returning because screen is off and the last reading is less than 5 minutes and 10 seconds old");
                        return;
                    }
                    if (isScreenOn)
                        updateNotification();
                    // On every 5th tick lets only do an update to avoid double notifications when there is a timing mismatch
                    if (tickCounter==5) {
                        updateNotification();
                        return;
                    }
                    if (tickCounter>6) {
                        Log.d(TAG, "Reanalyzing the download");
                        if (previousDownloads.size() > 0) {
                            AbstractDownloadAnalyzer downloadAnalyzer = new G4DownloadAnalyzer(previousDownloads.get(previousDownloads.size() - 1), context);
                            analyzedDownload = downloadAnalyzer.analyze();
                            mNotifyMgr.notify(deviceID, buildNotification(analyzedDownload));
                        }
                        tickCounter=0;
                    }
                    tickCounter+=1;
                } catch (NoDataException e) {
//                    e.printStackTrace();
                }

            }
        }
    }

    private Bitmap getThumbnailByPhoneDataUri(Uri phoneDataUri){
        String id=phoneDataUri.getLastPathSegment();
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,null, ContactsContract.Data._ID+" = ? ",new String[]{id},null);
        int thumbnailUriIdx=cursor.getColumnIndex(ContactsContract.Data.PHOTO_ID);
        String thumbnailId=null;
        if (cursor.moveToFirst()){
            thumbnailId=cursor.getString(thumbnailUriIdx);
        }
        cursor.close();
        Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, Long.valueOf(thumbnailId));
        cursor = context.getContentResolver().query(uri, new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO},null,null,null);
        Bitmap thumbnail=null;
        if (cursor.moveToFirst()){
            final byte[] thumbnailBytes = cursor.getBlob(0);
            if (thumbnailBytes!=null){
                thumbnail= BitmapFactory.decodeByteArray(thumbnailBytes,0,thumbnailBytes.length);
            }
        }
        cursor.close();
        return thumbnail;
    }


    public InputStream openDisplayPhoto(Uri phoneDataUri) {
        String id=phoneDataUri.getLastPathSegment();
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,null, ContactsContract.Data._ID+" = ? ",new String[]{id},null);
        int thumbnailUriIdx=cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
        String contactId=null;
        if (cursor.moveToFirst()){
            contactId=cursor.getString(thumbnailUriIdx);
        }
        cursor.close();
        Log.d(TAG,"ContactId=>"+contactId);
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId));
        Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
        try {
            AssetFileDescriptor fd =
                    context.getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
            return fd.createInputStream();
        } catch (IOException e) {
            return null;
        }
    }
}