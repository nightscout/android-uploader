package com.nightscout.core.drivers;

import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.events.EventReporter;
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

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class DexcomG4 extends AbstractDevice {
    public static final int VENDOR_ID = 8867;
    public static final int PRODUCT_ID = 71;
    public static final int DEVICE_CLASS = 2;
    public static final int DEVICE_SUBCLASS = 0;
    public static final int PROTOCOL = 0;
    private final EventReporter eventReporter;

    protected NightscoutPreferences preferences;
    protected int numOfPages;
    protected AbstractUploader uploaderDevice;
    private ReadData readData;

    public DexcomG4(ReadData readData, NightscoutPreferences preferences,
                    AbstractUploader uploaderDevice, EventReporter eventReporter) {
        this.readData = readData;
        this.preferences = preferences;
        this.uploaderDevice = uploaderDevice;
        this.eventReporter = checkNotNull(eventReporter);
    }

    @Override
    public boolean isConnected() {
        return readData.isConnected();
    }

    @Override
    protected Download doDownload() {
        DateTime downloadStartTime = new DateTime();

        Download.Builder downloadBuilder = new Download.Builder().timestamp(
            downloadStartTime.toString());
        G4Data.Builder g4DataBuilder = new G4Data.Builder();

        if (!readData.isConnected()) {
            getReporter().report(EventType.DEVICE, EventSeverity.WARN, "G4 device not connected.");
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

            recentRecords = readData.getRecentEGVsPages(1, systemTime,
                                                        downloadStartTime.getMillis());
            if (recentRecords.size() == 0) {
                return downloadBuilder.status(DownloadStatus.NO_DATA).build();
            }
            sensorRecords = readData.getRecentSensorRecords(1, systemTime,
                                                            downloadStartTime.getMillis());
            meterRecords = readData.getRecentMeterRecords(systemTime, downloadStartTime.getMillis());
            calRecords = readData.getRecentCalRecords(systemTime, downloadStartTime.getMillis());
            insertionRecords = readData.getRecentInsertion(systemTime, downloadStartTime.getMillis());
        } catch (IOException e) {
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR,
                                 "IO error to device " + e.getMessage());
            downloadBuilder.status(DownloadStatus.IO_ERROR);
        } catch (InvalidRecordLengthException e) {
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR,
                                 "Application error " + e.getMessage());
            downloadBuilder.status(DownloadStatus.APPLICATION_ERROR);
        } catch (CRCFailError e) {
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR, "CRC failed " + e);
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

    @Override
    protected EventReporter getReporter() {
        return eventReporter;
    }

    public void setNumOfPages(int numOfPages) {
        this.numOfPages = numOfPages;
    }
}
