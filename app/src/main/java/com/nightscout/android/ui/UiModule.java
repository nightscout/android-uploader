package com.nightscout.android.ui;

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
public class UiModule {

  @Provides
  @Singleton
  AppContainer provideAppContainer() {
    return AppContainer.DEFAULT;
  }

  @Provides
  @Singleton
  ActivityHierarchyServer provideActivityHierarchyServer() {
    return ActivityHierarchyServer.NONE;
  }
}
