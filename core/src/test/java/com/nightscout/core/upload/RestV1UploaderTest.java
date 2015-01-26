package com.nightscout.core.upload;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.preferences.TestPreferences;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

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
import static org.mockito.Mockito.when;

public class RestV1UploaderTest {
    private RestV1Uploader restUploader;
    private HttpClient mockHttpClient;
    private ArgumentCaptor<HttpUriRequest> captor;
    private TestPreferences preferences;

    @Before
    public void setUp() throws Exception {
        preferences = new TestPreferences();
        restUploader = new RestV1Uploader(preferences, URI.create("http://testingtesting@test.com/v1"));
        mockHttpClient = Mockito.mock(HttpClient.class);
        restUploader.setClient(mockHttpClient);
        setUpExecuteCaptor();
    }

    public void setUpExecuteCaptor() throws IOException {
        setUpExecuteCaptor(200);
    }

    public void setUpExecuteCaptor(int status) throws IOException {
        captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(new ProtocolVersion("mock", 1, 2), status, ""));
        response.setEntity(new StringEntity(""));
        when(mockHttpClient.execute(captor.capture())).thenReturn(response);
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
            assertThat(jsonObject.get("noise"), is(not(nullValue())));
        } else {
            assertThat(jsonObject.has("filtered"), is(false));
            assertThat(jsonObject.has("unfiltered"), is(false));
            assertThat(jsonObject.has("rssi"), is(false));
            assertThat(jsonObject.has("noise"), is(false));
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

    public static void verifyDeviceStatus(JSONObject jsonObject, AbstractUploaderDevice deviceStatus)
            throws JSONException {
        assertThat(jsonObject.getInt("uploaderBattery"), is(deviceStatus.getBatteryLevel()));
    }

    @Test
    public void testInitialize_StripUserInfo() {
        RestV1Uploader uploader = new RestV1Uploader(preferences,
                URI.create("http://testingtesting@test.com/v1"));
        assertThat(uploader.getUri().toString(), is("http://test.com/v1"));
    }

    @Test
    public void testInitialize_GenerateToken() {
        RestV1Uploader uploader = new RestV1Uploader(preferences,
                URI.create("http://testingtesting@test.com/v1"));
        assertThat(uploader.getSecret(), is(not(nullValue())));
        assertThat(uploader.getSecret(), is("b0212be2cc6081fba3e0b6f3dc6e0109d6f7b4cb"));
    }

    @Test
    public void testInitalize_NoToken() {
        try {
            new RestV1Uploader(preferences, URI.create("http://test.com"));
            fail("Should not be a valid uploader.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("token"));
        }
    }

    @Test
    public void testGlucoseDataSet_Endpoint() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(captor.getValue().getURI().toString(), containsString("entries"));
    }

    @Test
    public void testAPISecret() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        HttpPost post = (HttpPost) captor.getValue();
        Header[] headers = post.getHeaders("api-secret");
        assertThat(headers.length, is(1));
        assertThat(headers[0].getValue(), is("b0212be2cc6081fba3e0b6f3dc6e0109d6f7b4cb"));
    }

    @Test
    public void testGlucoseDataSet_Entity() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        verifyGlucoseDataSet(new JSONObject(entity), false);
    }

    @Test
    public void testGlucoseDataSet_EntitySensorUploadEnabled() throws Exception {
        preferences.setSensorUploadEnabled(true);
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        verifyGlucoseDataSet(new JSONObject(entity), true);
    }

    @Test
    public void testMeterRecord_Endpoint() throws Exception {
        restUploader.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
        assertThat(captor.getValue().getURI().toString(), containsString("entries"));
    }

    @Test
    public void testMeterRecord_Entity() throws Exception {
        restUploader.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        verifyMeterRecord(new JSONObject(entity));
    }

    @Test
    public void testCalRecord_Endpoint() throws Exception {
        preferences.setCalibrationUploadEnabled(true);
        restUploader.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
        assertThat(captor.getValue().getURI().toString(), containsString("entries"));
    }

    @Test
    public void testCalRecord_Entity() throws Exception {
        preferences.setCalibrationUploadEnabled(true);
        restUploader.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        verifyCalRecord(new JSONObject(entity));
    }

    @Test
    public void testDeviceStatus_Endpoint() throws Exception {
        restUploader.uploadDeviceStatus(mockDeviceStatus());
        assertThat(captor.getValue().getURI().toString(), containsString("devicestatus"));
    }

    @Test
    public void testDeviceStatus_Entity() throws Exception {
        AbstractUploaderDevice deviceStatus = mockDeviceStatus();
        restUploader.uploadDeviceStatus(deviceStatus);
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        verifyDeviceStatus(new JSONObject(entity), deviceStatus);
    }
}
