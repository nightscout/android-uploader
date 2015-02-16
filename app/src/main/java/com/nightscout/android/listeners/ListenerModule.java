package com.nightscout.android.listeners;

import com.nightscout.android.Nightscout;
import com.nightscout.android.SyncingService;
import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.bus.ScopedBus;
import com.nightscout.core.listeners.RestListener;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.listeners.MongoListener;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
     injects = {
         SyncingService.class
    }
)
public class ListenerModule {

  @Provides
  @Singleton
  MongoListener providesMongoUploader(NightscoutPreferences preferences, ScopedBus scopedBus) {
    MongoListener uploader = new MongoListener(preferences);
    scopedBus.register(uploader);
    return uploader;
  }

  @Provides
  @Singleton
  UiListener providesUiListener(Nightscout app, ScopedBus scopedBus) {
    UiListener uiListener = new UiListener(app);
    scopedBus.register(uiListener);
    return uiListener;
  }

  @Provides
  @Singleton
  PebbleListener providesPebbleListener(NightscoutPreferences preferences, Pebble pebble, ScopedBus scopedBus) {
    PebbleListener pebbleListener = new PebbleListener(preferences, pebble);
    scopedBus.register(pebbleListener);
    return pebbleListener;
  }

  @Provides
  @Singleton
  RestListener providesRestListener(NightscoutPreferences preferences, ScopedBus scopedBus) {
    RestListener restListener = new RestListener(preferences);
    scopedBus.register(restListener);
    return restListener;
  }

}
