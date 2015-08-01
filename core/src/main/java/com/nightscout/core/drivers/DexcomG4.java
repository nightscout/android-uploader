package com.nightscout.core.drivers;

import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;
import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.preferences.NightscoutPreferences;

import net.tribe7.common.collect.Lists;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DexcomG4 extends AbstractDevice {
    public static final int VENDOR_ID = 8867;
    public static final int PRODUCT_ID = 71;
    public static final int DEVICE_CLASS = 2;
    public static final int DEVICE_SUBCLASS = 0;
    public static final int PROTOCOL = 0;

    protected NightscoutPreferences preferences;
    protected int numOfPages;
    protected AbstractUploaderDevice uploaderDevice;
    private ReadData readData;

    public DexcomG4(ReadData readData, NightscoutPreferences preferences,
                    AbstractUploaderDevice uploaderDevice) {
        this.readData = readData;
        this.preferences = preferences;
        this.uploaderDevice = uploaderDevice;
        this.deviceType = preferences.getDeviceType();
    }

    @Override
    public void onConnect() {
        super.onConnect();
        log.debug("onConnect Called DexcomG4 connection");
    }

    @Override
    public boolean isConnected() {
        return readData.isConnected();
    }

    @Override
    protected Download doDownload() {
        DateTime downloadDateTime = new DateTime();

        Download.Builder downloadBuilder = new Download.Builder().timestamp(downloadDateTime.toString());
        G4Data.Builder g4DataBuilder = new G4Data.Builder();

        if (!readData.isConnected()) {
            if (reporter != null) {
                reporter.report(EventType.DEVICE, EventSeverity.WARN,
                                messages.getString("event_g4_not_connected"));
            }
            return downloadBuilder.status(DownloadStatus.DEVICE_NOT_FOUND).build();
        }

        List<EGVRecord> recentRecords = new ArrayList<>();
        List<MeterRecord> meterRecords = new ArrayList<>();
        List<SensorRecord> sensorRecords = new ArrayList<>();
        List<CalRecord> calRecords = new ArrayList<>();
        List<InsertionRecord> insertionRecords = new ArrayList<>();

        int batLevel = 100;
        long systemTime = 0;
        String receiverId = "";
        String transmitterId = "";
        try {
            receiverId = readData.readSerialNumber();
            transmitterId = readData.readTrasmitterId();

            systemTime = readData.readSystemTime();
            batLevel = readData.readBatteryLevel();

            recentRecords = readData.getRecentEGVsPages(numOfPages, systemTime,
                                                        downloadDateTime.getMillis());
            if (recentRecords.size() == 0) {
                return downloadBuilder.status(DownloadStatus.NO_DATA).build();
            }
            sensorRecords = readData.getRecentSensorRecords(numOfPages, systemTime,
                                                            downloadDateTime.getMillis());
            meterRecords = readData.getRecentMeterRecords(systemTime, downloadDateTime.getMillis());
            calRecords = readData.getRecentCalRecords(systemTime, downloadDateTime.getMillis());
            insertionRecords = readData.getRecentInsertion(systemTime, downloadDateTime.getMillis());
        } catch (IOException e) {
            //TODO record this in the event log later
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "IO error to device");
            log.error("IO error to device " + e);

            downloadBuilder.status(DownloadStatus.IO_ERROR);
        } catch (InvalidRecordLengthException e) {
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "Application error " + e.getMessage());
            log.error("Application error " + e);
            downloadBuilder.status(DownloadStatus.APPLICATION_ERROR);
        } catch (CRCFailError e) {
            log.error("CRC failed", e);
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "CRC failed " + e);
        }

        g4DataBuilder
            .sensor_glucose_values(Lists.transform(recentRecords, EGVRecord.v2ModelConverter()))
            .calibrations(Lists.transform(calRecords, CalRecord.v2ModelConverter()))
            .manual_meter_entries(Lists.transform(meterRecords, MeterRecord.v2ModelConverter()))
            .insertions(Lists.transform(insertionRecords, InsertionRecord.v2ModelConverter()))
            .raw_sensor_readings(Lists.transform(sensorRecords, SensorRecord.v2ModelConverter()))
            .receiver_system_time_sec(systemTime)
            .receiver_battery_percent(batLevel / 100f)
            .receiver_id(receiverId)
            .transmitter_id(transmitterId);
        return downloadBuilder.g4_data(g4DataBuilder.build()).build();
    }

    public void setNumOfPages(int numOfPages) {
        this.numOfPages = numOfPages;
    }
}
