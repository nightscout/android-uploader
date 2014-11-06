package com.nightscout.android.upload;

import com.mongodb.DB;
import com.nightscout.android.preferences.PreferenceKeys;
import com.nightscout.android.test.BaseTest;
import com.nightscout.core.upload.AbstractRestUploader;
import com.nightscout.core.upload.MongoUploader;
import com.nightscout.core.upload.RestLegacyUploader;
import com.nightscout.core.upload.RestV1Uploader;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class UploaderTest extends BaseTest {
    private Uploader uploader;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInitialize_NoPrefs() {
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Is.is(Matchers.empty()));
    }

    public void testInitialize_MongoNoUri() {
        getPreferences().edit().putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, true).commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Is.is(Matchers.empty()));
    }

    public void testInitialize_MongoInvalidUri() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.MONGO_URI, "http://test.com")
                .commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Is.is(Matchers.empty()));
    }

    public void testInitialize_MongoValidUri() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.MONGO_URI, "mongodb://test.com")
                .commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Matchers.hasSize(1));
    }

    public void testInitialize_MongoValidCollection() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.MONGO_URI, "mongodb://test.com/db")
                .putString(PreferenceKeys.MONGO_COLLECTION, "collection")
                .commit();
        uploader = new Uploader(getContext());
        MongoUploader mongoUploader = (MongoUploader) uploader.getUploaders().get(0);
        mongoUploader.setDB(Mockito.mock(DB.class));
        ArgumentCaptor<String> collectionName = ArgumentCaptor.forClass(String.class);
        Mockito.when(mongoUploader.getDB().getCollection(collectionName.capture())).thenReturn(null);
        mongoUploader.getCollection();
        MatcherAssert.assertThat(collectionName.getValue(), is("collection"));
    }

    public void testInitialize_MongoValidDeviceStatusCollection() throws Exception {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.MONGO_URI, "mongodb://test.com/db")
                .putString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION, "dscollection")
                .commit();
        uploader = new Uploader(getContext());
        MongoUploader mongoUploader = (MongoUploader) uploader.getUploaders().get(0);
        mongoUploader.setDB(Mockito.mock(DB.class));
        ArgumentCaptor<String> collectionName = ArgumentCaptor.forClass(String.class);
        Mockito.when(mongoUploader.getDB().getCollection(collectionName.capture())).thenReturn(null);
        mongoUploader.getDeviceStatusCollection();
        MatcherAssert.assertThat(collectionName.getValue(), is("dscollection"));
    }

    public void testInitialize_Api() {
        getPreferences().edit().putBoolean(PreferenceKeys.API_UPLOADER_ENABLED, true).commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Is.is(Matchers.empty()));
    }

    public void testInitialize_ApiLegacy() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.API_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.API_URIS, "http://test.com/legacy")
                .commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Matchers.hasSize(1));
        AbstractRestUploader restUploader = (AbstractRestUploader) uploader.getUploaders().get(0);
        assertThat(restUploader, Is.is(Matchers.instanceOf(RestLegacyUploader.class)));
        assertThat(restUploader.getUri().toString(), is("http://test.com/legacy"));
    }

    public void testInitialize_Apiv1NoToken() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.API_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.API_URIS, "http://test.com/v1")
                .commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Matchers.hasSize(0));
    }

    public void testInitialize_Apiv1() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.API_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.API_URIS, "http://123@test.com/v1")
                .commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Matchers.hasSize(1));
        AbstractRestUploader restUploader = (AbstractRestUploader) uploader.getUploaders().get(0);
        assertThat(restUploader, Is.is(Matchers.instanceOf(RestV1Uploader.class)));
        assertThat(restUploader.getUri().toString(), is("http://test.com/v1"));
    }

    public void testInitialize_RestMultiple() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.API_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.API_URIS, "http://123@test.com/v1 http://test.com/legacy")
                .commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Matchers.hasSize(2));
        assertThat(uploader.getUploaders(), Matchers.containsInAnyOrder(
                Matchers.instanceOf(RestV1Uploader.class), Matchers.instanceOf(RestLegacyUploader.class)));
    }

    public void testInitialize_RestAndMongo() {
        getPreferences().edit()
                .putBoolean(PreferenceKeys.API_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.API_URIS, "http://123@test.com/v1")
                .putBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, true)
                .putString(PreferenceKeys.MONGO_URI, "mongodb://test.com/db")
                .putString(PreferenceKeys.MONGO_COLLECTION, "collection")
                .commit();
        uploader = new Uploader(getContext());
        assertThat(uploader.getUploaders(), Matchers.hasSize(2));
        assertThat(uploader.getUploaders(), Matchers.containsInAnyOrder(
                Matchers.instanceOf(RestV1Uploader.class), Matchers.instanceOf(MongoUploader.class)));
    }
}
