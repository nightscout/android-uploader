package com.nightscout.core.upload;

import com.mongodb.MongoClientURI;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.preferences.TestPreferences;

import net.tribe7.common.collect.Lists;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.nightscout.core.test.MockFactory.mockCalRecord;
import static com.nightscout.core.test.MockFactory.mockDeviceStatus;
import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static com.nightscout.core.test.MockFactory.mockMeterRecord;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MongoUploaderTest {
    @Mock
    MongoCollection<Document> mockCollection;
    @Mock
    MongoCollection<Document> mockStatusCollection;

    @Captor
    ArgumentCaptor<List<WriteModel<Document>>> captor;
    @Captor
    ArgumentCaptor<Document> statusCaptor;

    private MongoUploader mongoUploader;
    private TestPreferences preferences;
    private List<GlucoseDataSet> glucoseSets;
    private List<MeterRecord> meterRecords;
    private List<CalRecord> calRecords;
    private List<InsertionRecord> insertionRecords;
    private AbstractUploaderDevice deviceStatus;
    private int rcvrBat;

    @Before
    public void setUp() throws Exception {
        preferences = new TestPreferences();
        EventReporter reporter = new EventReporter() {
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
        mongoUploader.setDeviceStatusCollection(mockStatusCollection);
        glucoseSets = Lists.newArrayList();
        meterRecords = Lists.newArrayList();
        calRecords = Lists.newArrayList();
        insertionRecords = Lists.newArrayList();
        deviceStatus = mockDeviceStatus();
        rcvrBat = 1;
        setUpUpsertCapture();
    }

    protected boolean doUpload() {
        return mongoUploader.uploadRecords(glucoseSets, meterRecords, calRecords, insertionRecords, deviceStatus, rcvrBat);
    }

    public void verifyGlucoseDataSet(List<Document> documents, boolean enableCloudSensorData) {
        for (Document dbObject : documents) {
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
    }

    public void verifyMeterRecord(List<Document> documents) {
        for (Document dbObject : documents) {
            assertThat(dbObject.getString("device"), is("UNKNOWN"));
            assertThat(dbObject.getString("type"), is("mbg"));
            assertThat(dbObject.get("date"), is(not(nullValue())));
            assertThat(dbObject.get("dateString"), is(not(nullValue())));
            assertThat(dbObject.get("mbg"), is(not(nullValue())));
        }
    }

    public void verifyCalRecord(List<Document> documents) {
        for (Document dbObject : documents) {
            assertThat(dbObject.getString("device"), is("UNKNOWN"));
            assertThat(dbObject.getString("type"), is("cal"));
            assertThat(dbObject.get("date"), is(not(nullValue())));
            assertThat(dbObject.get("dateString"), is(not(nullValue())));
            assertThat(dbObject.get("slope"), is(not(nullValue())));
            assertThat(dbObject.get("intercept"), is(not(nullValue())));
            assertThat(dbObject.get("scale"), is(not(nullValue())));
        }
    }

    public void verifyDeviceStatus(Document dbObject, AbstractUploaderDevice deviceStatus) {
        assertThat(dbObject.getInteger("uploaderBattery"), is(deviceStatus.getBatteryLevel()));
        assertThat(dbObject.get("created_at"), is(not(nullValue())));
    }

    public void setUpUpsertCapture() {
        BulkWriteResult result = mock(BulkWriteResult.class);
        when(result.wasAcknowledged()).thenReturn(true);
        when(mockCollection.bulkWrite(captor.capture())).thenReturn(
            result);
        statusCaptor = ArgumentCaptor.forClass(Document.class);
        doNothing().when(mockStatusCollection).insertOne(statusCaptor.capture());
        validateMockitoUsage();
    }

    @Test
    public void testUploadGlucoseDataSets() {
        glucoseSets.add(mockGlucoseDataSet());
        doUpload();
        List<Document> glucoseDataDocs = getDocs(captor.getValue(), "sgv");
        assertThat(glucoseDataDocs, hasSize(1));
        verifyGlucoseDataSet(glucoseDataDocs, false);
    }

    private List<Document> getDocs(List<WriteModel<Document>> captor, String type) {
        List<Document> output = Lists.newArrayList();
        List<ReplaceOneModel<Document>> upserts = getAsUpsertList(captor);
        for (ReplaceOneModel<Document> upsert : upserts) {
            Document doc = upsert.getReplacement();
            if (type.equals(doc.get("type"))) {
                output.add(doc);
            }
        }
        return output;
    }

    private List<ReplaceOneModel<Document>> getAsUpsertList(
        List<WriteModel<Document>> documentWriteModel) {
        List<ReplaceOneModel<Document>> output = Lists.newArrayList();
        for (WriteModel<Document> doc : documentWriteModel) {
            if (!(doc instanceof ReplaceOneModel)) {
                throw new RuntimeException(doc + " not instance of ReplaceOneModel!");
            }
            output.add((ReplaceOneModel<Document>) doc);
        }
        return output;
    }

    @Test
    public void testUploadGlucoseDataSets_CloudSensorData() {
        preferences.setRawEnabled(true);
        glucoseSets.add(mockGlucoseDataSet());
        doUpload();
        List<Document> docs = getDocs(captor.getValue(), "sgv");
        assertThat(docs, hasSize(1));
        verifyGlucoseDataSet(docs, true);
    }

    @Test
    public void testUploadMeterRecord() throws Exception {
        meterRecords.add(mockMeterRecord());
        doUpload();
        List<Document> docs = getDocs(captor.getValue(), "mbg");
        assertThat(docs, hasSize(1));
        verifyMeterRecord(docs);
    }

    @Test
    public void testUploadCalRecord() {
        preferences.setRawEnabled(true);
        calRecords.add(mockCalRecord());
        doUpload();
        List<Document> docs = getDocs(captor.getValue(), "cal");
        assertThat(docs, hasSize(1));
        verifyCalRecord(docs);
    }

    @Test
    public void testUploadDeviceStatus() {
        doUpload();
        verifyDeviceStatus(statusCaptor.getValue(), mockDeviceStatus());
    }
}
