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

    protected abstract void doUpload(GlucoseDataSet glucoseDataSet) throws IOException;

    protected void doUpload(MeterRecord meterRecord) throws IOException {
        log.info("Meter record upload not supported.");
    }

    protected void doUpload(CalRecord calRecord) throws IOException {
        log.info("Cal record upload not supported.");
    }

    protected void doUpload(DeviceStatus deviceStatus) throws IOException {
        log.info("Device status upload not supported.");
    }

    public BaseUploader(NightscoutPreferences preferences) {
        checkNotNull(preferences);
        this.preferences = preferences;
    }

    // TODO(trhodeos): implement some sort of retry logic in all of these public functions.
    public final void uploadGlucoseDataSets(List<GlucoseDataSet> glucoseDataSets) {
        if (glucoseDataSets == null) {
            return;
        }
        for (GlucoseDataSet glucoseDataSet : glucoseDataSets) {
            try {
                doUpload(glucoseDataSet);
            } catch (IOException e) {
                log.error("Error uploading glucose data set.", e);
            }
        }
    }

    public final void uploadMeterRecords(List<MeterRecord> meterRecords) {
        if (meterRecords == null) {
            return;
        }
        for (MeterRecord meterRecord : meterRecords) {
            try {
                doUpload(meterRecord);
            } catch (IOException e) {
                log.error("Error uploading meter record.", e);
            }
        }
    }

    public final void uploadCalRecords(List<CalRecord> calRecords) {
        if (calRecords == null) {
            return;
        }
        if (getPreferences().isCalibrationUploadEnabled()) {
            for (CalRecord calRecord : calRecords) {
                try {
                    doUpload(calRecord);
                } catch (IOException e) {
                    log.error("Error uploading calibration record.", e);
                }
            }
        }
    }

    public final void uploadDeviceStatus(DeviceStatus deviceStatus) {
        if (deviceStatus == null) {
            return;
        }
        try {
            doUpload(deviceStatus);
        } catch (IOException e) {
            log.error("Error uploading device status", e);
        }
    }

    protected NightscoutPreferences getPreferences() {
        return this.preferences;
    }
}
