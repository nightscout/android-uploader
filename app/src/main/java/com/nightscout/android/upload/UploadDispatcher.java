package com.nightscout.android.upload;

import com.google.common.collect.Maps;

import com.nightscout.android.db.DbUtils;
import com.nightscout.android.db.model.ProtoRecord;
import com.nightscout.core.model.v2.Calibration;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.model.v2.G4Metadata;
import com.nightscout.core.model.v2.Insertion;
import com.nightscout.core.model.v2.ManualMeterEntry;
import com.nightscout.core.model.v2.RawSensorReading;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.upload.v2.AckHandler;
import com.nightscout.core.upload.v2.G4DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UploadDispatcher {
  private static final Logger log = LoggerFactory.getLogger(UploadDispatcher.class);
  private Map<ProtoRecord.Consumer, G4DataHandler> consumerMap = Maps.newHashMap();

  public void dispatch(ProtoRecord.Consumer consumer) {
    Download download = reconstructDownload(consumer);
    G4DataHandler handler = consumerMap.get(consumer);
    if (handler == null) {
      log.warn("Could not find data handler for {}, using default handler.", consumer.name());
      handler = new AckHandler();
    }
  }

  private Download reconstructDownload(ProtoRecord.Consumer consumer) {
    Download.Builder download = new Download.Builder();
    download.g4_data(reconstructG4Data(consumer));
    return download.build();
  }

  private G4Data reconstructG4Data(ProtoRecord.Consumer consumer) {
    return new G4Data.Builder()
        .metadata(DbUtils.getLatestFromDb(G4Metadata.class))
        .calibrations(DbUtils.getAllFromDb(Calibration.class, consumer))
        .insertions(DbUtils.getAllFromDb(Insertion.class, consumer))
        .sensor_glucose_values(DbUtils.getAllFromDb(SensorGlucoseValue.class, consumer))
        .raw_sensor_readings(DbUtils.getAllFromDb(RawSensorReading.class, consumer))
        .manual_meter_entries(DbUtils.getAllFromDb(ManualMeterEntry.class, consumer)).build();
  }
}
