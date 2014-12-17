package com.nightscout.core.download;

import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Download Object specific to the G4
 *
 * @see com.nightscout.core.download.Download
 */
public class G4Download extends Download {

  protected List<EGVRecord> egvRecords;
  protected List<MeterRecord> meterRecords;
  protected List<CalRecord> calRecords;
  protected List<SensorRecord> sensorRecords;
  protected int receiverBattery;
  protected GlucoseUnits units;
  protected long systemTime;
  protected long displayTime;
  protected String name;

  private G4Download(DateTime downloadTimestamp, DownloadStatus downloadStatus, int uploaderBattery,
                     List<EGVRecord> egvRecords, List<MeterRecord> meterRecords,
                     List<CalRecord> calRecords, List<SensorRecord> sensorRecords,
                     int receiverBattery, GlucoseUnits units, long sysTime, long dispTime) {
    super(downloadTimestamp, downloadStatus, uploaderBattery);
    this.egvRecords = checkNotNull(egvRecords);
    this.meterRecords = checkNotNull(meterRecords);
    this.calRecords = checkNotNull(calRecords);
    this.sensorRecords = checkNotNull(sensorRecords);
    this.receiverBattery = checkNotNull(receiverBattery);
    this.units = checkNotNull(units);
    this.systemTime = checkNotNull(sysTime);
    this.displayTime = checkNotNull(dispTime);
  }

  public List<EGVRecord> getEgvRecords() {
    return egvRecords;
  }

  public void setEgvRecords(List<EGVRecord> egvRecords) {
    this.egvRecords = egvRecords;
  }

  public List<MeterRecord> getMeterRecords() {
    return meterRecords;
  }

  public void setMeterRecords(List<MeterRecord> meterRecords) {
    this.meterRecords = meterRecords;
  }

  public List<CalRecord> getCalRecords() {
    return calRecords;
  }

  public void setCalRecords(List<CalRecord> calRecords) {
    this.calRecords = calRecords;
  }

  public List<SensorRecord> getSensorRecords() {
    return sensorRecords;
  }

  public void setSensorRecords(List<SensorRecord> sensorRecords) {
    this.sensorRecords = sensorRecords;
  }

  public int getReceiverBattery() {
    return receiverBattery;
  }

  public void setReceiverBattery(int receiverBattery) {
    this.receiverBattery = receiverBattery;
  }

  public GlucoseUnits getUnits() {
    return units;
  }

  public void setUnits(GlucoseUnits units) {
    this.units = units;
  }

  public long getSystemTime() {
    return systemTime;
  }

  public void setSystemTime(long systemTime) {
    this.systemTime = systemTime;
  }

  public long getDisplayTime() {
    return displayTime;
  }

  public void setDisplayTime(long displayTime) {
    this.displayTime = displayTime;
  }

  /**
   * <p> This method creates a serialized representation of this download object using protobuf
   * </p>
   *
   * @return serialized download object
   */
  public byte[] toProtobufByteArray() {
    // TODO (klee): Inconsistent usage of SGV/EGV Trend/Direction
    SGV.CookieMonsterG4Download.Builder builder = SGV.CookieMonsterG4Download.newBuilder();
    SGV.CookieMonsterG4Download.DownloadStatus downloadStatus =
        SGV.CookieMonsterG4Download.DownloadStatus.values()[status.ordinal()];
    SGV.CookieMonsterG4Download.Unit u =
        SGV.CookieMonsterG4Download.Unit.values()[units.ordinal()];
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    for (EGVRecord record : egvRecords) {
      SGV.CookieMonsterG4EGV.Builder sgvBuilder = SGV.CookieMonsterG4EGV.newBuilder();
      SGV.CookieMonsterG4EGV.Direction direction =
          SGV.CookieMonsterG4EGV.Direction.values()[record.getTrend().ordinal()];
      sgvBuilder.setSgv(record.getBGValue())
          .setTimestamp(record.getRawSystemTimeSeconds())
          .setDirection(direction);
      builder.addSgv(sgvBuilder);
    }
    builder.setUnits(u)
        .setDownloadTimestamp(fmt.print(downloadTimestamp))
        .setReceiverSystemTime(systemTime)
        .setDownloadStatus(downloadStatus)
        .setReceiverBattery(receiverBattery)
        .setUploaderBattery(uploaderBattery);
    for (MeterRecord record : meterRecords) {
      SGV.CookieMonsterG4Meter.Builder meterBuilder = SGV.CookieMonsterG4Meter.newBuilder();
      meterBuilder.setMeterBg(record.getMeterBG())
          .setMeterTime(record.getMeterTime())
          .setTimestamp(record.getRawSystemTimeSeconds());
      builder.addMeter(meterBuilder);
    }
    for (SensorRecord record : sensorRecords) {
      SGV.CookieMonsterG4Sensor.Builder sensorBuilder =
          SGV.CookieMonsterG4Sensor.newBuilder();
      sensorBuilder.setFiltered(record.getFiltered())
          .setUnfiltered(record.getUnfiltered())
          .setRssi(record.getRssi())
          .setTimestamp(record.getRawSystemTimeSeconds());
      builder.addSensor(sensorBuilder);
    }
    for (CalRecord record : calRecords) {
      SGV.CookieMonsterG4Cal.Builder calBuilder = SGV.CookieMonsterG4Cal.newBuilder();
      calBuilder.setSlope(record.getSlope())
          .setIntercept(record.getIntercept())
          .setScale(record.getScale())
          .setTimestamp(record.getRawSystemTimeSeconds());
      builder.addCal(calBuilder);
    }
    builder.setPatientName(name);
    SGV.CookieMonsterG4Download download = builder.build();
    return download.toByteArray();
  }


  public class G4DownloadBuilder {

    private DateTime downloadTimestamp;
    private DownloadStatus downloadStatus = DownloadStatus.NONE;
    private int uploaderBattery;
    private List<EGVRecord> egvRecords;
    private List<MeterRecord> meterRecords;
    private List<CalRecord> calRecords;
    private List<SensorRecord> sensorRecords;
    private int receiverBattery;
    private GlucoseUnits units;
    private long sysTime;
    private long dispTime;

    public G4DownloadBuilder setDownloadTimestamp(DateTime downloadTimestamp) {
      this.downloadTimestamp = downloadTimestamp;
      return this;
    }

    public G4DownloadBuilder setDownloadStatus(DownloadStatus status) {
      this.downloadStatus = status;
      return this;
    }

    public G4DownloadBuilder setUploaderBattery(int uploaderBattery) {
      this.uploaderBattery = uploaderBattery;
      return this;
    }

    public G4DownloadBuilder setEgvRecords(List<EGVRecord> egvRecords) {
      this.egvRecords = egvRecords;
      return this;
    }

    public G4DownloadBuilder setMeterRecords(List<MeterRecord> meterRecords) {
      this.meterRecords = meterRecords;
      return this;
    }

    public G4DownloadBuilder setCalRecords(List<CalRecord> calRecords) {
      this.calRecords = calRecords;
      return this;
    }

    public G4DownloadBuilder setSensorRecords(List<SensorRecord> sensorRecords) {
      this.sensorRecords = sensorRecords;
      return this;
    }

    public G4DownloadBuilder setReceiverBattery(int receiverBattery) {
      this.receiverBattery = receiverBattery;
      return this;
    }

    public G4DownloadBuilder setUnits(GlucoseUnits units) {
      this.units = units;
      return this;
    }

    public G4DownloadBuilder setSystemTime(long sysTime) {
      this.sysTime = sysTime;
      return this;
    }

    public G4DownloadBuilder setDisplayTime(long dispTime) {
      this.dispTime = dispTime;
      return this;
    }

    public G4Download createG4Download() {
      return new G4Download(downloadTimestamp, status, uploaderBattery, egvRecords,
                            meterRecords, calRecords, sensorRecords, receiverBattery, units,
                            sysTime,
                            dispTime);
    }
  }
}
