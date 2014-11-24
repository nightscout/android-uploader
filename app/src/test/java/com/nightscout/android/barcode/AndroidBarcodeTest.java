package com.nightscout.android.barcode;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.common.collect.Lists;
import com.google.zxing.integration.android.IntentIntegrator;
import com.nightscout.android.preferences.PreferenceKeys;
import com.nightscout.android.settings.SettingsActivity;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.barcode.NSBarcodeConfigKeys;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowPreferenceManager;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AndroidBarcodeTest extends RobolectricTestBase {
    Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SettingsActivity.class).create().get();
    }

    @Test
    public void shouldSetMongoPrefsOnScanResult(){
        SharedPreferences sharedPrefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
        String mongoUri = "mongodb://test.com";
        String mongoCollection = "cgm_data";
        String mongoDeviceStatusCollection = "devicestatus";
        String jsonString = "{\""+ NSBarcodeConfigKeys.MONGO_CONFIG+"\":{\""+NSBarcodeConfigKeys.MONGO_URI+"\":\""+mongoUri+"\",\""+NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION+"\":\""+mongoDeviceStatusCollection+"\",\""+NSBarcodeConfigKeys.MONGO_COLLECTION+"\":\""+mongoCollection+"\"}}";
        Intent intent = createFakeScanIntent(jsonString);
        new SettingsActivity().onActivityResult(IntentIntegrator.REQUEST_CODE,Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,false),is(true));
        assertThat(sharedPrefs.getString(PreferenceKeys.MONGO_URI,null),is(mongoUri));
        assertThat(sharedPrefs.getString(PreferenceKeys.MONGO_COLLECTION,null),is(mongoCollection));
        assertThat(sharedPrefs.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION,null),is(mongoDeviceStatusCollection));

        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED,true),is(false));
    }

    @Test
    public void shouldSetApiPrefsOnScanResult(){
        SharedPreferences sharedPrefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
        String apiUri = "http://test.com/v1";

        String jsonString = "{\""+ NSBarcodeConfigKeys.API_CONFIG+"\":[{\""+NSBarcodeConfigKeys.API_URI+"\":\""+apiUri+"\"}]}";
        Intent intent = createFakeScanIntent(jsonString);
        new SettingsActivity().onActivityResult(IntentIntegrator.REQUEST_CODE,Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED,false),is(true));
        assertThat(sharedPrefs.getString(PreferenceKeys.API_URIS,null),is(apiUri));

        // Check to make sure that the mongo uploader was not enabled
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,true),is(false));
    }

    @Test
    public void shouldSetMultipleApiPrefsOnScanResult(){
        SharedPreferences sharedPrefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
        String apiUri = "http://test.com/v1 http://test.com";
        List<String> apiUris = Lists.newArrayList(apiUri.split(" "));

        String jsonString = "{\""+ NSBarcodeConfigKeys.API_CONFIG+"\":[{\""+NSBarcodeConfigKeys.API_URI+"\":\""+apiUris.get(0)+"\"},{\""+NSBarcodeConfigKeys.API_URI+"\":\""+apiUris.get(1)+"\"}]}";
        Intent intent = createFakeScanIntent(jsonString);
        new SettingsActivity().onActivityResult(IntentIntegrator.REQUEST_CODE,Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED,false),is(true));
        // May not be good - lists don't have a guaranteed order?
        assertThat(sharedPrefs.getString(PreferenceKeys.API_URIS,null),is(apiUri));

        // Check to make sure that the mongo uploader was not enabled
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,true),is(false));
    }

    @Test
    public void shouldSetMongoAndApiPrefsOnScanResult(){
        SharedPreferences sharedPrefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
        String apiUri = "http://test.com/v1";
        String mongoUri = "mongodb://test.com";
        String mongoCollection = "cgm_data";
        String mongoDeviceStatusCollection = "devicestatus";


        String jsonString = "{\""+NSBarcodeConfigKeys.MONGO_CONFIG+"\":{\""+NSBarcodeConfigKeys.MONGO_URI+"\":\""+mongoUri+"\",\""+NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION+"\":\""+mongoDeviceStatusCollection+"\",\""+NSBarcodeConfigKeys.MONGO_COLLECTION+"\":\""+mongoCollection+"\"},\""+ NSBarcodeConfigKeys.API_CONFIG+"\":[{\""+NSBarcodeConfigKeys.API_URI+"\":\""+apiUri+"\"}]}";

        Intent intent = createFakeScanIntent(jsonString);
        new SettingsActivity().onActivityResult(IntentIntegrator.REQUEST_CODE,Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED,false),is(true));
        assertThat(sharedPrefs.getString(PreferenceKeys.API_URIS,null),is(apiUri));

        // Check to make sure that the mongo uploader was not enabled
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,false),is(true));
    }

    @Test
    public void shouldStartScanActivity(){
        AndroidBarcode barcode = new AndroidBarcode(activity);
        barcode.scan();
        Intent intent = getShadowApplication().getNextStartedActivity();
        assertThat(intent.getComponent().getClassName(),is(com.google.zxing.client.android.CaptureActivity.class.getName()));
    }

    private Intent createFakeScanIntent(String jsonString){
        Intent intent = new Intent(AndroidBarcode.SCAN_INTENT);
        intent.putExtra("SCAN_RESULT",jsonString);
        intent.putExtra("SCAN_RESULT_FORMAT","");
        intent.putExtra("SCAN_RESULT_BYTES",new byte[0]);
        intent.putExtra("SCAN_RESULT_ORIENTATION",Integer.MIN_VALUE);
        intent.putExtra("SCAN_RESULT_ERROR_CORRECTION_LEVEL","");
        return intent;
    }
}
