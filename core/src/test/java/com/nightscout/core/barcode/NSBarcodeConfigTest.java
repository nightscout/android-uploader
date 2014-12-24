package com.nightscout.core.barcode;


import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NSBarcodeConfigTest {
    String jsonConfig;
    NSBarcodeConfig barcode;

    @Before
    public void setUp() {
        jsonConfig = "";
        barcode = null;
    }

    private void setBarcode() {
        barcode = new NSBarcodeConfig(jsonConfig);
    }

    private void setValidMongoOnlyNoCollections() {
        jsonConfig = "{'mongo':{'uri':'mongodb://user:pass@test.com/cgm_data'}}";
        setBarcode();
    }

    private void setSingleValidApiOnly() {
        jsonConfig = "{'rest':{'endpoint':['http://abc@test.com/v1']}}";
        setBarcode();
    }

    private void setSingleValidApiAndMongo() {
        jsonConfig = "{'mongo':{'uri':'mongodb://user:pass@test.com/cgm_data'}, 'rest':{'endpoint':['http://abc@test.com/v1']}}";
        setBarcode();
    }

    private void setMultipleValidApiOnly() {
        jsonConfig = "{'rest':{'endpoint':['http://abc@test.com/v1', 'http://test.com/']}}";
        setBarcode();
    }

    private void setEmptyValidApiOnly() {
        jsonConfig = "{'rest':{'endpoint':[]}}";
        setBarcode();
    }

    private void setEmptyValidMongoOnly() {
        jsonConfig = "{'mongo':{}";
        setBarcode();
    }

    private void setInvalidConfigWithValidJson() {
        jsonConfig = "{'some':{'random':['values']}}";
        setBarcode();
    }

    private void setInvalidJson() {
        jsonConfig = "{foo bar";
        setBarcode();
    }

    private void verifySingleApiUri() {
        List<String> uris = Lists.newArrayList("http://abc@test.com/v1");
        assertThat(barcode.getApiUris(), is(uris));
    }

    private void verifyMultipleApiUri() {
        List<String> uris = Lists.newArrayList("http://abc@test.com/v1", "http://test.com/");
        assertThat(barcode.getApiUris(), is(uris));
    }

    private void verifyMongoUri() {
        assertThat(barcode.getMongoUri().get(), is("mongodb://user:pass@test.com/cgm_data"));
    }

    @Test
    public void testMongoEnabledWithMongoConfig() {
        setValidMongoOnlyNoCollections();
        assertThat(barcode.hasMongoConfig(), is(true));
    }

    @Test
    public void testApiNotEnabledWithMongoConfig() {
        setValidMongoOnlyNoCollections();
        assertThat(barcode.hasApiConfig(), is(false));
    }

    @Test
    public void testMongoUriSetWithMongoConfig() {
        setValidMongoOnlyNoCollections();
        verifyMongoUri();
    }

    @Test
    public void testApiEnabledWithApiConfig() {
        setSingleValidApiOnly();
        assertThat(barcode.hasApiConfig(), is(true));
    }

    @Test
    public void testMongoIsNotEnabledWithApiConfig() {
        setSingleValidApiOnly();
        assertThat(barcode.hasMongoConfig(), is(false));
    }

    @Test
    public void testApiUriIsSetWithApiConfig() {
        setSingleValidApiOnly();
        verifySingleApiUri();
    }

    @Test
    public void testSingleApiUriAndMongoEnablesMongoWithApiAndMongoConfig() {
        setSingleValidApiAndMongo();
        assertThat(barcode.hasMongoConfig(), is(true));
    }

    @Test
    public void testSingleApiUriAndMongoEnablesApiWithApiAndMongoConfig() {
        setSingleValidApiAndMongo();
        assertThat(barcode.hasApiConfig(), is(true));
    }

    @Test
    public void testSingleApiUriAndMongoSetsApiWithApiAndMongoConfig() {
        setSingleValidApiAndMongo();
        verifySingleApiUri();
    }

    @Test
    public void testSingleApiUriAndMongoSetsMongoWithApiAndMongoConfig() {
        setSingleValidApiAndMongo();
        verifyMongoUri();
    }

    @Test
    public void testMultipleValidApiUriEnablesApiWithApiConfig() {
        setMultipleValidApiOnly();
        assertThat(barcode.hasApiConfig(), is(true));
    }

    @Test
    public void testMultipleValidApiUriDoesNotEnableMongoWithApiConfig() {
        setMultipleValidApiOnly();
        assertThat(barcode.hasMongoConfig(), is(false));
    }

    @Test
    public void testMultipleValidApiUriSetsApiWithApiConfig() {
        setMultipleValidApiOnly();
        verifyMultipleApiUri();
    }

    @Test
    public void testEmptyValidApiDoesNotEnableApiWithApiConfig() {
        setEmptyValidApiOnly();
        assertThat(barcode.hasApiConfig(), is(false));
    }

    @Test
    public void testEmptyValidApiDoesNotEnableMongoWithApiConfig() {
        setEmptyValidApiOnly();
        assertThat(barcode.hasMongoConfig(), is(false));
    }

    @Test
    public void testEmptyValidMongoDoesNotEnableApiWithApiConfig() {
        setEmptyValidMongoOnly();
        assertThat(barcode.hasApiConfig(), is(false));
    }

    @Test
    public void testEmptyValidMongoDoesNotEnableMongoWithApiConfig() {
        setEmptyValidMongoOnly();
        assertThat(barcode.hasMongoConfig(), is(false));
    }

    @Test
    public void testInvalidConfigWithValidJsonDoesNotEnableMongo() {
        setInvalidConfigWithValidJson();
        assertThat(barcode.hasMongoConfig(), is(false));
    }

    @Test
    public void testInvalidConfigWithValidJsonDoesNotEnableApi() {
        setInvalidConfigWithValidJson();
        assertThat(barcode.hasApiConfig(), is(false));
    }

    @Test
    public void testInvalidJsonDoesNotEnableMongo() {
        setInvalidJson();
        assertThat(barcode.hasMongoConfig(), is(false));
    }

    @Test
    public void testInvalidJsonDoesNotEnableApi() {
        setInvalidJson();
        assertThat(barcode.hasApiConfig(), is(false));
    }
}