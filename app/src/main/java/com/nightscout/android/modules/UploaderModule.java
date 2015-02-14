package com.nightscout.android.modules;

import android.app.Application;

import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.SyncingService;
import com.nightscout.core.bus.ScopedBus;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.MongoUploader;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
     injects = {
         SyncingService.class
    }
)
public class UploaderModule {

  @Provides
  @Singleton
  MongoUploader providesMongoUploader(NightscoutPreferences preferences, ScopedBus scopedBus) {
    MongoUploader uploader = new MongoUploader(preferences);
    scopedBus.register(uploader);
    return uploader;
  }

}
