package com.nightscout.core.upload;

import com.google.common.collect.Lists;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.preferences.TestPreferences;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nightscout.core.test.MockFactory.mockCalRecord;
import static com.nightscout.core.test.MockFactory.mockDeviceStatus;
import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static com.nightscout.core.test.MockFactory.mockMeterRecord;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BaseUploaderTest {
    private MockUploader mockUploader;
    private ExceptionThrowingUploader exceptionUploader;
    private TestPreferences preferences;

    class MockUploader extends BaseUploader {
        public List<CalibrationEntry> calRecords;
        public List<GlucoseDataSet> glucoseDataSets;
        public List<MeterEntry> meterRecords;

        public MockUploader(NightscoutPreferences preferences) {
            super(preferences);
            clear();
        }

        public void clear() {
            calRecords = new ArrayList<>();
            glucoseDataSets = new ArrayList<>();
            meterRecords = new ArrayList<>();
        }

        @Override
        protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
            glucoseDataSets.add(glucoseDataSet);
            return true;
        }

        @Override
        protected boolean doUpload(MeterEntry meterRecord) throws IOException {
            meterRecords.add(meterRecord);
            return true;
        }

        @Override
        protected boolean doUpload(CalibrationEntry calRecord) throws IOException {
            calRecords.add(calRecord);
            return true;
        }
    }

    class ExceptionThrowingUploader extends BaseUploader {
        public ExceptionThrowingUploader(NightscoutPreferences preferences) {
            super(preferences);
        }

        @Override
        protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
            throw new IOException("glucose");
        }

        @Override
        protected boolean doUpload(MeterEntry meterRecord) throws IOException {
            throw new IOException("meter");
        }

        @Override
        protected boolean doUpload(CalibrationEntry calRecord) throws IOException {
            throw new IOException("cal");
        }

        @Override
        protected boolean doUpload(AbstractUploaderDevice deviceStatus) throws IOException {
            throw new IOException("device");
        }
    }

    @Before
    public void setUp() throws Exception {
        preferences = new TestPreferences();
        mockUploader = new MockUploader(preferences);
        exceptionUploader = new ExceptionThrowingUploader(preferences);
    }

    @Test
    public void testUploadGlucoseDataSets_Null() {
        try {
            mockUploader.uploadGlucoseDataSets(null);
        } catch (NullPointerException e) {
            fail("Shouldn't get a NPE");
        }
    }

    @Test
    public void testUploadGlucoseDataSets_Zero() {
        mockUploader.uploadGlucoseDataSets(new ArrayList<GlucoseDataSet>());
        assertThat(mockUploader.glucoseDataSets, is(empty()));
    }

    @Test
    public void testUploadGlucoseDataSets_One() {
        List<GlucoseDataSet> list = Lists.newArrayList(mockGlucoseDataSet());
        mockUploader.uploadGlucoseDataSets(list);
        assertThat(mockUploader.glucoseDataSets, hasSize(1));
    }

    @Test
    public void testUploadGlucoseDataSets_Many() {
        List<GlucoseDataSet> list = Lists.newArrayList(
                mockGlucoseDataSet(),
                mockGlucoseDataSet(),
                mockGlucoseDataSet());
        mockUploader.uploadGlucoseDataSets(list);
        assertThat(mockUploader.glucoseDataSets, hasSize(3));
    }

    @Test
    public void testUploadGlucoseDataSets_Exception() {
        try {
            exceptionUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        } catch (Exception e) {
            fail("Shouldn't throw an exception.");
        }
    }

    @Test
    public void testUploadMeterRecords_Null() {
        try {
            mockUploader.uploadMeterRecords(null);
        } catch (NullPointerException e) {
            fail("Shouldn't get a NPE");
        }
    }

    @Test
    public void testUploadMeterRecords_Zero() {
        mockUploader.uploadMeterRecords(new ArrayList<MeterEntry>());
        assertThat(mockUploader.meterRecords, is(empty()));
    }

    @Test
    public void testUploadMeterRecords_One() throws Exception {
        List<MeterEntry> list = Lists.newArrayList(mockMeterRecord());
        mockUploader.uploadMeterRecords(list);
        assertThat(mockUploader.meterRecords, hasSize(1));
    }

    @Test
    public void testUploadMeterRecords_Many() throws Exception {
        List<MeterEntry> list = Lists.newArrayList(
                mockMeterRecord(),
                mockMeterRecord(),
                mockMeterRecord());
        mockUploader.uploadMeterRecords(list);
        assertThat(mockUploader.meterRecords, hasSize(3));
    }

    @Test
    public void testUploadMeterRecords_Exception() {
        try {
            exceptionUploader.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
        } catch (Exception e) {
            fail("Shouldn't throw an exception.");
        }
    }

    @Test
    public void testUploadCalRecords_Null() {
        preferences.setCalibrationUploadEnabled(true);
        try {
            mockUploader.uploadCalRecords(null);
        } catch (NullPointerException e) {
            fail("Shouldn't get a NPE");
        }
    }

    @Test
    public void testUploadCalRecords_Zero() {
        preferences.setCalibrationUploadEnabled(true);
        mockUploader.uploadCalRecords(new ArrayList<CalibrationEntry>());
        assertThat(mockUploader.calRecords, is(empty()));
    }

    @Test
    public void testUploadCalRecords_One() {
        preferences.setCalibrationUploadEnabled(true);
        List<CalibrationEntry> list = null;
        try {
            list = Lists.newArrayList(mockCalRecord());
        } catch (InvalidRecordLengthException e) {
            fail("Shouldn't get an exception");
        }
        mockUploader.uploadCalRecords(list);
        assertThat(mockUploader.calRecords, hasSize(1));
    }

    @Test
    public void testUploadCalRecords_Many() {
        preferences.setCalibrationUploadEnabled(true);
        List<CalibrationEntry> list = null;
        try {
            list = Lists.newArrayList(
                    mockCalRecord(),
                    mockCalRecord(),
                    mockCalRecord());
        } catch (InvalidRecordLengthException e) {
            fail("Shouldn't get an exception");
        }
        mockUploader.uploadCalRecords(list);
        assertThat(mockUploader.calRecords, hasSize(3));
    }

    @Test
    public void testUploadCalRecords_Exception() {
        preferences.setCalibrationUploadEnabled(true);
        try {
            exceptionUploader.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
        } catch (Exception e) {
            fail("Shouldn't throw an exception.");
        }
    }

    @Test
    public void testUploadDeviceStatus_Exception() {
        try {
            exceptionUploader.uploadDeviceStatus(mockDeviceStatus());
        } catch (Exception e) {
            fail("Shouldn't throw an exception.");
        }
    }
}
