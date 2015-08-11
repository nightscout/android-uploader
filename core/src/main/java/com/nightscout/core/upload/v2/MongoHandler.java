package com.nightscout.core.upload.v2;

import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.preferences.NightscoutPreferences;

public class MongoHandler extends G4DataHandler {

  private final NightscoutPreferences preferences;

  public MongoHandler(NightscoutPreferences preferences) {
    this.preferences = preferences;
  }

  @Override
  protected boolean isEnabled() {
    return preferences.isMongoUploadEnabled();
  }

  @Override
  protected void handleG4Data(final G4Data download) {

  }
}
