package com.nightscout.core.upload;

import com.google.common.base.Joiner;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractRestUploader extends BaseUploader {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final URL url;
    // TODO(trhodeos): dependency injection.
    private OkHttpClient client;

    public AbstractRestUploader(NightscoutPreferences preferences, URL baseUrl) {
        super(preferences);
        checkNotNull(baseUrl);
        this.url = baseUrl;
    }

    protected void setExtraHeaders(Request.Builder builder) { }

    public OkHttpClient getClient() {
        if (client != null) {
            return client;
        }
        client = new OkHttpClient();
        return client;
    }

    protected String getEndpointUrl(String endpoint) {
        String output;
        if (url.toString().endsWith("/")) {
            output = url.toString() + endpoint;
        } else {
            output = Joiner.on('/').join(url.toString(), endpoint);
        }
        return output;
    }

    protected boolean doPost(String endpoint, JSONObject jsonObject) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(getEndpointUrl(endpoint))
                .post(RequestBody.create(JSON, jsonObject.toString()))
                .addHeader("Content-Type", JSON.toString())
                .addHeader("Accept", JSON.toString());
        setExtraHeaders(builder);
        return getClient().newCall(builder.build()).execute().isSuccessful();
    }
}
