package com.nightscout.core.upload;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploader;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.preferences.TestPreferences;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        public List<CalRecord> calRecords;
        public List<GlucoseDataSet> glucoseDataSets;
        public List<MeterRecord> meterRecords;
        public List<InsertionRecord> insertionRecords;

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
        protected boolean doUpload(MeterRecord meterRecord) throws IOException {
            meterRecords.add(meterRecord);
            return true;
        }

        @Override
        protected boolean doUpload(CalRecord calRecord) throws IOException {
            calRecords.add(calRecord);
            return true;
        }

        @Override
        protected boolean doUpload(InsertionRecord insertionRecord) throws IOException {
            insertionRecords.add(insertionRecord);
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
        protected boolean doUpload(MeterRecord meterRecord) throws IOException {
            throw new IOException("meter");
        }

        @Override
        protected boolean doUpload(CalRecord calRecord) throws IOException {
            throw new IOException("cal");
        }

        @Override
        protected boolean doUpload(InsertionRecord insertionRecord) throws IOException {
            throw new IOException("insertion");
        }

        @Override
        protected boolean doUpload(AbstractUploader deviceStatus, int receiverBattery) throws IOException {
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
        List<GlucoseDataSet> list = new ArrayList<>(Arrays.asList(mockGlucoseDataSet()));
        mockUploader.uploadGlucoseDataSets(list);
        assertThat(mockUploader.glucoseDataSets, hasSize(1));
    }

    @Test
    public void testUploadGlucoseDataSets_Many() {
//        List<GlucoseDataSet> list = Lists.newArrayList(
//                mockGlucoseDataSet(),
//                mockGlucoseDataSet(),
//                mockGlucoseDataSet());
        List<GlucoseDataSet> list = new ArrayList<>(Arrays.asList(new GlucoseDataSet[]{mockGlucoseDataSet(), mockGlucoseDataSet(), mockGlucoseDataSet()}));
        mockUploader.uploadGlucoseDataSets(list);
        assertThat(mockUploader.glucoseDataSets, hasSize(3));
    }

    @Test
    public void testUploadGlucoseDataSets_Exception() {
        try {
            exceptionUploader.uploadGlucoseDataSets(new ArrayList<>(Arrays.asList(mockGlucoseDataSet())));
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
        mockUploader.uploadMeterRecords(new ArrayList<MeterRecord>());
        assertThat(mockUploader.meterRecords, is(empty()));
    }

    @Test
    public void testUploadMeterRecords_One() throws Exception {
        List<MeterRecord> list = new ArrayList<>(Arrays.asList(mockMeterRecord()));
        mockUploader.uploadMeterRecords(list);
        assertThat(mockUploader.meterRecords, hasSize(1));
    }

    @Test
    public void testUploadMeterRecords_Many() throws Exception {
        List<MeterRecord> list = new ArrayList<>(Arrays.asList(new MeterRecord[]{mockMeterRecord(), mockMeterRecord(), mockMeterRecord()}));
        mockUploader.uploadMeterRecords(list);
        assertThat(mockUploader.meterRecords, hasSize(3));
    }

    @Test
    public void testUploadMeterRecords_Exception() {
        try {
            exceptionUploader.uploadMeterRecords(new ArrayList<>(Arrays.asList(mockMeterRecord())));
        } catch (Exception e) {
            fail("Shouldn't throw an exception.");
        }
    }

    @Test
    public void testUploadCalRecords_Null() {
        preferences.setRawEnabled(true);
        try {
            mockUploader.uploadCalRecords(null);
        } catch (NullPointerException e) {
            fail("Shouldn't get a NPE");
        }
    }

    @Test
    public void testUploadCalRecords_Zero() {
        preferences.setRawEnabled(true);
        mockUploader.uploadCalRecords(new ArrayList<CalRecord>());
        assertThat(mockUploader.calRecords, is(empty()));
    }

    @Test
    public void testUploadCalRecords_One() {
        preferences.setRawEnabled(true);
        List<CalRecord> list = null;
        try {
            list = new ArrayList<>(Arrays.asList(mockCalRecord()));
        } catch (InvalidRecordLengthException e) {
            fail("Shouldn't get an exception");
        }
        mockUploader.uploadCalRecords(list);
        assertThat(mockUploader.calRecords, hasSize(1));
    }

    @Test
    public void testUploadCalRecords_Many() {
        preferences.setRawEnabled(true);
        List<CalRecord> list = null;
        try {
            list = new ArrayList<>(Arrays.asList(new CalRecord[]{mockCalRecord(), mockCalRecord(), mockCalRecord()}));
        } catch (InvalidRecordLengthException e) {
            fail("Shouldn't get an exception");
        }
        mockUploader.uploadCalRecords(list);
        assertThat(mockUploader.calRecords, hasSize(3));
    }

    @Test
    public void testUploadCalRecords_Exception() {
        preferences.setRawEnabled(true);
        try {
            exceptionUploader.uploadCalRecords(new ArrayList<>(Arrays.asList(mockCalRecord())));
        } catch (Exception e) {
            fail("Shouldn't throw an exception.");
        }
    }

    @Test
    public void testUploadDeviceStatus_Exception() {
        try {
            exceptionUploader.uploadDeviceStatus(mockDeviceStatus(), 100);
        } catch (Exception e) {
            fail("Shouldn't throw an exception.");
        }
    }
}
