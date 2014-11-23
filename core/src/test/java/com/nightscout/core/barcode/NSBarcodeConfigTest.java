package com.nightscout.core.barcode;


import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class NSBarcodeConfigTest {

    @Test
    public void testValidMongoBarcode() throws Exception {
        String mongoUri="mongo://test.com/cgm_data";
        String configString= "{\""+ NSBarcodeConfigKeys.MONGO_CONFIG+"\":{"+ NSBarcodeConfigKeys.MONGO_URI+":\""+mongoUri+"\"}}";
        NSBarcodeConfig barcode = new NSBarcodeConfig(configString);
        assertThat(barcode.getMongoUri(),is(mongoUri));
    }

    @Test
    public void testValidLegacyApiBarcode() throws Exception {
        String legacyApiUri="http://test.com/";
        String configString= "{\""+ NSBarcodeConfigKeys.API_CONFIG+"\":[{"+ NSBarcodeConfigKeys.API_URI+":\""+legacyApiUri+"\"}]}";
        NSBarcodeConfig barcode = new NSBarcodeConfig(configString);
        assertThat(barcode.getApiUris().get(0),is(legacyApiUri));
    }

    @Test
    public void testValidApiV1Barcode() throws Exception {
        String apiUri="http://abc@test.com/v1";
        String configString= "{\""+ NSBarcodeConfigKeys.API_CONFIG+"\":[{"+ NSBarcodeConfigKeys.API_URI+":\""+apiUri+"\"}]}";
        NSBarcodeConfig barcode = new NSBarcodeConfig(configString);
        assertThat(barcode.getApiUris().get(0),is(apiUri));
    }

    @Test
    public void testMultipleRest() throws Exception {
        List<String> uris=Lists.newArrayList();
        uris.add("http://abc@test.com/v1");
        uris.add("http://test.com/");
        String configString= "{\""+ NSBarcodeConfigKeys.API_CONFIG+"\":[{\""+ NSBarcodeConfigKeys.API_URI+"\":\""+uris.get(0)+"\"},{\""+ NSBarcodeConfigKeys.API_URI+"\":\""+uris.get(1)+"\"}]}";
        NSBarcodeConfig barcode = new NSBarcodeConfig(configString);
        assertThat(barcode.getApiUris().size(),is(2));
        assertThat(barcode.getApiUris(), is(uris));
    }

    @Test
    public void testMongoAndRest() throws Exception {
        String mongoUri="mongo://test.com/cgm_data";
        String apiUri="http://abc@test.com/";
        String configString= "{\""+ NSBarcodeConfigKeys.MONGO_CONFIG+"\":{\""+
                NSBarcodeConfigKeys.MONGO_URI+"\":\""+mongoUri+"\"},\""+ NSBarcodeConfigKeys.API_CONFIG+
                "\":[{\""+ NSBarcodeConfigKeys.API_URI+"\":\""+apiUri+"\"}]}";
        NSBarcodeConfig barcode = new NSBarcodeConfig(configString);
        assertThat(barcode.getMongoUri(),is(mongoUri));
        assertThat(barcode.getApiUris().get(0),is(apiUri));
    }

    // TODO(@ktind): add tests for URI validation
}
