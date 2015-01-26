package com.nightscout.android.barcode;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.common.collect.Lists;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.nightscout.android.R;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.settings.SettingsActivity;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPreferenceManager;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Config(emulateSdk = 16)
@RunWith(RobolectricTestRunner.class)
public class AndroidBarcodeTest extends RobolectricTestBase {
    Activity activity;
    SharedPreferences sharedPrefs;
    String jsonConfig = null;
    NightscoutPreferences prefs;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SettingsActivity.class).create().get();
        sharedPrefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
        prefs = new AndroidPreferences(activity.getApplicationContext(), sharedPrefs);
    }

    private void setValidMongoOnlyWithIntentResult(){
        jsonConfig = "{'mongo':{'uri':'mongodb://user:pass@test.com/cgm_data'}}";
        fakeActivityResult();
    }

    private void setSingleValidApiOnlyWithIntentResult(){
        jsonConfig = "{'rest':{'endpoint':['http://abc@test.com/v1']}}";
        fakeActivityResult();
    }

    private void setSingleValidApiAndMongoWithIntentResult(){
        jsonConfig = "{'mongo':{'uri':'mongodb://user:pass@test.com/cgm_data'}, 'rest':{'endpoint':['http://abc@test.com/']}}";
        fakeActivityResult();
    }

    private void setMultipleValidApiOnlyWithIntentResult(){
        jsonConfig = "{'rest':{'endpoint':['http://abc@test.com/v1', 'http://test.com/']}}";
        fakeActivityResult();
    }

    private void setEmptyValidApiOnlyWithIntentResult(){
        jsonConfig = "{'rest':{'endpoint':[]}}";
        fakeActivityResult();
    }

    private void setEmptyValidMongoOnlyWithIntentResult(){
        jsonConfig = "{'mongo':{}}";
        fakeActivityResult();
    }

    private void setInvalidConfigWithValidJson(){
        jsonConfig = "{'some':{'random':['values']}}";
        fakeActivityResult();
    }

    private void setInvalidJsonWithIntentResult(){
        jsonConfig = "{foo bar";
        fakeActivityResult();
    }

    private void fakeActivityResult(){
        Intent intent = createFakeScanIntent(jsonConfig);
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class)
                .create()
                .start()
                .resume()
                .get();
        activity.onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent);
    }

    @Test
    public void mongoConfigShouldMongoEnablePrefsOnScanResult() throws Exception {
        setValidMongoOnlyWithIntentResult();
        assertThat(prefs.isMongoUploadEnabled(), is(true));
    }

    @Test
    public void mongoConfigShouldSetMongoUriPrefsOnScanResult() throws Exception {
        setValidMongoOnlyWithIntentResult();
        assertThat(prefs.getMongoClientUri(), is("mongodb://user:pass@test.com/cgm_data"));
    }

    @Test
    public void mongoConfigShouldNotEnableApiPrefsOnScanResult() throws Exception {
        setValidMongoOnlyWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(false));
    }

    @Test
    public void apiConfigShouldEnableApiPrefsOnScanResult() throws Exception{
        setSingleValidApiOnlyWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(true));
    }

    @Test
    public void apiConfigShouldSetApiPrefsOnScanResult() throws Exception{
        setSingleValidApiOnlyWithIntentResult();
        List<String> uris = Lists.newArrayList("http://abc@test.com/v1");
        assertThat(prefs.getRestApiBaseUris(), is(uris));
    }

    @Test
    public void apiConfigShouldNotSetMongoPrefsOnScanResult() throws Exception{
        setSingleValidApiOnlyWithIntentResult();
        assertThat(prefs.isMongoUploadEnabled(), is(false));
    }

    @Test
    public void multipleApiUriConfigShouldEnableApiPrefsOnScanResult() throws Exception {
        setMultipleValidApiOnlyWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(true));
    }

    @Test
    public void multipleApiUriConfigShouldNotEnableMongoPrefsOnScanResult() throws Exception {
        setMultipleValidApiOnlyWithIntentResult();
        assertThat(prefs.isMongoUploadEnabled(), is(false));
    }

    @Test
    public void multipleValidApiUriConfigShouldEnableApiUriPrefsOnScanResult() throws Exception {
        setMultipleValidApiOnlyWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(true));
    }

    @Test
    public void multipleValidApiUriConfigShouldSetApiUriPrefsOnScanResult() throws Exception {
        List<String> uris = Lists.newArrayList("http://abc@test.com/v1", "http://test.com/");
        setMultipleValidApiOnlyWithIntentResult();
        assertThat(prefs.getRestApiBaseUris(), is(uris));
    }

    @Test
    public void mongoAndApiConfigShouldEnableApiPrefsOnScanResult() throws Exception {
        setSingleValidApiAndMongoWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(true));
    }

    @Test
    public void mongoAndApiConfigShouldEnableMongoPrefsOnScanResult() throws Exception {
        setSingleValidApiAndMongoWithIntentResult();
        assertThat(prefs.isMongoUploadEnabled(), is(true));
    }

    @Test
    public void mongoAndApiConfigShouldSetMongoPrefsOnScanResult() throws Exception {
        setSingleValidApiAndMongoWithIntentResult();
        assertThat(prefs.getMongoClientUri(), is("mongodb://user:pass@test.com/cgm_data"));
    }

    @Test
    public void mongoAndApiConfigShouldSetApiPrefsOnScanResult() throws Exception {
        setSingleValidApiAndMongoWithIntentResult();
        List<String> uris = Lists.newArrayList("http://abc@test.com/");
        assertThat(prefs.getRestApiBaseUris(), is(uris));
    }


    @Test
    public void shouldStartScanActivity(){
        AndroidBarcode barcode = new AndroidBarcode(activity);
        barcode.scan();
        Intent intent = getShadowApplication().getNextStartedActivity();
        assertThat(intent.getComponent().getClassName(), is(CaptureActivity.class.getName()));
    }

    @Test
    public void validMongoOnlyShouldSetDefaultSgCollectionForOnlyMongoUriSet(){
        setValidMongoOnlyWithIntentResult();
        assertThat(prefs.getMongoCollection(), is(getShadowApplication().getApplicationContext().getString(R.string.pref_default_mongodb_collection)));
    }

    @Test
    public void validMongoOnlyShouldSetDefaultDeviceStatusCollectionForOnlyMongoUriSet(){
        setValidMongoOnlyWithIntentResult();
        assertThat(prefs.getMongoDeviceStatusCollection(), is(getShadowApplication().getApplicationContext()
                        .getString(R.string.pref_default_mongodb_device_status_collection)));
    }

    @Test
    public void invalidJsonShouldNotEnableMongo(){
        setInvalidJsonWithIntentResult();
        assertThat(prefs.isMongoUploadEnabled(), is(false));
    }

    @Test
    public void invalidJsonShouldNotEnableApi(){
        setInvalidJsonWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(false));
    }

    @Test
    public void setEmptyApiConfigShouldNotEnableApi(){
        setEmptyValidApiOnlyWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(false));
    }

    @Test
    public void setEmptyApiConfigShouldNotEnableMongo(){
        setEmptyValidApiOnlyWithIntentResult();
        assertThat(prefs.isMongoUploadEnabled(), is(false));
    }

    @Test
    public void setEmptyMongoConfigShouldNotEnableApi(){
        setEmptyValidMongoOnlyWithIntentResult();
        assertThat(prefs.isRestApiEnabled(), is(false));
    }

    @Test
    public void setEmptyMongoConfigShouldNotEnableMongo(){
        setEmptyValidMongoOnlyWithIntentResult();
        assertThat(prefs.isMongoUploadEnabled(), is(false));
    }

    @Test
    public void invalidConfigShouldNotEnableMongo(){
        setInvalidConfigWithValidJson();
        assertThat(prefs.isMongoUploadEnabled(), is(false));
    }

    @Test
    public void invalidConfigShouldNotEnableApi(){
        setInvalidConfigWithValidJson();
        assertThat(prefs.isRestApiEnabled(), is(false));
    }

    private Intent createFakeScanIntent(String jsonString){
        Intent intent = new Intent(AndroidBarcode.SCAN_INTENT);
        intent.putExtra("SCAN_RESULT", jsonString);
        intent.putExtra("SCAN_RESULT_FORMAT", "");
        intent.putExtra("SCAN_RESULT_BYTES", new byte[0]);
        intent.putExtra("SCAN_RESULT_ORIENTATION", Integer.MIN_VALUE);
        intent.putExtra("SCAN_RESULT_ERROR_CORRECTION_LEVEL", "");
        return intent;
    }
}