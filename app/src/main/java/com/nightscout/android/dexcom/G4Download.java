package com.nightscout.android.dexcom;

import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.dexcom.records.SensorRecord;
import com.nightscout.android.protobuf.SGV;

import java.util.ArrayList;
import java.util.List;

public class G4Download {
    protected List<EGVRecord> EGVRecords;
    protected List<CalRecord> CalRecords;
    protected List<SensorRecord> SensorRecords;
    protected List<MeterRecord> MeterRecords;
    protected GlucoseUnit unit;
    protected long downloadTimestamp;
    protected DownloadStatus downloadStatus;
    protected int receiverBattery;
    protected int uploaderBattery;

    private G4Download(G4DownloadBuilder builder){
        this.EGVRecords=builder.EGVRecords;
        this.CalRecords=builder.CalRecords;
        this.SensorRecords=builder.SensorRecords;
        this.unit=builder.units;
        this.downloadTimestamp=builder.downloadTimestamp;
        this.downloadStatus=builder.downloadStatus;
        this.receiverBattery=builder.receiverBattery;
        this.uploaderBattery=builder.uploaderBattery;
        this.MeterRecords =builder.MeterRecords;
    }

    public G4Download(G4Download g4Download){
        this.EGVRecords=g4Download.getEGVRecords();
        this.CalRecords=g4Download.getCalRecords();
        this.SensorRecords=g4Download.getSensorRecords();
        this.unit=g4Download.getUnit();
        this.downloadTimestamp=g4Download.getDownloadTimestamp();
        this.downloadStatus=g4Download.getDownloadStatus();
        this.receiverBattery=g4Download.getReceiverBattery();
        this.uploaderBattery=g4Download.getUploaderBattery();
        this.MeterRecords =g4Download.getMeterRecords();
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
        return MeterRecords;
    }

    public int getUploaderBattery() {
        return uploaderBattery;
    }

    public long getDownloadTimestamp() {
        return downloadTimestamp;
    }

    public GlucoseUnit getUnit() {
        return unit;
    }

    public static class G4DownloadBuilder {
        protected List<EGVRecord> EGVRecords;
        protected List<CalRecord> CalRecords;
        protected List<SensorRecord> SensorRecords;
        protected List<MeterRecord> MeterRecords;
        protected GlucoseUnit units;
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

        public G4DownloadBuilder setUnits(GlucoseUnit units) {
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
            this.MeterRecords = meterRecords;
            return this;
        }

        public G4Download build(){
            return new G4Download(this);
        }
    }

    public G4Download(SGV.CookieMonsterG4Download download){
        ArrayList<EGVRecord> egvRecords=new ArrayList<EGVRecord>();
        ArrayList<SensorRecord> sensorRecords=new ArrayList<SensorRecord>();
        ArrayList<CalRecord> calRecords=new ArrayList<CalRecord>();
        ArrayList<MeterRecord> meterRecords=new ArrayList<MeterRecord>();

        for (SGV.CookieMonsterG4EGV record:download.getSgvList()){
            egvRecords.add(new EGVRecord(record.getSgv(), Trend.values()[record.getDirection().getNumber()],record.getTimestamp(),record.getSysTimestamp()));
        }
        for (SGV.CookieMonsterG4Sensor record:download.getSensorList()){
            sensorRecords.add(new SensorRecord(record.getUnfiltered(),record.getFiltered(),record.getRssi(),record.getTimestamp(),record.getSysTimestamp()));
        }
        for (SGV.CookieMonsterG4Cal record:download.getCalList()){
            calRecords.add(new CalRecord(record.getSlope(),record.getIntercept(),record.getScale(),record.getTimestamp(),record.getSysTimestamp()));
        }
        for (SGV.CookieMonsterG4Meter record:download.getMeterList()){
            meterRecords.add(new MeterRecord(record.getMeterBg(),record.getMeterTime(),record.getTimestamp(),record.getSysTimestamp()));
        }
        this.downloadStatus=DownloadStatus.values()[download.getDownloadStatus().getNumber()];
        this.unit=GlucoseUnit.values()[download.getUnits().getNumber()];
        this.receiverBattery=download.getReceiverBattery();
        this.uploaderBattery=download.getUploaderBattery();
        this.downloadTimestamp=download.getDownloadTimestamp();
        // FIXME: these variable names break conventions but need to be renamed to avoid conflicts
        this.EGVRecords=egvRecords;
        this.MeterRecords=meterRecords;
        this.SensorRecords=sensorRecords;
        this.CalRecords=calRecords;
    }

    public SGV.CookieMonsterG4Download toCookieProtobuf(){
        SGV.CookieMonsterG4Download.Builder downloadBuilder = SGV.CookieMonsterG4Download.newBuilder();
        SGV.CookieMonsterG4Download.DownloadStatus pbDownloadStatus=SGV.CookieMonsterG4Download.DownloadStatus.values()[downloadStatus.getId()];
        SGV.CookieMonsterG4Download.Unit pbUnit=SGV.CookieMonsterG4Download.Unit.values()[unit.getId()];

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
        for (MeterRecord record: MeterRecords){
            pbMeter=pbMeterBuilder.setTimestamp(record.getDisplayTime().getTime())
                    .setMeterBg(record.getMeterBG())
                    .setMeterTime(record.getMeterTime())
                    .build();
            downloadBuilder.addMeter(pbMeter);
        }

        return downloadBuilder.build();
    }
}
