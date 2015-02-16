package com.nightscout.core.listeners;

import com.google.common.collect.Lists;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.preferences.UpdatePreferences;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.upload.RestLegacyUploader;
import com.nightscout.core.upload.RestV1Uploader;
import com.squareup.otto.Subscribe;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RestListener {

  private List<BaseUploader> uploaders;
  private NightscoutPreferences preferences;

  public RestListener(NightscoutPreferences preferences) {
    this.preferences = preferences;
  }

  private void reset() {
    uploaders = Lists.newArrayList();
    initializeUploaders();
  }


  private void initializeUploaders() {
    List<String> baseUrisSetting = preferences.getRestApiBaseUris();
    List<URI> baseUris = new ArrayList<>();
    for (String baseURLSetting : baseUrisSetting) {
      String baseUriString = baseURLSetting.trim();
      if (baseUriString.isEmpty()) continue;
      baseUris.add(URI.create(baseUriString));
    }

    for (URI baseUri : baseUris) {
      if (baseUri.getPath().contains("v1")) {
        uploaders.add(new RestV1Uploader(preferences, baseUri));
      } else {
        uploaders.add(new RestLegacyUploader(preferences, baseUri));
      }
    }
  }

  @Subscribe
  public void handleG4Download(G4Download download) {
    for (BaseUploader uploader : uploaders) {
      List<GlucoseDataSet> glucoseDataSets =
          Utils.mergeGlucoseDataRecords(download.sgv, download.sensor);
      uploader.uploadGlucoseDataSets(glucoseDataSets);
      uploader.uploadMeterRecords(download.meter);
      uploader.uploadCalRecords(download.cal);
    }
  }

  @Subscribe
  public void handleUpdatePreferences(UpdatePreferences updatePreferences) {
    reset();
  }
}
