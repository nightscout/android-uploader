package com.nightscout.core.upload.v2;

import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.converters.RestV1Converters;
import com.nightscout.core.upload.converters.SensorGlucoseValueAndRawSensorReading;
import com.nightscout.core.utils.DexcomG4Utils;
import com.nightscout.core.utils.RestUriUtils;
import com.squareup.wire.Message;

import net.tribe7.common.base.Strings;
import net.tribe7.common.collect.Lists;

import org.json.JSONObject;

import java.net.URI;
import java.util.List;

import retrofit.RestAdapter;
import retrofit.client.Response;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class RestV1Handler extends G4DataHandler {

  private final NightscoutPreferences preferences;
  private final Object httpClientLock = new Object();

  private String currentUrl;
  private RestV1Service restV1Service;
  private String apiSecret;

  public RestV1Handler(NightscoutPreferences preferences) {
    this.preferences = checkNotNull(preferences);
  }

  @Override
  protected boolean isEnabled() {
    return preferences.isRestApiEnabled();
  }

  @Override
  protected void handleSettingsRefresh() {
    final boolean preferencesChanged = !Strings.nullToEmpty(currentUrl).equals(preferences.getRestApiBaseUris().get(0));
    if (preferencesChanged) {
      refreshConnection();
    }
  }

  private void refreshConnection() {
    synchronized (httpClientLock) {
      if (restV1Service != null) {
        restV1Service = null;
      }
      URI userSetting = URI.create(preferences.getRestApiBaseUris().get(0));

      currentUrl = RestUriUtils.removeToken(userSetting).toString();
      apiSecret = checkNotNull(RestUriUtils.generateSecret(userSetting.getUserInfo()));
      restV1Service = new RestAdapter.Builder().setEndpoint(currentUrl).setLogLevel(
          RestAdapter.LogLevel.BASIC).build().create(RestV1Service.class);
    }
  }

  @Override
  protected List<Message> handleG4Data(G4Data download) {
    List<SensorGlucoseValueAndRawSensorReading> joinedReadings = DexcomG4Utils.mergeRawEntries(download);
    List<JSONObject> entries = Lists.transform(joinedReadings, RestV1Converters.sensorReadingConverter());
    entries.addAll(Lists.transform(download.insertions, RestV1Converters.insertionConverter()));
    entries.addAll(Lists.transform(download.manual_meter_entries, RestV1Converters.manualMeterEntryConverter()));
    List<Message> output = Lists.newArrayList();
    Response response;
    synchronized (httpClientLock) {
       response = restV1Service.uploadEntries(apiSecret, entries);
    }
    if (response != null && (response.getStatus() / 100) == 2) {
      DexcomG4Utils.addAllEntriesAsMessages(download, output);
    }
    return output;
  }
}
