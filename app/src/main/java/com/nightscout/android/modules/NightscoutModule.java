package com.nightscout.android.modules;

import android.app.Application;

import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.exceptions.AcraFeedbackDialog;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.ui.UiModule;
import com.nightscout.android.upload.Uploader;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.acra.ACRA;

import java.util.TimeZone;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
    includes = {
        UiModule.class
    },
    injects = {
        Nightscout.class,
        MainActivity.class,
        Uploader.class
    }
)
public class NightscoutModule {

  private Application app;

  public NightscoutModule(Application app) {
    this.app = app;
  }

  @Provides
  @Singleton
  Application providesApplication() {
    return app;
  }

  @Provides
  @Singleton
  public NightscoutPreferences provideNightscoutPreferences(Application app) {
    return new AndroidPreferences(app.getApplicationContext());
  }

  @Provides
  @Singleton
  FeedbackDialog providesFeedbackDialog(Application app) {
    ACRA.init(app);
    ACRA.getErrorReporter().putCustomData("timezone", TimeZone.getDefault().getID());
    return new AcraFeedbackDialog();
  }

  @Provides
  @Singleton
  public AndroidEventReporter androidEventReporter(Application app) {
    return AndroidEventReporter.getReporter(app);
  }
}
