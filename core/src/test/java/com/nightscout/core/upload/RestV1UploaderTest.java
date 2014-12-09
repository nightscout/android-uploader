package com.nightscout.core.upload;

import com.google.common.collect.Lists;
import com.nightscout.core.preferences.TestPreferences;
import com.nightscout.core.records.DeviceStatus;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static com.nightscout.core.test.MockFactory.mockCalRecord;
import static com.nightscout.core.test.MockFactory.mockDeviceStatus;
import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static com.nightscout.core.test.MockFactory.mockMeterRecord;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class RestV1UploaderTest {
    private RestV1Uploader restUploader;
    private TestPreferences preferences;
    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        preferences = new TestPreferences();
        server = new MockWebServer();
        initializeMockServer();
        restUploader = new RestV1Uploader(preferences, server.getUrl("/v1"), "123");
    }

    public void initializeMockServer() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.play();
    }

    public static void verifyGlucoseDataSet(JSONObject jsonObject, boolean enableCloudSensorData)
            throws JSONException {
        assertThat(jsonObject.getString("device"), is("dexcom"));
        assertThat(jsonObject.get("date"), is(not(nullValue())));
        assertThat(jsonObject.get("dateString"), is(not(nullValue())));
        assertThat(jsonObject.get("sgv"), is(not(nullValue())));
        assertThat(jsonObject.get("direction"), is(not(nullValue())));
        assertThat(jsonObject.get("type"), is(not(nullValue())));
        if (enableCloudSensorData) {
            assertThat(jsonObject.get("filtered"), is(not(nullValue())));
            assertThat(jsonObject.get("unfiltered"), is(not(nullValue())));
            assertThat(jsonObject.get("rssi"), is(not(nullValue())));
        } else {
            assertThat(jsonObject.has("filtered"), is(false));
            assertThat(jsonObject.has("unfiltered"), is(false));
            assertThat(jsonObject.has("rssi"), is(false));
        }
    }

    public static void verifyMeterRecord(JSONObject jsonObject) throws JSONException {
        assertThat(jsonObject.getString("device"), is("dexcom"));
        assertThat(jsonObject.getString("type"), is("mbg"));
        assertThat(jsonObject.get("date"), is(not(nullValue())));
        assertThat(jsonObject.get("dateString"), is(not(nullValue())));
        assertThat(jsonObject.get("mbg"), is(not(nullValue())));
    }

    public static void verifyCalRecord(JSONObject jsonObject) throws JSONException {
        assertThat(jsonObject.getString("device"), is("dexcom"));
        assertThat(jsonObject.getString("type"), is("cal"));
        assertThat(jsonObject.get("date"), is(not(nullValue())));
        assertThat(jsonObject.get("dateString"), is(not(nullValue())));
        assertThat(jsonObject.get("slope"), is(not(nullValue())));
        assertThat(jsonObject.get("intercept"), is(not(nullValue())));
        assertThat(jsonObject.get("scale"), is(not(nullValue())));
    }

    public static void verifyDeviceStatus(JSONObject jsonObject, DeviceStatus deviceStatus)
            throws JSONException {
        assertThat(jsonObject.getInt("uploaderBattery"), is(deviceStatus.getBatteryLevel()));
    }

    public JSONObject getJSONBody() throws Exception {
        return new JSONObject(server.takeRequest().getUtf8Body());
    }

    @Test
    public void testInitialize_GenerateToken() throws Exception {
        RestV1Uploader uploader = new RestV1Uploader(preferences,
                new URL("http://test.com/v1"), "123");
        assertThat(uploader.getToken(), is(not(nullValue())));
        assertThat(uploader.getToken(), is("313233"));
    }

    @Test
    public void testInitalize_NoToken() throws Exception {
        try {
            new RestV1Uploader(preferences, new URL("http://test.com"), "");
            fail("Should not be a valid uploader.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("token"));
        }
    }

    @Test
    public void testGlucoseDataSet_Endpoint() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(server.takeRequest().getPath(), containsString("entries"));
    }

    @Test
    public void testGlucoseDataSet_Entity() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        verifyGlucoseDataSet(getJSONBody(), false);
    }

    @Test
    public void testGlucoseDataSet_EntitySensorUploadEnabled() throws Exception {
        preferences.setSensorUploadEnabled(true);
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        verifyGlucoseDataSet(getJSONBody(), true);
    }

    @Test
    public void testMeterRecord_Endpoint() throws Exception {
        restUploader.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
        assertThat(server.takeRequest().getPath(), containsString("entries"));
    }

    @Test
    public void testMeterRecord_Entity() throws Exception {
        restUploader.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
        verifyMeterRecord(getJSONBody());
    }

    @Test
    public void testCalRecord_Endpoint() throws Exception {
        preferences.setCalibrationUploadEnabled(true);
        restUploader.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
        assertThat(server.takeRequest().getPath(), containsString("entries"));
    }

    @Test
    public void testCalRecord_Entity() throws Exception {
        preferences.setCalibrationUploadEnabled(true);
        restUploader.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
        verifyCalRecord(getJSONBody());
    }

    @Test
    public void testDeviceStatus_Endpoint() throws Exception {
        restUploader.uploadDeviceStatus(mockDeviceStatus());
        assertThat(server.takeRequest().getPath(), containsString("devicestatus"));
    }

    @Test
    public void testDeviceStatus_Entity() throws Exception {
        DeviceStatus deviceStatus = mockDeviceStatus();
        restUploader.uploadDeviceStatus(deviceStatus);
        verifyDeviceStatus(getJSONBody(), deviceStatus);
    }
}
