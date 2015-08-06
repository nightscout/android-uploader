package com.nightscout.android.db.model;

import com.orm.SugarRecord;

public class DeviceRecord extends SugarRecord<DeviceRecord> {
  public DeviceRecord() {}
  public DeviceRecord(String id) {
    identifier = id;
  }
  String identifier;
}
