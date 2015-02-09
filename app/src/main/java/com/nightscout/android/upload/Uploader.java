package com.nightscout.android.upload;

import android.content.Context;

import com.mongodb.MongoClientURI;
import com.nightscout.android.Nightscout;
import com.nightscout.android.R;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.drivers.UploaderDevice;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.DownloadResults;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.model.SensorEntry;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.upload.MongoUploader;
import com.nightscout.core.upload.RestLegacyUploader;
import com.nightscout.core.upload.RestV1Uploader;
import com.squareup.wire.Message;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class Uploader {

  private final List<BaseUploader> uploaders;
  private boolean allUploadersInitialized = true;
  private Context context;

  @Inject
  AndroidEventReporter reporter;
  @Inject
  NightscoutPreferences preferences;

  public Uploader(Context context) {
    Nightscout app = Nightscout.get(checkNotNull(context));
    app.inject(this);
    this.context = context;

    uploaders = new ArrayList<>();
    if (preferences.isMongoUploadEnabled()) {
      allUploadersInitialized &= initializeMongoUploader();
    }
    if (preferences.isRestApiEnabled()) {
      allUploadersInitialized &= initializeRestUploaders();
    }
  }

  private boolean initializeMongoUploader() {
    String dbURI = preferences.getMongoClientUri();
    String collectionName = preferences.getMongoCollection();
    String dsCollectionName = preferences.getMongoDeviceStatusCollection();
    checkNotNull(collectionName);
    checkNotNull(dsCollectionName);
    MongoClientURI uri;
    try {
      uri = new MongoClientURI(dbURI);
    } catch (IllegalArgumentException e) {
      reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                      context.getString(R.string.unknown_mongo_host));
      return false;
    } catch (NullPointerException e) {
      reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                      context.getString(R.string.unknown_mongo_host));
      return false;
    } catch (StringIndexOutOfBoundsException e) {
      reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                      context.getString(R.string.unknown_mongo_host));
      return false;
    }
    uploaders.add(new MongoUploader(preferences, uri, collectionName, dsCollectionName));
    return true;
  }

  private boolean initializeRestUploaders() {
    List<String> baseUrisSetting = preferences.getRestApiBaseUris();
    List<URI> baseUris = new ArrayList<>();
    boolean allInitialized = true;
    for (String baseURLSetting : baseUrisSetting) {
      String baseUriString = baseURLSetting.trim();
      if (baseUriString.isEmpty()) {
        continue;
      }
      try {
        baseUris.add(URI.create(baseUriString));
      } catch (IllegalArgumentException e) {
        allInitialized = false;
        reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        context.getString(R.string.illegal_rest_url, baseUriString));
      }
    }

    for (URI baseUri : baseUris) {
      if (baseUri.getPath().contains("v1")) {
        try {
          uploaders.add(new RestV1Uploader(preferences, baseUri));
        } catch (IllegalArgumentException e) {
          allInitialized = false;
          reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                          context.getString(R.string.illegal_rest_url, baseUri.toString()));
        }
      } else {
        uploaders.add(new RestLegacyUploader(preferences, baseUri));
      }
    }
    return allInitialized;
  }

  public boolean upload(DownloadResults downloadResults, int numRecords) {
    G4Download download = downloadResults.getDownload();
    List<SensorGlucoseValueEntry> sgvList = filterRecords(numRecords, download.sgv);
    List<CalibrationEntry> calList = filterRecords(numRecords, download.cal);
    List<MeterEntry> meterList = filterRecords(numRecords, download.meter);
    List<SensorEntry> sensorList = filterRecords(numRecords, download.sensor);

    List<GlucoseDataSet> glucoseDataSets = Utils.mergeGlucoseDataRecords(sgvList, sensorList);

    return upload(glucoseDataSets, meterList, calList);
  }

  private <T extends Message> List<T> filterRecords(int numRecords, List<T> records) {
    int recordIndexToStop = Math.max(records.size() - numRecords, 0);
    List<T> results = new ArrayList<>();
    for (int i = records.size(); i > recordIndexToStop; i--) {
      results.add(records.get(i - 1));
    }
    return results;
  }

  public boolean upload(DownloadResults downloadResults) {
    G4Download download = downloadResults.getDownload();
    List<GlucoseDataSet> glucoseDataSets =
        Utils.mergeGlucoseDataRecords(download.sgv, download.sensor);
    return upload(glucoseDataSets, download.meter, download.cal);
  }

  private boolean upload(List<GlucoseDataSet> glucoseDataSets,
                         List<MeterEntry> meterRecords,
                         List<CalibrationEntry> calRecords) {

    UploaderDevice deviceStatus = AndroidUploaderDevice.getUploaderDevice(context);

    boolean allSuccessful = true;
    for (BaseUploader uploader : uploaders) {
      // TODO(klee): capture any exceptions here so that all configured uploaders will attempt
      // to upload
      allSuccessful &=
          uploader.uploadRecords(glucoseDataSets, meterRecords, calRecords, deviceStatus);
      reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                      context.getString(R.string.event_success_upload, uploader.getIdentifier()));

    }

    // Quick hack to prevent MQTT only from reporting not uploading to cloud
    int otherUploaders = (preferences.isMqttEnabled()) ? 1 : 0;
    // Force a failure if an uploader was not properly initialized, but only after the other
    // uploaders were executed.
    return allUploadersInitialized && allSuccessful && (uploaders.size() + otherUploaders != 0);
  }

  protected List<BaseUploader> getUploaders() {
    return uploaders;
  }

  protected boolean areAllUploadersInitialized() {
    return allUploadersInitialized;
  }
}
