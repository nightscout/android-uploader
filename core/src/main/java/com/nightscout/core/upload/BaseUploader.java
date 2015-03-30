package com.nightscout.core.upload;

import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseUploader {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final NightscoutPreferences preferences;
    protected String identifier;
    protected String deviceStr;

    protected abstract boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException;

    protected boolean doUpload(MeterRecord meterRecord) throws IOException {
        log.info("Meter record upload not supported.");
        return true;
    }

    protected boolean doUpload(CalRecord calRecord) throws IOException {
        log.info("Cal record upload not supported.");
        return true;
    }

    protected boolean doUpload(AbstractUploaderDevice deviceStatus, int rcvrBat) throws IOException {
        log.info("Device status upload not supported.");
        return true;
    }

    public BaseUploader(NightscoutPreferences preferences) {
        checkNotNull(preferences);
        this.preferences = preferences;
        deviceStr = preferences.getDeviceType().name();
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
    public final boolean uploadMeterRecords(List<MeterRecord> meterRecords) {
        if (meterRecords == null) {
            return true;
        }
        boolean output = true;
        for (MeterRecord meterRecord : meterRecords) {
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
    public final boolean uploadCalRecords(List<CalRecord> calRecords) {
        if (calRecords == null) {
            return true;
        }
        boolean output = true;
        if (getPreferences().isCalibrationUploadEnabled()) {
            for (CalRecord calRecord : calRecords) {
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
    public final boolean uploadDeviceStatus(AbstractUploaderDevice deviceStatus, int rcvrBat) {
        if (deviceStatus == null) {
            return true;
        }
        try {
            return doUpload(deviceStatus, rcvrBat);
        } catch (IOException e) {
            log.error("Error uploading device status", e);
            return false;
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    protected NightscoutPreferences getPreferences() {
        return this.preferences;
    }

    /**
     * Upload records, can be overridden to send all data in one batch.
     *
     * @param glucoseDataSets
     * @param meterRecords
     * @param calRecords
     * @param deviceStatus
     * @return True if the (all) uploads was successful or False if at least one upload was unsuccessful.
     */
    public boolean uploadRecords(List<GlucoseDataSet> glucoseDataSets, List<MeterRecord> meterRecords, List<CalRecord> calRecords, AbstractUploaderDevice deviceStatus, int rcvrBat) {
        boolean allSuccessful = uploadGlucoseDataSets(glucoseDataSets);
        log.debug("allSuccessful after glucoseDatasets: {}", allSuccessful);
        allSuccessful &= uploadMeterRecords(meterRecords);
        log.debug("allSuccessful after meterrecords: {}", allSuccessful);
        allSuccessful &= uploadCalRecords(calRecords);
        log.debug("allSuccessful after cal records: {}", allSuccessful);
        allSuccessful &= uploadDeviceStatus(deviceStatus, rcvrBat);
        log.debug("allSuccessful after device status: {}", allSuccessful);
        return allSuccessful;
    }

}
