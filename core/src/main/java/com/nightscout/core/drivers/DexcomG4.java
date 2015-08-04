package com.nightscout.core.drivers;

import com.nightscout.core.Timestamped;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;
import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.model.v2.SensorGlucoseValue;

import net.tribe7.common.base.Optional;
import net.tribe7.common.base.Supplier;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class DexcomG4 extends AbstractDevice {
    public static final int VENDOR_ID = 8867;
    public static final int PRODUCT_ID = 71;
    public static final int DEVICE_CLASS = 2;
    public static final int DEVICE_SUBCLASS = 0;
    public static final int PROTOCOL = 0;
    private final EventReporter eventReporter;

    protected AbstractUploaderDevice uploaderDevice;
    private Supplier<DeviceTransport> dexcomDeviceTransportSupplier;

    public DexcomG4(Supplier<DeviceTransport> dexcomDeviceTransportSupplier,
                    AbstractUploaderDevice uploaderDevice, EventReporter eventReporter) {
        this.dexcomDeviceTransportSupplier = dexcomDeviceTransportSupplier;
        this.uploaderDevice = uploaderDevice;
        this.eventReporter = checkNotNull(eventReporter);
    }

    @Override
    protected Download doDownloadAllAfter(Optional<Timestamped> timestamped) {
        DateTime downloadStartTime = new DateTime();

        Download.Builder downloadBuilder = new Download.Builder().timestamp(
            downloadStartTime.toString());
        G4Data.Builder g4DataBuilder = new G4Data.Builder();

        DeviceTransport transport = dexcomDeviceTransportSupplier.get();
        if (!transport.isConnected()) {
            getReporter().report(EventType.DEVICE, EventSeverity.WARN, "G4 device not connected.");
            return downloadBuilder.status(DownloadStatus.DEVICE_NOT_FOUND).build();
        }

        try {
            g4DataBuilder.receiver_system_time_sec(DexcomG4Driver.readSystemTime(transport))
                .receiver_battery_percent(DexcomG4Driver.readBatteryLevel(transport) / 100f)
                .receiver_id(DexcomG4Driver.readSerialNumber(transport))
                .transmitter_id(DexcomG4Driver.readTrasmitterId(transport));

            List<SensorGlucoseValue> recentRecords = DexcomG4Driver.getAllSensorGlucoseValuesAfter(transport, timestamped);
            if (recentRecords.size() == 0) {
                return downloadBuilder.status(DownloadStatus.NO_DATA).build();
            }

            g4DataBuilder
                .sensor_glucose_values(recentRecords)
                .calibrations(DexcomG4Driver.getRecentCalibrations(transport))
                .manual_meter_entries(DexcomG4Driver.getRecentManualMeterEntries(transport))
                .insertions(DexcomG4Driver.getRecentInsertions(transport))
                .raw_sensor_readings(DexcomG4Driver.getRecentRawSensorReadings(transport, 1));

            return downloadBuilder.g4_data(g4DataBuilder.build()).build();
        } catch (IOException e) {
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR,
                                 "IO error to device " + e.getMessage());
            downloadBuilder.status(DownloadStatus.IO_ERROR);
        } catch (InvalidRecordLengthException e) {
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR,
                                 "Application error " + e.getMessage());
            downloadBuilder.status(DownloadStatus.APPLICATION_ERROR);
        } catch (CRCFailError e) {
            // TODO(trhodeos): better error handling.
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR, "CRC failed " + e);
            downloadBuilder.status(DownloadStatus.IO_ERROR);
        }
        return downloadBuilder.build();

    }

    @Override
    protected EventReporter getReporter() {
        return eventReporter;
    }
}
