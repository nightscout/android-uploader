package com.nightscout.android.upload;

import android.content.Intent;

import com.nightscout.android.R;
import com.nightscout.android.ToastReceiver;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.preferences.TestPreferences;

import net.tribe7.common.base.Function;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
    public void shouldSendToastIntentOnInvalidRestv1Uri() throws Exception {
        TestPreferences prefs = new TestPreferences();
        prefs.setRestApiEnabled(true);
        List<String> invalidUri = new ArrayList<>(Arrays.asList(new String[]{"http://test/v1"}));
        prefs.setRestApiBaseUris(invalidUri);

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
        List<String> invalidUri = new ArrayList<>(Arrays.asList(new String[]{"\\invalid"}));
        prefs.setRestApiBaseUris(invalidUri);

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
