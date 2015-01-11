package com.nightscout.core.upload;

import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseUploader {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final NightscoutPreferences preferences;

    protected abstract boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException;

    protected boolean doUpload(MeterEntry meterRecord) throws IOException {
        log.info("Meter record upload not supported.");
        return true;
    }

    protected boolean doUpload(CalibrationEntry calRecord) throws IOException {
        log.info("Cal record upload not supported.");
        return true;
    }

    protected boolean doUpload(AbstractUploaderDevice deviceStatus) throws IOException {
        log.info("Device status upload not supported.");
        return true;
    }

    public BaseUploader(NightscoutPreferences preferences) {
        checkNotNull(preferences);
        this.preferences = preferences;
    }

    // TODO(trhodeos): implement some sort of retry logic in all of these public functions.
    public final boolean uploadGlucoseDataSets(List<GlucoseDataSet> glucoseDataSets) {
        if (glucoseDataSets == null) {
            return true;
        }
        boolean output = true;
        for (GlucoseDataSet glucoseDataSet : glucoseDataSets) {
            try {
                output &= doUpload(glucoseDataSet);
            } catch (IOException e) {
                log.error("Error uploading glucose data set.", e);
                output = false;
            }
        }
        return output;
    }

    /**
     * Uploads the meter records
     *
     * @param meterRecords
     * @return True if the upload was successful, false if the upload was unsuccessful
     */
    public final boolean uploadMeterRecords(List<MeterEntry> meterRecords) {
        if (meterRecords == null) {
            return true;
        }
        boolean output = true;
        for (MeterEntry meterRecord : meterRecords) {
            try {
                output &= doUpload(meterRecord);
            } catch (IOException e) {
                log.error("Error uploading meter record.", e);
                output = false;
            }
        }
        return output;
    }

    /**
     * Uploads the calibration records
     *
     * @param calRecords
     * @return True if the upload was successful, false if the upload was unsuccessful
     */
    public final boolean uploadCalRecords(List<CalibrationEntry> calRecords) {
        if (calRecords == null) {
            return true;
        }
        boolean output = true;
        if (getPreferences().isCalibrationUploadEnabled()) {
            for (CalibrationEntry calRecord : calRecords) {
                try {
                    output &= doUpload(calRecord);
                } catch (IOException e) {
                    log.error("Error uploading calibration record.", e);
                    output = false;
                }
            }
        }
        return output;
    }

    /**
     * Uploads the device status
     *
     * @param deviceStatus
     * @return True if the upload was successful or False if the upload was unsuccessful
     */
    public final boolean uploadDeviceStatus(AbstractUploaderDevice deviceStatus) {
        if (deviceStatus == null) {
            return true;
        }
        try {
            return doUpload(deviceStatus);
        } catch (IOException e) {
            log.error("Error uploading device status", e);
            return false;
        }
    }

    protected NightscoutPreferences getPreferences() {
        return this.preferences;
    }

    /**
     * Upload records, can be overridden to send all data in one batch.
     * @param glucoseDataSets
     * @param meterRecords
     * @param calRecords
     * @param deviceStatus
     * @return True if the (all) uploads was successful or False if at least one upload was unsuccessful.
     */
    public boolean uploadRecords(List<GlucoseDataSet> glucoseDataSets, List<MeterEntry> meterRecords, List<CalibrationEntry> calRecords, AbstractUploaderDevice deviceStatus) {
        boolean allSuccessful = uploadGlucoseDataSets(glucoseDataSets);
        allSuccessful &= uploadMeterRecords(meterRecords);
        allSuccessful &= uploadCalRecords(calRecords);
        allSuccessful &= uploadDeviceStatus(deviceStatus);
        return allSuccessful;
    }
}
