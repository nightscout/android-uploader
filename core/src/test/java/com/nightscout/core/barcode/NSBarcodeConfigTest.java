package com.nightscout.core.barcode;


import com.google.common.collect.Lists;
import com.nightscout.core.preferences.TestPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class NSBarcodeConfigTest {
    TestPreferences prefs;

    @Before
    public void setUp() {
        prefs = new TestPreferences();
    }

    @Test
    public void testMongoSet() throws Exception {
        String mongoUri = "mongodb://user:pass@test.com/cgm_data";
        String mongoCollection = "cgm_data";
        String deviceStatusCollection = "devicestatus";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.MONGO_URI, mongoUri);
        child.put(NSBarcodeConfigKeys.MONGO_COLLECTION, mongoCollection);
        child.put(NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION, deviceStatusCollection);
        json.put(NSBarcodeConfigKeys.MONGO_CONFIG, child);
        NSBarcodeConfig barcode = new NSBarcodeConfig(json.toString(), prefs);
        String mongo = barcode.getMongoUri().get();
        assertThat(mongo, is(mongoUri));
        assertEquals(barcode.getMongoCollection(), mongoCollection);
        assertEquals(barcode.getMongoDeviceStatusCollection(), deviceStatusCollection);
    }

    @Test
    public void testValidLegacyApiBarcode() throws Exception {
        String legacyApiUri="https://test.com/";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.API_URI, legacyApiUri);
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(0, child);
        json.put(NSBarcodeConfigKeys.API_CONFIG, jsonArray);
        NSBarcodeConfig barcode = new NSBarcodeConfig(json.toString(), prefs);
        assertThat(barcode.getApiUris().size(), is(1));
        assertThat(barcode.getApiUris().get(0), is(legacyApiUri));
    }

    // This should be different as it will have its own validators
    @Test
    public void testValidApiV1Barcode() throws Exception {
        String apiUri="http://abc@test.com/v1";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.API_URI, apiUri);
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(0, child);
        json.put(NSBarcodeConfigKeys.API_CONFIG, jsonArray);
        NSBarcodeConfig barcode = new NSBarcodeConfig(json.toString(), prefs);
        assertThat(barcode.getApiUris().size(), is(1));
        assertThat(barcode.getApiUris().get(0), is(apiUri));
    }

    @Test
    public void testMultipleRest() throws Exception {
        List<String> uris=Lists.newArrayList();
        uris.add("http://abc@test.com/v1");
        uris.add("http://test.com/");
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        child.put(NSBarcodeConfigKeys.API_URI, uris.get(0));
        jsonArray.put(0,child);
        child = new JSONObject();
        child.put(NSBarcodeConfigKeys.API_URI, uris.get(1));
        jsonArray.put(1, child);
        json.put(NSBarcodeConfigKeys.API_CONFIG, jsonArray);
        NSBarcodeConfig barcode = new NSBarcodeConfig(json.toString(), prefs);
        assertThat(barcode.getApiUris().size(), is(2));
        assertThat(barcode.getApiUris(), is(uris));
    }

    @Test
    public void testMongoAndRestV1() throws Exception {
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
        jsonArray.put(0,child);
        json.put(NSBarcodeConfigKeys.API_CONFIG, jsonArray);
        json.put(NSBarcodeConfigKeys.MONGO_CONFIG, child);
        NSBarcodeConfig barcode = new NSBarcodeConfig(json.toString(), prefs);
        assertThat(barcode.getMongoUri().get(), is(mongoUri));
        assertThat(barcode.getApiUris().get(0), is(apiUri));
    }

    @Test
    public void testNoMongoSet() throws Exception {
        String apiUri="http://abc@test.com/v1";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.API_URI,apiUri);
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(0,child);
        json.put(NSBarcodeConfigKeys.API_CONFIG,jsonArray);
        NSBarcodeConfig barcode = new NSBarcodeConfig(json.toString(), prefs);
        assertThat(barcode.getMongoUri().isPresent(), is(false));
        assertThat(barcode.getMongoCollection(), nullValue());
        assertThat(barcode.getMongoDeviceStatusCollection(), nullValue());
    }

    @Test
    public void testNoApiSet() throws Exception {
        String mongoUri = "mongodb://user:pass@test.com/cgm_data";
        String mongoCollection = "cgm_data";
        String deviceStatusCollection = "devicestatus";
        JSONObject json = new JSONObject();
        JSONObject child = new JSONObject();
        child.put(NSBarcodeConfigKeys.MONGO_URI, mongoUri);
        child.put(NSBarcodeConfigKeys.MONGO_COLLECTION, mongoCollection);
        child.put(NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION, deviceStatusCollection);
        json.put(NSBarcodeConfigKeys.MONGO_CONFIG, child);
        NSBarcodeConfig barcode = new NSBarcodeConfig(json.toString(), prefs);
        assertThat(barcode.getApiUris(), is(empty()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidJsonConfig() throws Exception {
        String foo = "bar";
        new NSBarcodeConfig(foo, prefs);
    }

    @Test
    public void testNoApiUrisReturnsEmptyList() throws Exception {
        String mongoUri = "mongodb://user:pass@test.com/cgm_data";
        String configString = "{\"" + NSBarcodeConfigKeys.MONGO_CONFIG + "\":{"+ NSBarcodeConfigKeys.MONGO_URI + ":\"" + mongoUri + "\"}}";
        NSBarcodeConfig barcode = new NSBarcodeConfig(configString, prefs);
        assertThat(barcode.getApiUris(), is(empty()));
    }

    @Test
    public void testMongoConfigWithNoCollectionReturnsDefaults(){
        String jsonConfig = "{\"mongo_settings\":{\"mongo_uri\":\"mongodb://user:pass@test.com/cgm_data\"}}";
        NSBarcodeConfig barcode = new NSBarcodeConfig(jsonConfig, prefs);
        assertThat(barcode.getMongoCollection(), is(TestPreferences.DEFAULT_MONGO_COLLECTION));
        assertThat(barcode.getMongoDeviceStatusCollection(), is(TestPreferences.DEFAULT_MONGO_DEVICE_STATUS_COLLECTION));
    }

    @After
    public void tearDown(){
        prefs = null;
    }
}