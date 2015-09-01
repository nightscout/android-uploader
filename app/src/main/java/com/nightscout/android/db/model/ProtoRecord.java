package com.nightscout.android.db.model;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import android.support.annotation.NonNull;

import com.nightscout.core.Timestamped;
import com.orm.SugarRecord;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import net.tribe7.common.hash.Hashing;

import java.io.IOException;
import java.util.List;

public class ProtoRecord extends SugarRecord<ProtoRecord> implements Timestamped {

  public ProtoRecord() {}
  public ProtoRecord(long timestampSec, RecordType recordType, byte[] serializedProtobuf) {
    this.timestampSec = timestampSec;
    this.recordType = recordType;
    this.consumedBy = Lists.newArrayList();
    this.md5Hash = Hashing.md5().hashBytes(serializedProtobuf).toString();
    this.serializedProtobuf = serializedProtobuf;
  }

  @Override
  public long getTimestampSec() {
    return timestampSec;
  }

  @Override
  public int compareTo(@NonNull Timestamped another) {
    return Longs.compare(timestampSec, another.getTimestampSec());
  }

  public enum Consumer {
    MONGO,
    API_V1_1, // Support up to three API uploaders.
    API_V1_2,
    API_V1_3,
    MQTT,
    INTERNAL_UI,
    PEBBLE,
    BROADCAST
  }

  public enum RecordType {
    UNKNOWN,
    G4_SENSOR_GLUCOSE_VALUE,
    G4_INSERTION,
    G4_CALIBRATION,
    G4_MANUAL_METER_ENTRY,
    G4_RAW_SENSOR_READING,
    G4_METADATA
  }

  /**
   * Timestamp of when the record was created (NOT when it was downloaded).
   */
  long timestampSec;

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
   * md5 hash of the serialized protobuf.
   */
  String md5Hash;

  /**
   * Serialized protobuffer.
   */
  byte[] serializedProtobuf;

  public String getMd5Hash() {
    return md5Hash;
  }

  public boolean existsInDatabase() {
    return ProtoRecord.count(ProtoRecord.class, "md5_hash = ?", new String[]{getMd5Hash()}) > 0;
  }

  public <T extends Message> T getAs(Class<T> clazz) {
    try {
      return new Wire().parseFrom(serializedProtobuf, clazz);
    } catch (IOException e) {
      return null;
    }
  }

  public boolean hasBeenConsumedBy(Consumer consumer) {
    return consumedBy.contains(consumer);
  }
}
