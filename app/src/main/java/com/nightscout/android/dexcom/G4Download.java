package com.nightscout.android.dexcom;

import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.dexcom.records.SensorRecord;
import com.nightscout.android.protobuf.SGV;
import java.util.List;

public class G4Download {
    protected List<EGVRecord> EGVRecords;
    protected List<CalRecord> CalRecords;
    protected List<SensorRecord> SensorRecords;
    protected List<MeterRecord> meterRecords;
    protected Unit units;
    protected long downloadTimestamp;
    protected DownloadStatus downloadStatus;
    protected int receiverBattery;
    protected int uploaderBattery;

    private G4Download(G4DownloadBuilder builder){
        this.EGVRecords=builder.EGVRecords;
        this.CalRecords=builder.CalRecords;
        this.SensorRecords=builder.SensorRecords;
        this.units=builder.units;
        this.downloadTimestamp=builder.downloadTimestamp;
        this.downloadStatus=builder.downloadStatus;
        this.receiverBattery=builder.receiverBattery;
        this.uploaderBattery=builder.uploaderBattery;
        this.meterRecords=builder.meterRecords;
    }

    public List<EGVRecord> getEGVRecords() {
        return EGVRecords;
    }

    public List<CalRecord> getCalRecords() {
        return CalRecords;
    }

    public List<SensorRecord> getSensorRecords() {
        return SensorRecords;
    }

    public int getReceiverBattery() {
        return receiverBattery;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public List<MeterRecord> getMeterRecords() {
        return meterRecords;
    }

    public int getUploaderBattery() {
        return uploaderBattery;
    }

    public long getDownloadTimestamp() {
        return downloadTimestamp;
    }

    public Unit getUnits() {
        return units;
    }

    public enum DownloadStatus {
        SUCCESS(0),
        NO_DATA(1),
        DEVICE_NOT_FOUND(2),
        IO_ERROR(3),
        APPLICATION_ERROR(4),
        NONE(6),
        UNKNOWN(7);

        private int id;

        DownloadStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum Unit {
        MGDL(0, "mg/dL"),
        MMOL(1, "mmol/L");

        private int id;
        private String friendlyName;

        Unit(int ID, String friendlyName) {
            this.id = ID;
            this.friendlyName = friendlyName;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public int getId() {
            return id;
        }
    }

    public static class G4DownloadBuilder {
        protected List<EGVRecord> EGVRecords;
        protected List<CalRecord> CalRecords;
        protected List<SensorRecord> SensorRecords;
        protected List<MeterRecord> meterRecords;
        protected Unit units;
        protected long downloadTimestamp;
        protected DownloadStatus downloadStatus;
        protected int receiverBattery;
        protected int uploaderBattery;

        public G4DownloadBuilder setEGVRecords(List<EGVRecord> EGVRecords) {
            this.EGVRecords = EGVRecords;
            return this;
        }

        public G4DownloadBuilder setCalRecords(List<CalRecord> calRecords) {
            CalRecords = calRecords;
            return this;
        }

        public G4DownloadBuilder setSensorRecords(List<SensorRecord> sensorRecords) {
            SensorRecords = sensorRecords;
            return this;
        }

        public G4DownloadBuilder setUnits(Unit units) {
            this.units = units;
            return this;
        }

        public G4DownloadBuilder setDownloadTimestamp(long downloadTimestamp) {
            this.downloadTimestamp = downloadTimestamp;
            return this;
        }

        public G4DownloadBuilder setDownloadStatus(DownloadStatus downloadStatus) {
            this.downloadStatus = downloadStatus;
            return this;
        }

        public G4DownloadBuilder setReceiverBattery(int receiverBattery) {
            this.receiverBattery = receiverBattery;
            return this;
        }

        public G4DownloadBuilder setUploaderBattery(int uploaderBattery) {
            this.uploaderBattery = uploaderBattery;
            return this;
        }

        public G4DownloadBuilder setMeterRecords(List<MeterRecord> meterRecords) {
            this.meterRecords = meterRecords;
            return this;
        }

        public G4Download build(){
            return new G4Download(this);
        }
    }
    public SGV.CookieMonsterG4Download toCookieProtobuf(){
        SGV.CookieMonsterG4Download.Builder downloadBuilder = SGV.CookieMonsterG4Download.newBuilder();
        SGV.CookieMonsterG4Download.DownloadStatus pbDownloadStatus=SGV.CookieMonsterG4Download.DownloadStatus.values()[downloadStatus.getId()];
        SGV.CookieMonsterG4Download.Unit pbUnit=SGV.CookieMonsterG4Download.Unit.values()[units.getId()];

        SGV.CookieMonsterG4EGV.Builder pbSgvBuilder = SGV.CookieMonsterG4EGV.newBuilder();
        SGV.CookieMonsterG4EGV pbSgv;

        downloadBuilder.setDownloadStatus(pbDownloadStatus)
                .setUnits(pbUnit)
                .setReceiverBattery(receiverBattery)
                .setUploaderBattery(uploaderBattery)
                .setDownloadTimestamp(downloadTimestamp);

        for (EGVRecord record:EGVRecords) {
            SGV.CookieMonsterG4EGV.Direction pbDirection=SGV.CookieMonsterG4EGV.Direction.values()[record.getTrend().getID()];
            pbSgv=pbSgvBuilder.setSgv(record.getBGValue())
                    .setDirection(pbDirection)
                    .setTimestamp(record.getDisplayTimeSeconds())
                    .build();
            downloadBuilder.addSgv(pbSgv);
        }

        SGV.CookieMonsterG4Cal.Builder pbCalBuilder = SGV.CookieMonsterG4Cal.newBuilder();
        SGV.CookieMonsterG4Cal pbCal;

        for (CalRecord record:CalRecords){
            pbCal=pbCalBuilder.setIntercept(record.getIntercept())
                    .setScale(record.getScale())
                    .setSlope(record.getSlope())
                    .setTimestamp(record.getDisplayTimeSeconds())
                    .build();
            downloadBuilder.addCal(pbCal);
        }

        SGV.CookieMonsterG4Sensor.Builder pbSensorBuilder = SGV.CookieMonsterG4Sensor.newBuilder();
        SGV.CookieMonsterG4Sensor pbSensor;
        for (SensorRecord record:SensorRecords){
            pbSensor=pbSensorBuilder.setFiltered(record.getFiltered())
                    .setRssi(record.getRSSI())
                    .setUnfiltered(record.getUnfiltered())
                    .setTimestamp(record.getDisplayTimeSeconds())
                    .build();
            downloadBuilder.addSensor(pbSensor);
        }

        SGV.CookieMonsterG4Meter.Builder pbMeterBuilder = SGV.CookieMonsterG4Meter.newBuilder();
        SGV.CookieMonsterG4Meter pbMeter;
        for (MeterRecord record:meterRecords){
            pbMeter=pbMeterBuilder.setTimestamp(record.getDisplayTime().getTime())
                    .setMeterBg(record.getMeterBG())
                    .setMeterTime(record.getMeterTime())
                    .build();
            downloadBuilder.addMeter(pbMeter);
        }

        return downloadBuilder.build();
    }
}
