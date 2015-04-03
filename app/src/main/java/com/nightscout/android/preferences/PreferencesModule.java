package com.nightscout.android.preferences;

import android.app.Application;

import com.nightscout.android.ui.NightscoutNavigationDrawer;
import com.nightscout.core.preferences.NightscoutPreferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
    injects = {
            NightscoutNavigationDrawer.class
    },
    complete = false,
    library = true
)
public class PreferencesModule {

  @Provides
  @Singleton
  public NightscoutPreferences provideNightscoutPreferences(Application app) {
    return new AndroidPreferences(app.getApplicationContext());
  }
}
