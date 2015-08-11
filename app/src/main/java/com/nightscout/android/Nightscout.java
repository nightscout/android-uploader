package com.nightscout.android;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.ui.ActivityHierarchyServer;
import com.nightscout.core.BusProvider;
import com.orm.Database;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dagger.ObjectGraph;

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
  private static final Logger log = LoggerFactory.getLogger(Nightscout.class);
  private Tracker tracker = null;

  private ObjectGraph objectGraph;

  @Override
  public void onCreate() {
    super.onCreate();
    buildObjectGraph();
    ActivityHierarchyServer activityHierarchyServer = objectGraph.get(ActivityHierarchyServer.class);
    BusProvider.getInstance();
    registerActivityLifecycleCallbacks(activityHierarchyServer);
  }

  protected Object[] getModules() {
    return Modules.list(this);
  }

  public void buildObjectGraph() {
    objectGraph = ObjectGraph.create(getModules());
  }

  public void inject(Object o) {
    objectGraph.inject(o);
  }

  public static Nightscout get(Context context) {
    return (Nightscout) context.getApplicationContext();
  }

  synchronized public Tracker getTracker() {
    log.debug("getTracker called");
    if (tracker == null) {
      log.debug("tracker was null - returning new tracker");
      GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
      analytics.setDryRun(false);
      analytics.getLogger().setLogLevel(com.google.android.gms.analytics.Logger.LogLevel.WARNING);
      analytics.setLocalDispatchPeriod(7200);
      tracker = analytics.newTracker(R.xml.app_tracker);
      return tracker;
    }
    return tracker;
  }
}
