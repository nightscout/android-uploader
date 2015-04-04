package com.nightscout.core.drivers;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.DownloadStatus;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.model.SensorEntry;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

public class DexcomG4 extends AbstractDevice {
    public static final int VENDOR_ID = 8867;
    public static final int PRODUCT_ID = 71;
    public static final int DEVICE_CLASS = 2;
    public static final int DEVICE_SUBCLASS = 0;
    public static final int PROTOCOL = 0;

    protected NightscoutPreferences preferences;
    protected int numOfPages;
    protected AbstractUploaderDevice uploaderDevice;
    protected DeviceTransport transport;
    protected String receiverId = "";
    protected String transmitterId = "";

    protected Action1<Boolean> connectionStateListener = new Action1<Boolean>() {

        @Override
        public void call(Boolean connected) {
            if (connected) {
                onConnect();
            } else {
                onDisconnect();
            }
        }
    };

    public DexcomG4(DeviceTransport transport, NightscoutPreferences preferences,
                    AbstractUploaderDevice uploaderDevice) {
        this.transport = transport;
        this.preferences = preferences;
        this.uploaderDevice = uploaderDevice;
        this.deviceName = "DexcomG4";
        this.deviceType = preferences.getDeviceType();
        log.debug("New device being created: {}", this.deviceType);
        if (transport != null) {
            this.transport.registerConnectionListener(connectionStateListener);
        }
    }

    public String getReceiverId() {
        return receiverId;
    }

    @Override
    public void onConnect() {
        super.onConnect();
        log.debug("onConnect Called DexcomG4 connection");
//        ReadData readData = new ReadData(transport);
//        try {
//            receiverId = readData.readSerialNumber();
//            transmitterId = readData.readTrasmitterId();
//            log.debug("ReceiverId: {}", receiverId);
//            log.debug("TransmitterId: {}", transmitterId);
//        } catch (IOException e) {
//            log.error("Exception {}", e);
//            e.printStackTrace();
//        }
    }

    @Override
    public boolean isConnected() {
        return transport != null && transport.isConnected();
    }

    @Override
    protected G4Download doDownload() {
        G4Download.Builder downloadBuilder = new G4Download.Builder();

        DateTime dateTime = new DateTime();
        if (!isConnected()) {
            reporter.report(EventType.DEVICE, EventSeverity.WARN, messages.getString("event_g4_not_connected"));
            return downloadBuilder.download_timestamp(dateTime.toString())
                    .download_status(DownloadStatus.DEVICE_NOT_FOUND).build();
        }
        DownloadStatus status = DownloadStatus.SUCCESS;
        ReadData readData = new ReadData(transport);

        List<EGVRecord> recentRecords = new ArrayList<>();
        List<MeterRecord> meterRecords = new ArrayList<>();
        List<SensorRecord> sensorRecords = new ArrayList<>();
        List<CalRecord> calRecords = new ArrayList<>();
        int batLevel = 100;
        long systemTime = 0;
        try {
//            String receiverId = readData.readSerialNumber();
            if (receiverId.equals("")) {
                receiverId = readData.readSerialNumber();
                log.debug("ReceiverId: {}", receiverId);
            }
            if (transmitterId.equals("")) {
                transmitterId = readData.readTrasmitterId();
                log.debug("TransmitterId: {}", transmitterId);
            }

            systemTime = readData.readSystemTime();

            dateTime = new DateTime();
            recentRecords = readData.getRecentEGVsPages(numOfPages, systemTime, dateTime.getMillis());
            if (preferences.isMeterUploadEnabled()) {
                meterRecords = readData.getRecentMeterRecords(systemTime, dateTime.getMillis());
            }
            if (preferences.isSensorUploadEnabled()) {
                sensorRecords = readData.getRecentSensorRecords(numOfPages, systemTime, dateTime.getMillis());
            }

            if (preferences.isCalibrationUploadEnabled()) {
                calRecords = readData.getRecentCalRecords(systemTime, dateTime.getMillis());
            }
            if (recentRecords.size() == 0) {
                status = DownloadStatus.NO_DATA;
            }

            // FIXME: readData.readBatteryLevel() seems to flake out on battery level reads via serial.
            // Removing for now.
            if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4) {
                batLevel = 100;
            } else if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4_SHARE2) {
                batLevel = readData.readBatteryLevel();
            }
            // TODO pull in other exceptions once we have the analytics/acra reporters
        } catch (IOException e) {
            //TODO record this in the event log later
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "IO error to device");
            log.error("IO error to device " + e);

            status = DownloadStatus.IO_ERROR;
        } catch (InvalidRecordLengthException e) {
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "Application error " + e.getMessage());
            log.error("Application error " + e);
            status = DownloadStatus.APPLICATION_ERROR;
        }

        List<SensorGlucoseValueEntry> cookieMonsterG4SGVs = EGVRecord.toProtobufList(recentRecords);
        List<CalibrationEntry> cookieMonsterG4Cals = CalRecord.toProtobufList(calRecords);
        List<MeterEntry> cookieMonsterG4Meters = MeterRecord.toProtobufList(meterRecords);
        List<SensorEntry> cookieMonsterG4Sensors =
                SensorRecord.toProtobufList(sensorRecords);

        downloadBuilder.sgv(cookieMonsterG4SGVs)
                .cal(cookieMonsterG4Cals)
                .sensor(cookieMonsterG4Sensors)
                .meter(cookieMonsterG4Meters)
                .receiver_system_time_sec(systemTime)
                .download_timestamp(dateTime.toString())
                .download_status(status)
                .uploader_battery(uploaderDevice.getBatteryLevel())
                .receiver_battery(batLevel)
                .receiver_id(receiverId)
                .transmitter_id(transmitterId)
                .units(GlucoseUnit.MGDL);
        return downloadBuilder.build();
    }

    public void setNumOfPages(int numOfPages) {
        this.numOfPages = numOfPages;
    }
}
