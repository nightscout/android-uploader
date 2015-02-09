package com.nightscout.android.upload;

import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UploaderTest extends RobolectricTestBase {

    @Test
    public void initializeShouldFailOnInvalidMongoUri() throws Exception {
        NightscoutPreferences prefs = new AndroidPreferences(getContext());
        prefs.setMongoUploadEnabled(true);
        prefs.setMongoClientUri("http://test.com");
        Uploader uploader = new Uploader(getContext());
        assertThat(uploader.areAllUploadersInitialized(), is(false));
    }
}
