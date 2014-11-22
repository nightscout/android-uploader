package com.nightscout.android.barcode;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;

import com.google.common.collect.Lists;
import com.google.zxing.integration.android.IntentIntegrator;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.preferences.PreferenceKeys;
import com.nightscout.android.settings.SettingsActivity;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.barcode.NSBarcodeConfig;
import com.nightscout.core.barcode.NSBarcodeConfigKeys;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPreferenceManager;
import org.robolectric.util.FragmentTestUtil;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Config(emulateSdk = 16)
@RunWith(RobolectricTestRunner.class)
public class AndroidBarcodeTest extends RobolectricTestBase {
    Activity activity;
    SharedPreferences sharedPrefs;
    private SettingsActivity.MainPreferenceFragment mainPreferenceFragment;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SettingsActivity.class).create().get();
        sharedPrefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
        mainPreferenceFragment = new SettingsActivity.MainPreferenceFragment();
        FragmentTestUtil.startFragment(mainPreferenceFragment, SettingsActivity.class);
    }

    @Test
    public void shouldSetMongoPrefsOnScanResult() throws Exception {
        String mongoUri = "mongodb://user:pass@test.com/cgm_data";
        String mongoCollection = "cgm_data";
        String deviceStatusCollection = "devicestatus";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.MONGO_URI, mongoUri);
        child.put(NSBarcodeConfigKeys.MONGO_COLLECTION, mongoCollection);
        child.put(NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION, deviceStatusCollection);
        json.put(NSBarcodeConfigKeys.MONGO_CONFIG,child);
        System.out.println(json.toString());
        Intent intent = createFakeScanIntent(json.toString());
        mainPreferenceFragment.onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED, false), is(true));
        assertThat(sharedPrefs.getString(PreferenceKeys.MONGO_URI, null), is(mongoUri));
        assertThat(sharedPrefs.getString(PreferenceKeys.MONGO_COLLECTION, null), is(mongoCollection));
        assertThat(sharedPrefs.getString(PreferenceKeys.MONGO_DEVICE_STATUS_COLLECTION, null), is(deviceStatusCollection));

        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED, true), is(false));
    }

    @Test
    public void shouldSetApiPrefsOnScanResult() throws Exception{
        String apiUri="http://abc@test.com/v1";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.API_URI,apiUri);
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(0,child);
        json.put(NSBarcodeConfigKeys.API_CONFIG,jsonArray);

        Intent intent = createFakeScanIntent(json.toString());
        mainPreferenceFragment.onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED,false),is(true));
        assertThat(sharedPrefs.getString(PreferenceKeys.API_URIS,null),is(apiUri));

        // Check to make sure that the mongo uploader was not enabled
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,true),is(false));
    }

    @Test
    public void shouldSetMultipleApiPrefsOnScanResult() throws Exception {
        List<String> uris=Lists.newArrayList();
        uris.add("http://abc@test.com/v1");
        uris.add("http://test.com/");
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        child.put(NSBarcodeConfigKeys.API_URI,uris.get(0));
        jsonArray.put(0,child);
        child = new JSONObject();
        child.put(NSBarcodeConfigKeys.API_URI,uris.get(1));
        jsonArray.put(1,child);
        json.put(NSBarcodeConfigKeys.API_CONFIG,jsonArray);

        Intent intent = createFakeScanIntent(json.toString());
        mainPreferenceFragment.onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED,false),is(true));
        // May not be good - lists don't have a guaranteed order?
        assertThat(Lists.newArrayList(sharedPrefs.getString(PreferenceKeys.API_URIS,null).split(" ")),is(uris));

        // Check to make sure that the mongo uploader was not enabled
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,true),is(false));
    }

    @Test
    public void shouldSetMongoAndApiPrefsOnScanResult() throws Exception {
        String apiUri="http://abc@test.com/";
        String mongoUri = "mongodb://user:pass@test.com/cgm_data";
        String mongoCollection = "cgm_data";
        String deviceStatusCollection = "devicestatus";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.MONGO_URI, mongoUri);
        child.put(NSBarcodeConfigKeys.MONGO_COLLECTION, mongoCollection);
        child.put(NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION, deviceStatusCollection);
        json.put(NSBarcodeConfigKeys.MONGO_CONFIG, child);
        child.put(NSBarcodeConfigKeys.API_URI, apiUri);
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(0, child);
        json.put(NSBarcodeConfigKeys.API_CONFIG, jsonArray);
        json.put(NSBarcodeConfigKeys.MONGO_CONFIG, child);

        Intent intent = createFakeScanIntent(json.toString());
        mainPreferenceFragment.onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent);
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.API_UPLOADER_ENABLED,false), is(true));
        assertThat(sharedPrefs.getString(PreferenceKeys.API_URIS,null), is(apiUri));

        // Check to make sure that the mongo uploader was not enabled
        assertThat(sharedPrefs.getBoolean(PreferenceKeys.MONGO_UPLOADER_ENABLED,false),is(true));
    }

    @Test
    public void shouldStartScanActivity(){
        AndroidBarcode barcode = new AndroidBarcode(activity);
        barcode.scan();
        Intent intent = getShadowApplication().getNextStartedActivity();
        assertThat(intent.getComponent().getClassName(), is(com.google.zxing.client.android.CaptureActivity.class.getName()));
    }

    @Test
    public void shouldSetDefaultCollectionsForOnlyMongoUriSet(){
        String jsonConfig = "{\""+NSBarcodeConfigKeys.MONGO_CONFIG+"\":{\""+NSBarcodeConfigKeys.MONGO_URI+"\":\"mongodb://user:pass@test.com/cgm_data\"}}";
        AndroidPreferences androidPreferences = new AndroidPreferences(sharedPrefs);
        NSBarcodeConfig barcode = new NSBarcodeConfig(jsonConfig, androidPreferences);
        assertThat(barcode.getMongoCollection().isPresent(),is(true));
        assertThat(barcode.getMongoDeviceStatusCollection().isPresent(),is(true));
        assertThat(barcode.getMongoCollection().get(), is(androidPreferences.getDefaultMongoCollection()));
        assertThat(barcode.getMongoDeviceStatusCollection().get(), is(androidPreferences.getDefaultMongoDeviceStatusCollection()));
    }

    private Intent createFakeScanIntent(String jsonString){
        Intent intent = new Intent(AndroidBarcode.SCAN_INTENT);
        intent.putExtra("SCAN_RESULT",jsonString);
        intent.putExtra("SCAN_RESULT_FORMAT", "");
        intent.putExtra("SCAN_RESULT_BYTES", new byte[0]);
        intent.putExtra("SCAN_RESULT_ORIENTATION", Integer.MIN_VALUE);
        intent.putExtra("SCAN_RESULT_ERROR_CORRECTION_LEVEL", "");
        return intent;
    }

    @After
    public void tearDown(){
        sharedPrefs = null;
    }
}