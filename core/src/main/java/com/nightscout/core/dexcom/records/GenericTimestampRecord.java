package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

abstract public class GenericTimestampRecord {

  protected final int OFFSET_SYS_TIME = 0;
  protected final int OFFSET_DISPLAY_TIME = 4;
  protected Date systemTime;
  protected int rawSystemTimeSeconds;
  protected Date displayTime;
  protected long rawDisplayTimeSeconds;

  public GenericTimestampRecord(byte[] packet) {
    rawSystemTimeSeconds =
        ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_SYS_TIME);
    systemTime = Utils.receiverTimeToDate(rawSystemTimeSeconds);
    rawDisplayTimeSeconds =
        ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_DISPLAY_TIME);
    displayTime = Utils.receiverTimeToDate(rawDisplayTimeSeconds);
  }

  public GenericTimestampRecord(Date displayTime, Date systemTime) {
    this.displayTime = displayTime;
    this.systemTime = systemTime;
  }

  public Date getSystemTime() {
    return systemTime;
  }

  public int getRawSystemTimeSeconds() {
    return rawSystemTimeSeconds;
  }

  public Date getDisplayTime() {
    return displayTime;
  }

  public long getRawDisplayTimeSeconds() {
    return rawDisplayTimeSeconds;
  }

}
