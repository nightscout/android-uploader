package com.nightscout.android.modules;

import android.app.Application;

import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.exceptions.AcraFeedbackDialog;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.core.bus.ScopedBus;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.acra.ACRA;

import java.util.TimeZone;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
    includes = {
        UiModule.class,
        UploaderModule.class
    },
    injects = {
        Nightscout.class,
        MainActivity.class
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
  FeedbackDialog providesReporter(Application app) {
    ACRA.init(app);
    ACRA.getErrorReporter().putCustomData("timezone", TimeZone.getDefault().getID());
    return new AcraFeedbackDialog();
  }

  @Provides
  @Singleton
  ScopedBus providesScopedBus() {
    return new ScopedBus(new Bus(ThreadEnforcer.ANY));
  }

  @Provides
  public NightscoutPreferences provideNightscoutPreferences(Application app) {
    return new AndroidPreferences(app.getApplicationContext());
  }
}
