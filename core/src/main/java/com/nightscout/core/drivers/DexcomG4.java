package com.nightscout.core.drivers;

import com.nightscout.core.BusProvider;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.DownloadStatus;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.InsertionEntry;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.model.SensorEntry;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.squareup.otto.Bus;

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
    protected List<SensorRecord> lastSensorRecords;
    protected List<CalRecord> lastCalRecords;

    protected Action1<G4ConnectionState> connectionStateListener = new Action1<G4ConnectionState>() {

        @Override
        public void call(G4ConnectionState connected) {
            switch (connected) {
                case CONNECTING:
                    onConnecting();
                    break;
                case CONNECTED:
                    onConnect();
                    break;
                case CLOSED:
                    onDisconnect();
                    break;
                case CLOSING:
                    onDisconnecting();
                    break;
                case READING:
                    onReading();
                    break;
                case WRITING:
                    onWriting();
                    break;
                default:
                    break;

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

        List<EGVRecord> recentRecords;
        List<MeterRecord> meterRecords = new ArrayList<>();
        List<SensorRecord> sensorRecords = new ArrayList<>();
        List<CalRecord> calRecords = new ArrayList<>();
        List<InsertionRecord> insertionRecords = new ArrayList<>();

        List<SensorGlucoseValueEntry> cookieMonsterG4SGVs;
        List<SensorEntry> cookieMonsterG4Sensors;
        Bus bus = BusProvider.getInstance();

        int batLevel = 100;
        long systemTime = 0;
        try {
            if (receiverId.equals("")) {
                receiverId = readData.readSerialNumber();
                log.debug("ReceiverId: {}", receiverId);
            } else {
                log.warn("Using receiverId from session: {}", receiverId);
            }
            if (transmitterId.equals("")) {
                transmitterId = readData.readTrasmitterId();
                log.debug("TransmitterId: {}", transmitterId);
            } else {
                log.warn("Using TransmitterId from session: {}", transmitterId);
            }

            systemTime = readData.readSystemTime();
            // FIXME: readData.readBatteryLevel() seems to flake out on battery level reads via serial.
            // Removing for now.
            if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4) {
                batLevel = 100;
            } else if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4_SHARE2) {
                batLevel = readData.readBatteryLevel();
            }

            dateTime = new DateTime();
            recentRecords = readData.getRecentEGVsPages(numOfPages, systemTime, dateTime.getMillis());
            if (recentRecords.size() > 0) {
                EGVRecord lastEgvRecord = recentRecords.get(recentRecords.size() - 1);
                cookieMonsterG4SGVs = EGVRecord.toProtobufList(recentRecords);
                boolean hasSensorData = (lastEgvRecord.getRawSystemTimeSeconds() > preferences.getLastEgvSysTime());
                preferences.setLastEgvSysTime(lastEgvRecord.getRawSystemTimeSeconds());

                UIDownload uiDownload = new UIDownload();
                uiDownload.download = downloadBuilder.sgv(cookieMonsterG4SGVs)
                        .download_timestamp(dateTime.toString())
                        .receiver_system_time_sec(systemTime)
                        .build();
                bus.post(uiDownload);

                if ((preferences.isRawEnabled() && hasSensorData) || (preferences.isRawEnabled() && lastSensorRecords == null)) {
                    sensorRecords = readData.getRecentSensorRecords(numOfPages * 2, systemTime, dateTime.getMillis());
                    lastSensorRecords = sensorRecords;
                } else {
                    sensorRecords = lastSensorRecords;
                    log.warn("Seems to be no new egv data. Assuming no sensor data either");
                }
                cookieMonsterG4Sensors = SensorRecord.toProtobufList(sensorRecords);

                G4Download download = downloadBuilder.sgv(cookieMonsterG4SGVs)
                        .sensor(cookieMonsterG4Sensors)
                        .receiver_system_time_sec(systemTime)
                        .download_timestamp(dateTime.toString())
                        .download_status(status)
                        .receiver_id(receiverId)
                        .receiver_battery(batLevel)
                        .uploader_battery(uploaderDevice.getBatteryLevel())
                        .transmitter_id(transmitterId)
                        .build();
                // FIXME - hack put in place to get data to the UI as soon as possible.
                // Problem was it would take 1+ minutes for BLE to respond with all datasets
                // enabled. This gets the data to the user as quickly as possible but
                // spreads the bus posts across multiple classes. This should be managed by
                // the collector service and not the download implementation.
                bus.post(download);
            }
            boolean hasCalData = false;
            if (preferences.isMeterUploadEnabled()) {
                meterRecords = readData.getRecentMeterRecords(systemTime, dateTime.getMillis());
                if (meterRecords.size() > 0) {
                    MeterRecord lastMeterRecord = meterRecords.get(meterRecords.size() - 1);
                    hasCalData = (lastMeterRecord.getRawSystemTimeSeconds() > preferences.getLastMeterSysTime());
                    preferences.setLastMeterSysTime(lastMeterRecord.getRawSystemTimeSeconds());
                }
            } else {
                hasCalData = true;
            }

            if ((preferences.isRawEnabled() && hasCalData) || (preferences.isRawEnabled() && lastCalRecords == null)) {
                calRecords = readData.getRecentCalRecords(systemTime, dateTime.getMillis());
                lastCalRecords = calRecords;
            } else {
                calRecords = lastCalRecords;
                log.warn("Seems to be no new new meter data or meter data is disabled. Assuming no cal data");
            }

            if (preferences.isInsertionUploadEnabled()) {
                log.debug("Reading insertions");
                insertionRecords = readData.getRecentInsertion(systemTime, dateTime.getMillis());
                log.debug("Number of insertion records: {}", insertionRecords.size());
            }
            if (recentRecords.size() == 0) {
                status = DownloadStatus.NO_DATA;
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
        } catch (CRCFailError e) {
            // FIXME: may consider localizing this catch at a lower level (like ReadData) so that
            // if the CRC check fails on one type of record we can capture the values if it
            // doesn't fail on other types of records. This means we'd need to broadcast back
            // partial results to the UI. Adding it to a lower level could make the ReadData class
            // more difficult to maintain - needs discussion.
            log.error("CRC failed", e);
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "CRC failed " + e);
        }

        List<CalibrationEntry> cookieMonsterG4Cals = CalRecord.toProtobufList(calRecords);
        List<MeterEntry> cookieMonsterG4Meters = MeterRecord.toProtobufList(meterRecords);
        List<InsertionEntry> cookieMonsterG4Inserts =
                InsertionRecord.toProtobufList(insertionRecords);
        log.debug("Number of insertion records (protobuf): {}", cookieMonsterG4Inserts.size());


//        downloadBuilder = new G4Download.Builder();
        downloadBuilder.cal(cookieMonsterG4Cals)
                .meter(cookieMonsterG4Meters)
                .insert(cookieMonsterG4Inserts)
                .receiver_system_time_sec(systemTime)
                .download_timestamp(dateTime.toString())
                .download_status(status)
                .uploader_battery(uploaderDevice.getBatteryLevel())
                .receiver_battery(batLevel)
                .receiver_id(receiverId)
                .transmitter_id(transmitterId);
        return downloadBuilder.build();
    }

    public class UIDownload {
        public G4Download download;
    }

    public void setNumOfPages(int numOfPages) {
        this.numOfPages = numOfPages;
    }
}
