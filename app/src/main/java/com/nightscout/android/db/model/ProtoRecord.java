package com.nightscout.android.db.model;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import android.support.annotation.NonNull;

import com.nightscout.core.Timestamped;
import com.orm.SugarRecord;

import java.util.List;

public class ProtoRecord extends SugarRecord<ProtoRecord> implements Timestamped {

  public ProtoRecord() {}
  public ProtoRecord(long timestamp_sec, RecordType recordType, byte[] serializedProtobuf) {
    this.timestamp_sec = timestamp_sec;
    this.recordType = recordType;
    this.consumedBy = Lists.newArrayList();
    this.serializedProtobuf = serializedProtobuf;
  }
  @Override
  public long getTimestampSec() {
    return timestamp_sec;
  }

  @Override
  public int compareTo(@NonNull Timestamped another) {
    return Longs.compare(timestamp_sec, another.getTimestampSec());
  }

  public enum Consumer {
    MONGO_UPLOADER,
    API_V2_UPLOADER,
    API_LEGACY_UPLOADER
  }

  public enum RecordType {
    G4_SENSOR_GLUCOSE_VALUE,
    G4_INSERTION,
    G4_CALIBRATION,
    G4_MANUAL_METER_ENTRY,
    G4_RAW_SENSOR_READING,
  }

  /**
   * Timestamp of when the record was created (NOT when it was downloaded).
   */
  long timestamp_sec;

  /**
   * List of consumers that have successfully consumed this value. Note: if the consumer wants to
   * re-use values (e.g. graph), they are responsible for caching values.
   */
  List<Consumer> consumedBy;

  /**
   * Record of this type. Used to deserialize this entry.
   */
  RecordType recordType;

  /**
   * Serialized protobuffer.
   */
  @Unique
  byte[] serializedProtobuf;

}
