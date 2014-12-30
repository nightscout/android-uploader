package com.nightscout.core.upload;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.preferences.TestPreferences;

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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import static com.nightscout.core.test.MockFactory.mockCalRecord;
import static com.nightscout.core.test.MockFactory.mockDeviceStatus;
import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static com.nightscout.core.test.MockFactory.mockMeterRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RestLegacyUploaderTest {
    RestLegacyUploader restUploader;
    HttpClient mockHttpClient;
    ArgumentCaptor<HttpUriRequest> captor;
    private TestPreferences preferences;

    @Before
    public void setUp() throws Exception {
        preferences = new TestPreferences();
        restUploader = new RestLegacyUploader(preferences, URI.create("http://test.com/"));
        mockHttpClient = mock(HttpClient.class);
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

    public static void verifyGlucoseDataSet(JSONObject jsonObject)
            throws JSONException {
        assertThat(jsonObject.getString("device"), is("dexcom"));
        assertThat(jsonObject.get("date"), is(not(nullValue())));
        assertThat(jsonObject.get("dateString"), is(not(nullValue())));
        assertThat(jsonObject.get("sgv"), is(not(nullValue())));
        assertThat(jsonObject.get("direction"), is(not(nullValue())));
    }

    public static void verifyDeviceStatus(JSONObject jsonObject, AbstractUploaderDevice deviceStatus)
            throws JSONException {
        assertThat(jsonObject.getInt("uploaderBattery"), is(deviceStatus.getBatteryLevel()));
    }

    @Test
    public void testGlucoseDataSet_Endpoint() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(captor.getValue().getURI().toString(), containsString("entries"));
    }

    @Test
    public void testGlucoseDataSet_Entity() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        verifyGlucoseDataSet(new JSONObject(entity));
    }

    @Test
    public void testMeterRecord_NoPost() throws Exception {
        reset(mockHttpClient);
        verifyNoMoreInteractions(mockHttpClient);
        restUploader.uploadMeterRecords(Lists.newArrayList(mockMeterRecord()));
    }

    @Test
    public void testCalRecord_NoPost() throws Exception {
        preferences.setCalibrationUploadEnabled(true);
        reset(mockHttpClient);
        verifyNoMoreInteractions(mockHttpClient);
        restUploader.uploadCalRecords(Lists.newArrayList(mockCalRecord()));
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
