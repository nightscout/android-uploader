package com.nightscout.android.listeners;

import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.core.model.G4Download;
import com.squareup.otto.Subscribe;

/**
 * Listens to downloads sent by devices, and broadcasts events to the UI so it will update.
 */
public class UiListener {

  private final Nightscout app;

  // Response to broadcast to activity

  public UiListener(Nightscout app) {
    this.app = app;
  }

  @Subscribe
  public void handleG4Download(G4Download download) {
    app.sendBroadcast(MainActivity.createCGMStatusIntent(download));
  }
}
