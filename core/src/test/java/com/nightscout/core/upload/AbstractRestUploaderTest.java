package com.nightscout.core.upload;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.preferences.TestPreferences;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.AbstractHttpMessage;
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

import static com.nightscout.core.test.MockFactory.mockGlucoseDataSet;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractRestUploaderTest {
    private NoopRestUploader restUploader;
    private HttpClient mockHttpClient;
    private ArgumentCaptor<HttpUriRequest> captor;

    class NoopRestUploader extends AbstractRestUploader {
        public NoopRestUploader(NightscoutPreferences preferences, URI uri) {
            super(preferences, uri);
        }

        @Override
        protected void setExtraHeaders(AbstractHttpMessage httpMessage) {
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
        URI uri = URI.create("http://test.com");
        restUploader = new NoopRestUploader(new TestPreferences(), uri);
        mockHttpClient = mock(HttpClient.class);
        restUploader.setClient(mockHttpClient);
        captor = ArgumentCaptor.forClass(HttpUriRequest.class);
    }

    public void setUpExecuteCaptor() throws IOException {
        setUpExecuteCaptor(200);
    }

    public void setUpExecuteCaptor(int status) throws IOException {
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(new ProtocolVersion("mock", 1, 2), status, ""));
        response.setEntity(new StringEntity(""));
        when(mockHttpClient.execute(captor.capture())).thenReturn(response);
    }

    @Test
    public void testUploads_isPost() throws Exception {
        setUpExecuteCaptor();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(captor.getValue().getMethod(), is("POST"));
    }

    @Test
    public void testUploads_setsUrl() throws Exception {
        setUpExecuteCaptor();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(captor.getValue().getURI(), is(not(nullValue())));
        assertThat(captor.getValue().getURI().toString(), is("http://test.com/endpoint"));
    }

    @Test
    public void testUploads_setsContentType() throws Exception {
        setUpExecuteCaptor();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(captor.getValue().getFirstHeader("Content-Type"), is(not(nullValue())));
        assertThat(captor.getValue().getFirstHeader("Content-Type").getValue(),
                is("application/json"));
    }

    @Test
    public void testUploads_setsAccept() throws Exception {
        setUpExecuteCaptor();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(captor.getValue().getFirstHeader("Accept"), is(not(nullValue())));
        assertThat(captor.getValue().getFirstHeader("Accept").getValue(), is("application/json"));
    }

    @Test
    public void testUploads_setsExtraHeaders() throws Exception {
        setUpExecuteCaptor();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(captor.getValue().getFirstHeader("key").getValue(), is("value"));
    }

    @Test
    public void testUploads_setsEntity() throws IOException, InvalidRecordLengthException {
        setUpExecuteCaptor();
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        assertThat(entity, containsString("jsonKey"));
        assertThat(entity, containsString("jsonValue"));
    }

    @Test
    public void testUploads_2XXStatusCodeReturnsTrue() throws IOException, InvalidRecordLengthException {
        setUpExecuteCaptor(251);
        boolean result = restUploader.uploadGlucoseDataSets(
                Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(result, is(true));
    }

    @Test
    public void testUploads_Non200StatusCodeReturnsFalse() throws IOException, InvalidRecordLengthException {
        setUpExecuteCaptor(400);
        boolean result = restUploader.uploadGlucoseDataSets(
                Lists.newArrayList(mockGlucoseDataSet()));
        assertThat(result, is(false));
    }
}
