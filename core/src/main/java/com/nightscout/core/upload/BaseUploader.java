package com.nightscout.core.upload;

import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.records.DeviceStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseUploader {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final NightscoutPreferences preferences;

    protected abstract boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException;

    protected boolean doUpload(MeterRecord meterRecord) throws IOException {
        log.info("Meter record upload not supported.");
        return true;
    }

    protected boolean doUpload(CalRecord calRecord) throws IOException {
        log.info("Cal record upload not supported.");
        return true;
    }

    protected boolean doUpload(DeviceStatus deviceStatus) throws IOException {
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

    public final boolean uploadDeviceStatus(DeviceStatus deviceStatus) {
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
}
