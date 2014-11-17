package com.nightscout.core.upload;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.preferences.TestPreferences;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.AbstractHttpMessage;
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
import static org.mockito.Mockito.verify;

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
        protected void doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("jsonKey", "jsonValue");
            } catch (JSONException e) {
                fail("This should never happen.");
            }
            doPost("endpoint", jsonObject);
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
        verify(mockHttpClient).execute(captor.capture());
    }

    @Test
    public void testUploads_isPost() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        setUpExecuteCaptor();
        assertThat(captor.getValue().getMethod(), is("POST"));
    }

    @Test
    public void testUploads_setsUrl() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        setUpExecuteCaptor();
        assertThat(captor.getValue().getURI(), is(not(nullValue())));
        assertThat(captor.getValue().getURI().toString(), is("http://test.com/endpoint"));
    }

    @Test
    public void testUploads_setsContentType() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        setUpExecuteCaptor();
        assertThat(captor.getValue().getFirstHeader("Content-Type"), is(not(nullValue())));
        assertThat(captor.getValue().getFirstHeader("Content-Type").getValue(),
                is("application/json"));
    }

    @Test
    public void testUploads_setsAccept() throws Exception{
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        setUpExecuteCaptor();
        assertThat(captor.getValue().getFirstHeader("Accept"), is(not(nullValue())));
        assertThat(captor.getValue().getFirstHeader("Accept").getValue(), is("application/json"));
    }

    @Test
    public void testUploads_setsExtraHeaders() throws Exception {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        setUpExecuteCaptor();
        assertThat(captor.getValue().getFirstHeader("key").getValue(), is("value"));
    }

    @Test
    public void testUploads_setsEntity() throws IOException {
        restUploader.uploadGlucoseDataSets(Lists.newArrayList(mockGlucoseDataSet()));
        setUpExecuteCaptor();
        HttpPost post = (HttpPost) captor.getValue();
        String entity = CharStreams.toString(new InputStreamReader(post.getEntity().getContent()));
        assertThat(entity, containsString("jsonKey"));
        assertThat(entity, containsString("jsonValue"));
    }
}
