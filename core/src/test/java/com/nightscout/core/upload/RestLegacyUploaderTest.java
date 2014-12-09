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

import static com.nightscout.core.test.MockFactory.mockCalRecord;
import static com.nightscout.core.test.MockFactory.mockDeviceStatus;
import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static com.nightscout.core.test.MockFactory.mockMeterRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class RestLegacyUploaderTest {
    RestLegacyUploader restUploader;
    MockWebServer server;
    private TestPreferences preferences;

    @Before
    public void setUp() throws Exception {
        preferences = new TestPreferences();
        server = new MockWebServer();
        initializeMockServer();
        restUploader = new RestLegacyUploader(preferences, server.getUrl("/"));
    }

    public void initializeMockServer() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.play();
    }

    public JSONObject getJSONBody() throws Exception {
        return new JSONObject(server.takeRequest().getUtf8Body());
    }

    public static void verifyGlucoseDataSet(JSONObject jsonObject)
            throws JSONException {
        assertThat(jsonObject.getString("device"), is("dexcom"));
        assertThat(jsonObject.get("date"), is(not(nullValue())));
        assertThat(jsonObject.get("dateString"), is(not(nullValue())));
        assertThat(jsonObject.get("sgv"), is(not(nullValue())));
        assertThat(jsonObject.get("direction"), is(not(nullValue())));
    }

    public static void verifyDeviceStatus(JSONObject jsonObject, DeviceStatus deviceStatus)
            throws JSONException {
        assertThat(jsonObject.getInt("uploaderBattery"), is(deviceStatus.getBatteryLevel()));
    }

    @Test
    public void testGlucoseDataSet_Endpoint() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(server.takeRequest().getPath(), containsString("entries"));
    }

    @Test
    public void testGlucoseDataSet_Entity() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        verifyGlucoseDataSet(getJSONBody());
    }

    @Test
    public void testMeterRecord_NoPost() throws Exception {
        restUploader.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
        assertThat(server.getRequestCount(), is(0));
    }

    @Test
    public void testCalRecord_NoPost() throws Exception {
        preferences.setCalibrationUploadEnabled(true);
        restUploader.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
        assertThat(server.getRequestCount(), is(0));
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
