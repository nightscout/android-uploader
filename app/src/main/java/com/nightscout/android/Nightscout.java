package com.nightscout.android;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import net.danlew.android.joda.JodaTimeAndroid;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formKey = "",
        formUri = "http://nightscout.cloudant.com/acra-nightscout/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="whisphisheiringliketfurg",
        formUriBasicAuthPassword="8CgjF6r2u4i8EhPHoPJjnk8f",
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.feebback_dialog_text,
        resDialogIcon = R.drawable.ic_launcher,
        resDialogTitle = R.string.feedback_dialog_title,
        resDialogCommentPrompt = R.string.feedback_dialog_comment_prompt,
        resDialogOkToast = R.string.feedback_dialog_ok_toast,
        excludeMatchingSharedPreferencesKeys= {"cloud_storage_mongodb_uri", "cloud_storage_api_base"},
        mode = ReportingInteractionMode.TOAST,
        logcatArguments = { "-t", "250", "-v", "time" }
)
public class Nightscout extends Application {
    private final String TAG = MainActivity.class.getSimpleName();
    private Tracker tracker = null;

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        JodaTimeAndroid.init(this);
    }

    synchronized public Tracker getTracker() {
        Log.d(TAG, "getTracker called");
        if (tracker == null) {
            Log.d(TAG,"tracker was null - returning new tracker");
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.setDryRun(false);
            analytics.getLogger().setLogLevel(Logger.LogLevel.WARNING);
            analytics.setLocalDispatchPeriod(7200);
            tracker =  analytics.newTracker(R.xml.app_tracker);
            return tracker;
        }
        return tracker;
    }
}
