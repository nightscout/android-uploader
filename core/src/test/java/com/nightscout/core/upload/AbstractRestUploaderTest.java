package com.nightscout.core.upload;

import com.google.common.collect.Lists;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.preferences.TestPreferences;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class AbstractRestUploaderTest {
    private NoopRestUploader restUploader;
    private MockWebServer server;

    class NoopRestUploader extends AbstractRestUploader {
        public NoopRestUploader(NightscoutPreferences preferences, URL url) {
            super(preferences, url);
        }

        @Override
        protected void setExtraHeaders(Request.Builder httpMessage) {
            httpMessage.addHeader("key", "value");
        }

        @Override
        protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("jsonKey", "jsonValue");
            } catch (JSONException e) {
                fail("This should never happen.");
            }
            return doPost("endpoint", jsonObject);
        }
    }

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
    }

    public void initializeMockServer() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.play();
        restUploader = new NoopRestUploader(new TestPreferences(), server.getUrl("/"));
    }

    @Test
    public void testUploads_isPost() throws Exception {
        initializeMockServer();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(server.takeRequest().getMethod(), is("POST"));
    }

    @Test
    public void testUploads_setsUrl() throws Exception {
        initializeMockServer();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(not(nullValue())));
        assertThat(request.getPath(), is("/endpoint"));
    }

    @Test
    public void testUploads_setsContentType() throws Exception {
        initializeMockServer();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Content-Type"), is(not(nullValue())));
        assertThat(request.getHeader("Content-Type"), containsString("application/json"));
    }

    @Test
    public void testUploads_setsAccept() throws Exception {
        initializeMockServer();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Accept"), is(not(nullValue())));
        assertThat(request.getHeader("Accept"), containsString("application/json"));
    }

    @Test
    public void testUploads_setsExtraHeaders() throws Exception {
        initializeMockServer();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(server.takeRequest().getHeader("key"), is("value"));
    }

    @Test
    public void testUploads_setsEntity() throws Exception {
        initializeMockServer();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        String entity = server.takeRequest().getUtf8Body();
        assertThat(entity, containsString("jsonKey"));
        assertThat(entity, containsString("jsonValue"));
    }

    @Test
    public void testUploads_2XXStatusCodeReturnsTrue() throws IOException, InvalidRecordLengthException {
        server.enqueue(new MockResponse().setResponseCode(251));
        server.play();
        restUploader = new NoopRestUploader(new TestPreferences(), server.getUrl("/"));
        boolean result = restUploader.uploadGlucoseDataSets(
                Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(result, is(true));
    }

    @Test
    public void testUploads_Non200StatusCodeReturnsFalse() throws IOException, InvalidRecordLengthException {
        server.enqueue(new MockResponse().setResponseCode(400));
        server.play();
        restUploader = new NoopRestUploader(new TestPreferences(), server.getUrl("/"));
        boolean result = restUploader.uploadGlucoseDataSets(
                Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(result, is(false));
    }
}
