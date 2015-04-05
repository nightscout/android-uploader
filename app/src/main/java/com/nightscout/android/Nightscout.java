package com.nightscout.android;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.ui.ActivityHierarchyServer;
import com.nightscout.core.BusProvider;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import javax.inject.Inject;

import dagger.ObjectGraph;

//import com.nightscout.android.exceptions.AcraFeedbackDialog;
//import net.danlew.android.joda.JodaTimeAndroid;

@ReportsCrashes(
        formUri = "https://collector.tracepot.com/a64e4a51",
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.feebback_dialog_text,
        resDialogIcon = R.drawable.ic_launcher,
        resDialogTitle = R.string.feedback_dialog_title,
        resDialogCommentPrompt = R.string.feedback_dialog_comment_prompt,
        resDialogOkToast = R.string.feedback_dialog_ok_toast,
        excludeMatchingSharedPreferencesKeys = {"cloud_storage_mongodb_uri", "cloud_storage_api_base", "cloud_storage_mqtt_user", "cloud_storage_mqtt_pass"},
        mode = ReportingInteractionMode.TOAST,
        logcatArguments = {"-t", "500", "-v", "time"}
)

public class Nightscout extends Application {
    private final String TAG = Nightscout.class.getSimpleName();
    private Tracker tracker = null;

    private ObjectGraph objectGraph;

    @Inject
    ActivityHierarchyServer activityHierarchyServer;

    @Inject
    FeedbackDialog feedbackDialog;

    @Override
    public void onCreate() {
        super.onCreate();
//        JodaTimeAndroid.init(this);
        buildObjectGraphAndInject();
        BusProvider.getInstance();
        registerActivityLifecycleCallbacks(activityHierarchyServer);
    }

    public void buildObjectGraphAndInject() {
        objectGraph = ObjectGraph.create(Modules.list(this));
        objectGraph.inject(this);
    }

    public void inject(Object o) {
        objectGraph.inject(o);
    }

    public static Nightscout get(Context context) {
        return (Nightscout) context.getApplicationContext();
    }

    synchronized public Tracker getTracker() {
        Log.d(TAG, "getTracker called");
        if (tracker == null) {
            Log.d(TAG, "tracker was null - returning new tracker");
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.setDryRun(false);
            analytics.getLogger().setLogLevel(Logger.LogLevel.WARNING);
            analytics.setLocalDispatchPeriod(7200);
            tracker = analytics.newTracker(R.xml.app_tracker);
            return tracker;
        }
        return tracker;
    }
}
