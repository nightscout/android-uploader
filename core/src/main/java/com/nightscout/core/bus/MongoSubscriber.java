package com.nightscout.core.bus;

import com.nightscout.core.model.G4Download;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.MongoUploader;
import com.squareup.otto.Subscribe;

public class MongoSubscriber {

  private final NightscoutPreferences preferences;

  public MongoSubscriber(NightscoutPreferences preferences) {
    this.preferences = preferences;
  }

  @Subscribe
  public void handleG4Download(G4Download g4Download) {
    if (!preferences.isMongoUploadEnabled()) {
      return;
    }

  }
}
