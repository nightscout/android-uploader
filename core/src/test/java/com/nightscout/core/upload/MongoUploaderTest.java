package com.nightscout.core.upload;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.drivers.AbstractUploader;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.preferences.TestPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;

import static com.nightscout.core.test.MockFactory.mockCalRecord;
import static com.nightscout.core.test.MockFactory.mockDeviceStatus;
import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static com.nightscout.core.test.MockFactory.mockMeterRecord;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.when;

public class MongoUploaderTest {
    MongoUploader mongoUploader;
    DBCollection mockCollection;
    ArgumentCaptor<BasicDBObject> captor;
    private TestPreferences preferences;
    private EventReporter reporter;

    @Before
    public void setUp() throws Exception {
        mockCollection = mock(DBCollection.class);
        preferences = new TestPreferences();
        reporter = new EventReporter() {
            @Override
            public void report(EventType type, EventSeverity severity, String message) {
                // This is a stub
            }
        };
        mongoUploader = new MongoUploader(
                preferences,
                new MongoClientURI("mongodb://localhost"),
                "collection",
                "dsCollection", reporter);
        mongoUploader.setCollection(mockCollection);
        mongoUploader.setDeviceStatusCollection(mockCollection);
        setUpUpsertCapture();
    }

    public void verifyGlucoseDataSet(boolean enableCloudSensorData) {
        BasicDBObject dbObject = captor.getValue();
        assertThat(dbObject.getString("device"), is("UNKNOWN"));
        assertThat(dbObject.get("date"), is(not(nullValue())));
        assertThat(dbObject.get("dateString"), is(not(nullValue())));
        assertThat(dbObject.get("sgv"), is(not(nullValue())));
        assertThat(dbObject.get("direction"), is(not(nullValue())));
        assertThat(dbObject.get("type"), is(not(nullValue())));
        if (enableCloudSensorData) {
            assertThat(dbObject.get("filtered"), is(not(nullValue())));
            assertThat(dbObject.get("unfiltered"), is(not(nullValue())));
            assertThat(dbObject.get("rssi"), is(not(nullValue())));
        } else {
            assertThat(dbObject.get("filtered"), is(nullValue()));
            assertThat(dbObject.get("unfiltered"), is(nullValue()));
            assertThat(dbObject.get("rssi"), is(nullValue()));
        }
    }

    public void verifyMeterRecord() {
        BasicDBObject dbObject = captor.getValue();
        assertThat(dbObject.getString("device"), is("UNKNOWN"));
        assertThat(dbObject.getString("type"), is("mbg"));
        assertThat(dbObject.get("date"), is(not(nullValue())));
        assertThat(dbObject.get("dateString"), is(not(nullValue())));
        assertThat(dbObject.get("mbg"), is(not(nullValue())));
    }

    public void verifyCalRecord() {
        BasicDBObject dbObject = captor.getValue();
        assertThat(dbObject.getString("device"), is("UNKNOWN"));
        assertThat(dbObject.getString("type"), is("cal"));
        assertThat(dbObject.get("date"), is(not(nullValue())));
        assertThat(dbObject.get("dateString"), is(not(nullValue())));
        assertThat(dbObject.get("slope"), is(not(nullValue())));
        assertThat(dbObject.get("intercept"), is(not(nullValue())));
        assertThat(dbObject.get("scale"), is(not(nullValue())));
    }

    public void verifyDeviceStatus(AbstractUploader deviceStatus) {
        BasicDBObject dbObject = captor.getValue();
        assertThat(dbObject.getInt("uploaderBattery"), is(deviceStatus.getBatteryLevel()));
        assertThat(dbObject.get("created_at"), is(not(nullValue())));
    }

    public void setUpUpsertCapture() {
        captor = ArgumentCaptor.forClass(BasicDBObject.class);
        WriteResult result = mock(WriteResult.class);
        when(result.getError()).thenReturn(null);
        when(mockCollection.update(
                any(DBObject.class),
                captor.capture(),
                eq(true),
                eq(false),
                eq(WriteConcern.UNACKNOWLEDGED))).thenReturn(result);
        validateMockitoUsage();
    }

    @Test
    public void testUploadGlucoseDataSets() {
        mongoUploader.uploadGlucoseDataSets(new ArrayList<>(Arrays.asList(mockGlucoseDataSet())));
        verifyGlucoseDataSet(false);
    }

    @Test
    public void testUploadGlucoseDataSets_CloudSensorData() {
        preferences.setRawEnabled(true);
        mongoUploader.uploadGlucoseDataSets(new ArrayList<>(Arrays.asList(mockGlucoseDataSet())));
        verifyGlucoseDataSet(true);
    }

    @Test
    public void testUploadMeterRecord() throws Exception {
        mongoUploader.uploadMeterRecords(new ArrayList<>(Arrays.asList(mockMeterRecord())));

        verifyMeterRecord();
    }

    @Test
    public void testUploadCalRecord() {
        preferences.setRawEnabled(true);
        try {
            mongoUploader.uploadCalRecords(new ArrayList<>(Arrays.asList(mockCalRecord())));
        } catch (InvalidRecordLengthException e) {
            fail("Shouldn't get an exception");
        }
        verifyCalRecord();
    }

    @Test
    public void testUploadDeviceStatus() {
        AbstractUploader deviceStatus = mockDeviceStatus();
        mongoUploader.uploadDeviceStatus(deviceStatus, 100);
        verifyDeviceStatus(deviceStatus);
    }

    @Test
    public void testReturnFalseWithInvalidURI() {
        mongoUploader = new MongoUploader(
                preferences,
                new MongoClientURI("mongodb://foobar/db"),
                "collection",
                "dsCollection", reporter);
        AbstractUploader deviceStatus = mockDeviceStatus();
        assertThat(mongoUploader.uploadDeviceStatus(deviceStatus, 100), is(false));
    }

    // TODO test mismatched EGV and sensor record has expected behavior
}
