package com.nightscout.android.upload;

import android.content.Intent;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.nightscout.android.R;
import com.nightscout.android.ToastReceiver;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.preferences.TestPreferences;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.upload.RestV1Uploader;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class UploaderTest extends RobolectricTestBase {

    @Test
    public void shouldSendToastIntentOnInvalidMongoUri() throws Exception {
        TestPreferences prefs = new TestPreferences();
        prefs.setMongoUploadEnabled(true);
        prefs.setMongoClientUri(null);

        whenOnBroadcastReceived(ToastReceiver.ACTION_SEND_NOTIFICATION,
                new Function<Intent, Void>() {
                    @Override
                    public Void apply(Intent input) {
                        assertThat(input.getStringExtra(ToastReceiver.TOAST_MESSAGE), is(not(nullValue())));
                        assertThat(input.getStringExtra(ToastReceiver.TOAST_MESSAGE),
                                is(getContext().getString(R.string.unknown_mongo_host)));
                        return null;
                    }
                });

        new Uploader(getContext(), prefs);
        assertIntentSeen();
    }

    @Test
    public void initializeShouldFailOnInvalidMongoUri() throws Exception {
        TestPreferences prefs = new TestPreferences();
        prefs.setMongoUploadEnabled(true);
        prefs.setMongoClientUri("http://test.com");
        Uploader uploader = new Uploader(getContext(), prefs);
        assertThat(uploader.areAllUploadersInitialized(), is(false));
    }

    @Test
    public void initalizedSecretIsEncodedInUri() throws Exception {
        TestPreferences prefs = new TestPreferences();
        prefs.setRestApiEnabled(true);
        List<String> uris = new ArrayList<>();
        uris.add("https://#ABC123DEF56@test.com/api/v1");
        prefs.setRestApiBaseUris(uris);
        Uploader uploader = new Uploader(getContext(), prefs);
        List<BaseUploader> uploaders = uploader.getUploaders();
        assertThat(uploaders.get(0).getClass().getName(), is(RestV1Uploader.class.getName()));
        assertThat(((RestV1Uploader) uploaders.get(0)).getUri().getUserInfo(), is("#ABC123DEF56"));
    }

    @Test
    public void initalizeWithPercentEncodedSecretIsEncodedInUri() throws Exception {
        TestPreferences prefs = new TestPreferences();
        prefs.setRestApiEnabled(true);
        List<String> uris = new ArrayList<>();
        uris.add("https://%23ABC123DEF56@mytest.com/api/v1");
        prefs.setRestApiBaseUris(uris);
        Uploader uploader = new Uploader(getContext(), prefs);
        List<BaseUploader> uploaders = uploader.getUploaders();
        assertThat(uploaders.get(0).getClass().getName(), is(RestV1Uploader.class.getName()));
        assertThat(((RestV1Uploader) uploaders.get(0)).getUri().getUserInfo(), is("#ABC123DEF56"));
    }

    @Test
    public void shouldSendToastIntentOnInvalidRestv1Uri() throws Exception {
        TestPreferences prefs = new TestPreferences();
        prefs.setRestApiEnabled(true);
        prefs.setRestApiBaseUris(Lists.newArrayList("http://test/v1"));

        whenOnBroadcastReceived(ToastReceiver.ACTION_SEND_NOTIFICATION,
                new Function<Intent, Void>() {
                    @Override
                    public Void apply(Intent input) {
                        assertThat(input.getStringExtra(ToastReceiver.TOAST_MESSAGE), is(not(nullValue())));
                        assertThat(input.getStringExtra(ToastReceiver.TOAST_MESSAGE),
                                is(getContext().getString(R.string.illegal_rest_url)));
                        return null;
                    }
                });
        new Uploader(getContext(), prefs);
        assertIntentSeen();
    }

    @Test
    public void shouldSendToastIntentOnInvalidRestUri() throws Exception {
        TestPreferences prefs = new TestPreferences();
        prefs.setRestApiEnabled(true);
        prefs.setRestApiBaseUris(Lists.newArrayList("\\invalid"));

        whenOnBroadcastReceived(ToastReceiver.ACTION_SEND_NOTIFICATION,
                new Function<Intent, Void>() {
                    @Override
                    public Void apply(Intent input) {
                        assertThat(input.getStringExtra(ToastReceiver.TOAST_MESSAGE), is(not(nullValue())));
                        assertThat(input.getStringExtra(ToastReceiver.TOAST_MESSAGE),
                                is(getContext().getString(R.string.illegal_rest_url)));
                        return null;
                    }
                });
        new Uploader(getContext(), prefs);
        assertIntentSeen();
    }
}
