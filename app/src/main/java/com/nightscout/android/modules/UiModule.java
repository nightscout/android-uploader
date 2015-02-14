package com.nightscout.android.modules;

import com.nightscout.android.MainActivity;
import com.nightscout.android.ui.ActivityHierarchyServer;
import com.nightscout.android.ui.AppContainer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
    injects = {
        MainActivity.class
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
