package com.nightscout.core.listeners;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.preferences.TestPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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

public class MongoListenerTest {
    MongoListener mongoListener;
    DBCollection mockCollection;
    ArgumentCaptor<BasicDBObject> captor;
    private TestPreferences preferences;

    @Before
    public void setUp() throws Exception {
        mockCollection = mock(DBCollection.class);
        preferences = new TestPreferences();
        mongoListener = new MongoListener(
                preferences);
        mongoListener.setCollection(mockCollection);
        mongoListener.setDeviceStatusCollection(mockCollection);
        setUpUpsertCapture();
    }

    public void verifyGlucoseDataSet(boolean enableCloudSensorData) {
        BasicDBObject dbObject = captor.getValue();
        assertThat(dbObject.getString("device"), is("dexcom"));
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
        assertThat(dbObject.getString("device"), is("dexcom"));
        assertThat(dbObject.getString("type"), is("mbg"));
        assertThat(dbObject.get("date"), is(not(nullValue())));
        assertThat(dbObject.get("dateString"), is(not(nullValue())));
        assertThat(dbObject.get("mbg"), is(not(nullValue())));
    }

    public void verifyCalRecord() {
        BasicDBObject dbObject = captor.getValue();
        assertThat(dbObject.getString("device"), is("dexcom"));
        assertThat(dbObject.getString("type"), is("cal"));
        assertThat(dbObject.get("date"), is(not(nullValue())));
        assertThat(dbObject.get("dateString"), is(not(nullValue())));
        assertThat(dbObject.get("slope"), is(not(nullValue())));
        assertThat(dbObject.get("intercept"), is(not(nullValue())));
        assertThat(dbObject.get("scale"), is(not(nullValue())));
    }

    public void verifyDeviceStatus(AbstractUploaderDevice deviceStatus) {
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
        mongoListener.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        verifyGlucoseDataSet(false);
    }

    @Test
    public void testUploadGlucoseDataSets_CloudSensorData() {
        preferences.setSensorUploadEnabled(true);
        mongoListener.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        verifyGlucoseDataSet(true);
    }

    @Test
    public void testUploadMeterRecord() throws Exception {
        mongoListener.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
        verifyMeterRecord();
    }

    @Test
    public void testUploadCalRecord() {
        preferences.setCalibrationUploadEnabled(true);
        try {
            mongoListener.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
        } catch (InvalidRecordLengthException e) {
            fail("Shouldn't get an exception");
        }
        verifyCalRecord();
    }

    @Test
    public void testUploadDeviceStatus() {
        AbstractUploaderDevice deviceStatus = mockDeviceStatus();
        mongoListener.uploadDeviceStatus(deviceStatus);
        verifyDeviceStatus(deviceStatus);
    }

    @Test
    public void testReturnFalseWithInvalidURI() {
        AbstractUploaderDevice deviceStatus = mockDeviceStatus();
        assertThat(mongoListener.uploadDeviceStatus(deviceStatus), is(false));
    }
}
