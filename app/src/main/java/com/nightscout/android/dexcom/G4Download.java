package com.nightscout.android.dexcom;

import com.nightscout.android.MainActivity;
import com.nightscout.android.TimeConstants;
import com.nightscout.android.devices.DexcomG4;
import com.nightscout.android.devices.Download;
import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.dexcom.records.SensorRecord;
import com.nightscout.android.protobuf.SGV;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class G4Download extends Download {
    protected List<EGVRecord> EGVRecords;
    protected List<CalRecord> CalRecords;
    protected List<SensorRecord> SensorRecords;
    protected List<MeterRecord> MeterRecords;
    protected int receiverBattery;
    protected int uploaderBattery;

    // Not included in protobuf definitions. Only used locally
    protected long nextUploadTime;
    protected long displayTime;

    public G4Download(){
        this.EGVRecords=new ArrayList<EGVRecord>();
        this.CalRecords=new ArrayList<CalRecord>();
        this.SensorRecords=new ArrayList<SensorRecord>();
        this.MeterRecords=new ArrayList<MeterRecord>();
        this.receiverBattery=0;
        this.uploaderBattery=MainActivity.batLevel;
        this.nextUploadTime=45000+G4Constants.TIME_SYNC_OFFSET;
        this.displayTime=0;
        this.driver=G4Constants.DRIVER;
    }

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
        this.driver=builder.driver;
        this.deviceID=builder.deviceID;
        this.nextUploadTime=builder.nextUploadTime;
        this.displayTime=builder.displayTime;
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
        this.driver=g4Download.getDriver();
        this.deviceID=g4Download.getDeviceID();
        this.nextUploadTime=g4Download.getNextUploadTime();
        this.displayTime=g4Download.displayTime;
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

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public int getDeviceID() {
        return deviceID;
    }

    public String getDriver() {
        return driver;
    }

    public EGVRecord getLastEGVRecord(){
        return EGVRecords.get(EGVRecords.size() - 1);
    }

    public long getLastEGVTimestamp(){
        return getLastEGVRecord().getDisplayTimeSeconds();
    }

    public Date getLastEGVDisplayTime(){
        return getLastEGVRecord().getDisplayTime();
    }

    public int getLastEGV(){
        return getLastEGVRecord().getBGValue();
    }

    public Trend getLastEGVTrend(){
        return getLastEGVRecord().getTrend();
    }

    public long getNextUploadTime() {
        return nextUploadTime;
    }

    public long getDisplayTime() {
        return displayTime;
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
        protected String driver;
        protected int deviceID;

        // Local only - not included in protobuf export
        protected long nextUploadTime;
        protected long displayTime;

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

        public G4DownloadBuilder setNextUploadTime(long nextUploadTime) {
            this.nextUploadTime = nextUploadTime;
            return this;
        }

        public G4DownloadBuilder setDisplayTime(long displayTime) {
            this.displayTime = displayTime;
            return this;
        }

        public G4DownloadBuilder setDriver(String driver) {
            this.driver = driver;
            return this;
        }

        public void setDeviceID(int deviceID) {
            this.deviceID = deviceID;
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
        this.driver=download.getDriver();
        this.deviceID=download.getDeviceId();

        // FIXME: these variable names break conventions but need to be renamed to avoid conflicts
        this.EGVRecords=egvRecords;
        this.MeterRecords=meterRecords;
        this.SensorRecords=sensorRecords;
        this.CalRecords=calRecords;

    }

    @Override
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
                .setDriver(driver)
                .setDeviceId(deviceID)
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
