package com.nightscout.android.listeners;

import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.preferences.UpdatePreferences;
import com.nightscout.core.utils.G4DownloadUtils;
import com.nightscout.core.utils.GlucoseReading;
import com.squareup.otto.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

public class PebbleListener {

  private final NightscoutPreferences preferences;
  private final Pebble pebble;

  public PebbleListener(NightscoutPreferences preferences, Pebble pebble) {
    this.preferences = checkNotNull(preferences);
    this.pebble = checkNotNull(pebble);
  }

  @Subscribe
  public void handleG4Download(G4Download g4Download) {
    SensorGlucoseValueEntry latestEntry = G4DownloadUtils.getLatest(g4Download.sgv);
    // TODO(trhodeos): add a check to make sure pebble is enabled before sending it out, in order
    // to preserve battery life.
    if (latestEntry.sgv_mgdl != -1) {
      pebble.sendDownload(new GlucoseReading(latestEntry.sgv_mgdl, GlucoseUnit.MGDL),
                          latestEntry.trend, latestEntry.disp_timestamp_sec);
    }
  }

  @Subscribe
  public void handlePreferencesChanged(UpdatePreferences updatePreferences) {
    pebble.setPwdName(preferences.getPwdName());
    pebble.setUnits(preferences.getPreferredUnits());
  }
}
