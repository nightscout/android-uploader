package com.nightscout.core.drivers;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.DownloadResults;
import com.nightscout.core.model.DownloadStatus;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.model.SensorEntry;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.joda.time.Duration.standardMinutes;
import static org.joda.time.Duration.standardSeconds;

public class DexcomG4 extends AbstractDevice {
    public static final int VENDOR_ID = 8867;
    public static final int PRODUCT_ID = 71;
    public static final int DEVICE_CLASS = 2;
    public static final int DEVICE_SUBCLASS = 0;
    public static final int PROTOCOL = 0;

    protected NightscoutPreferences preferences;
    protected int numOfPages;
    protected AbstractUploaderDevice uploaderDevice;

    public DexcomG4(DeviceTransport transport, NightscoutPreferences preferences,
                    AbstractUploaderDevice uploaderDevice) {
        super(transport);
        this.preferences = preferences;
        this.uploaderDevice = uploaderDevice;
        this.deviceName = "DexcomG4";
    }

    public static boolean isConnected(DeviceTransport transport) {
        return transport.isConnected(VENDOR_ID, PRODUCT_ID, DEVICE_CLASS, DEVICE_SUBCLASS, PROTOCOL);
    }

    @Override
    public boolean isConnected() {
        return transport.isConnected(VENDOR_ID, PRODUCT_ID, DEVICE_CLASS, DEVICE_SUBCLASS, PROTOCOL);
    }

    @Override
    protected DownloadResults doDownload() {
        DownloadStatus status = DownloadStatus.SUCCESS;
        try {
            transport.open();
        } catch (IOException e) {
            //TODO record this in the event log later
            status = DownloadStatus.IO_ERROR;
        }
        ReadData readData = new ReadData(transport);

        List<EGVRecord> recentRecords = new ArrayList<>();
        List<MeterRecord> meterRecords = new ArrayList<>();
        List<SensorRecord> sensorRecords = new ArrayList<>();
        List<CalRecord> calRecords = new ArrayList<>();
        long displayTime = 0;
        long timeSinceLastRecord = 0;
        int batLevel = 100;
        long systemTime = 0;
        if (status == DownloadStatus.SUCCESS) {
            try {
                recentRecords = readData.getRecentEGVsPages(numOfPages);
                meterRecords = readData.getRecentMeterRecords();
                if (preferences.isSensorUploadEnabled()) {
                    sensorRecords = readData.getRecentSensorRecords(numOfPages);
                }

                if (preferences.isCalibrationUploadEnabled()) {
                    calRecords = readData.getRecentCalRecords();
                }
                if (recentRecords.size() == 0) {
                    status = DownloadStatus.NO_DATA;
                }

                displayTime = readData.readDisplayTime().getTime();
                if (status == DownloadStatus.SUCCESS && recentRecords.size() > 0) {
                    timeSinceLastRecord = readData.getTimeSinceEGVRecord(
                            recentRecords.get(recentRecords.size() - 1));
                }
                systemTime = readData.readSystemTime();
                // FIXME: readData.readBatteryLevel() seems to flake out on battery level reads.
                // Removing for now.
                batLevel = 100;
                // TODO pull in other exceptions once we have the analytics/acra reporters
            } catch (IOException e) {
                //TODO record this in the event log later
                status = DownloadStatus.IO_ERROR;
            } catch (InvalidRecordLengthException e) {
                status = DownloadStatus.APPLICATION_ERROR;
            } finally {
                try {
                    transport.close();
                } catch (IOException e) {
                    //TODO record this in the event log later
                    status = DownloadStatus.IO_ERROR;
                }
            }
        }

        List<SensorGlucoseValueEntry> cookieMonsterG4SGVs = EGVRecord.toProtobufList(recentRecords);
        List<CalibrationEntry> cookieMonsterG4Cals = CalRecord.toProtobufList(calRecords);
        List<MeterEntry> cookieMonsterG4Meters = MeterRecord.toProtobufList(meterRecords);
        List<SensorEntry> cookieMonsterG4Sensors =
                SensorRecord.toProtobufList(sensorRecords);

        G4Download.Builder downloadBuilder = new G4Download.Builder();
        downloadBuilder.sgv(cookieMonsterG4SGVs)
                .cal(cookieMonsterG4Cals)
                .sensor(cookieMonsterG4Sensors)
                .meter(cookieMonsterG4Meters)
                .receiver_system_time_sec(systemTime)
                .download_timestamp(new Date().toString())
                .download_status(status)
                .uploader_battery(uploaderDevice.getBatteryLevel())
                .receiver_battery(batLevel)
                .units(GlucoseUnit.MGDL);


        // TODO: determine if the logic here is correct. I suspect it assumes the last record was
        // less than 5
        // minutes ago. If a reading is skipped and the device is plugged in then nextUploadTime
        // will be set to a negative number. This situation will eventually correct itself.
        long nextUploadTime = standardMinutes(5).minus(standardSeconds(timeSinceLastRecord))
                .getMillis();


        // convert into json for d3 plot
        JSONArray array = new JSONArray();
        for (EGVRecord recentRecord : recentRecords) {
            try {
                array.put(recentRecord.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new DownloadResults(downloadBuilder.build(), nextUploadTime, array, displayTime);
    }

    public void setNumOfPages(int numOfPages) {
        this.numOfPages = numOfPages;
    }
}
