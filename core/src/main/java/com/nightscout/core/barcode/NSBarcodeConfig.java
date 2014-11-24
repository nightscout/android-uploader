package com.nightscout.core.barcode;

import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A class to manage barcode configuration of the uploader
 */
public class NSBarcodeConfig {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private JSONObject config;
    private final String TAG = NSBarcodeConfig.class.getSimpleName();

    public NSBarcodeConfig(String decodeResults) {
        configureBarcode(decodeResults);
    }

    public NSBarcodeConfig(){
        this.config=new JSONObject();
    }

    public void configureBarcode(String jsonConfig){
        try {
            this.config = new JSONObject(jsonConfig);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid configuration from barcode: "+jsonConfig);
        }
    }

    public String getMongoUri() {
        String mongoUri = null;
        try {
            if (config.has(NSBarcodeConfigKeys.MONGO_CONFIG) &&
                    config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).has(NSBarcodeConfigKeys.MONGO_URI)) {
                mongoUri = config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).getString(NSBarcodeConfigKeys.MONGO_URI);
            }
        } catch (JSONException e) {
            return null;
        }
        return mongoUri;
    }

    public List<String> getApiUris() {
        List<String> apiUris = Lists.newArrayList();
        if (config.has(NSBarcodeConfigKeys.API_CONFIG)) {
            JSONArray childNode = null;
            try {
                childNode = config.getJSONArray(NSBarcodeConfigKeys.API_CONFIG);
            } catch (JSONException e) {
                log.error(TAG, "Invalid json array: "+ config.toString());
                return apiUris;
            }
            for (int index = 0; index < childNode.length(); index++) {
                try {
                    apiUris.add(new JSONObject(childNode.getString(index)).getString(NSBarcodeConfigKeys.API_URI));
                } catch (JSONException e) {
                    log.error(TAG, "Invalid child json object: "+ config.toString());
                }
            }
        }
        return apiUris;
    }

    public String getMongoCollection(){
        String mongoCollection = null;
        try {
            if (config.has(NSBarcodeConfigKeys.MONGO_CONFIG) &&
                    config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).has(NSBarcodeConfigKeys.MONGO_COLLECTION)) {
                mongoCollection = config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).getString(NSBarcodeConfigKeys.MONGO_COLLECTION);
            }
        } catch (JSONException e) {
            return null;
        }
        return mongoCollection;
    }

    public String getMongoDeviceStatusCollection(){
        String deviceStatusCollection = null;
        try {
            if (config.has(NSBarcodeConfigKeys.MONGO_CONFIG) &&
                    config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).has(NSBarcodeConfigKeys.MONGO_COLLECTION)) {
                deviceStatusCollection = config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).getString(NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION);
            }
        } catch (JSONException e) {
            return null;
        }
        return deviceStatusCollection;
    }

    public boolean hasMongoUri(){
        return config.has(NSBarcodeConfigKeys.MONGO_CONFIG);
    }

    public boolean hasApiUri(){
        return config.has(NSBarcodeConfigKeys.API_CONFIG);
    }
}
