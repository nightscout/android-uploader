package com.nightscout.core.upload;

import com.google.common.base.Joiner;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractRestUploader extends BaseUploader {
    private final URI uri;
    private HttpClient client;

    public AbstractRestUploader(NightscoutPreferences preferences, URI baseUri) {
        super(preferences);
        checkNotNull(baseUri);
        this.uri = baseUri;
    }

    protected void setExtraHeaders(AbstractHttpMessage httpMessage) { }

    public URI getUri() {
        return uri;
    }

    public HttpClient getClient() {
        if (client != null) {
            return client;
        }
        client = new DefaultHttpClient();
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    protected boolean doPost(String endpoint, JSONObject jsonObject) throws IOException {
        HttpPost httpPost = new HttpPost(Joiner.on('/').join(uri.toString(), endpoint));
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Accept", "application/json");
        setExtraHeaders(httpPost);
        httpPost.setEntity(new StringEntity(jsonObject.toString()));
        HttpResponse response = getClient().execute(httpPost);
        int statusCodeFamily = response.getStatusLine().getStatusCode() / 100;
        response.getEntity().consumeContent();
        return statusCodeFamily == 2;
    }
}
