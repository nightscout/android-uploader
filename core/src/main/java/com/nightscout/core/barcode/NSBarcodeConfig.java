package com.nightscout.core.barcode;

import com.google.common.base.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to manage barcode configuration of the uploader
 */
public class NSBarcodeConfig {
    protected static final Logger log = LoggerFactory.getLogger(NSBarcodeConfig.class);
    private JSONObject config = new JSONObject();

    public NSBarcodeConfig(String decodeResults) {
        configureBarcode(decodeResults);
    }

    public void configureBarcode(String jsonConfig){
        if (jsonConfig == null){
            throw new IllegalArgumentException("Null barcode");
        }
        try {
            this.config = new JSONObject(jsonConfig);
        } catch (JSONException e) {
            return;
        }
    }

    public Optional<String> getMongoUri() {
        String mongoUri = null;
        try {
            if (hasMongoConfig()) {
                mongoUri = config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).getString(NSBarcodeConfigKeys.MONGO_URI);
            } else {
                return Optional.absent();
            }
        } catch (JSONException e) {
            return Optional.absent();
        }
        return Optional.of(mongoUri);
    }

    public List<String> getApiUris() {
        List<String> apiUris = new ArrayList<>();
        if (hasApiConfig()){
            JSONArray jsonArray = null;
            try {
                jsonArray = config.getJSONObject(NSBarcodeConfigKeys.API_CONFIG)
                        .getJSONArray(NSBarcodeConfigKeys.API_URI);
            } catch (JSONException e) {
                log.error("Invalid json array: " + config.toString());
                return apiUris;
            }
            for (int index = 0; index < jsonArray.length(); index++) {
                try {
                    apiUris.add(jsonArray.getString(index));
                } catch (JSONException e) {
                    log.error("Invalid child json object: " + config.toString());
                }
            }
        }
        return apiUris;
    }

    public Optional<String> getMongoCollection() {
        if (!hasMongoConfig()) {
            return Optional.absent();
        }
        String mongoCollection = null;
        try {
            if (config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).has(NSBarcodeConfigKeys.MONGO_COLLECTION)) {
                mongoCollection = config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG)
                        .getString(NSBarcodeConfigKeys.MONGO_COLLECTION);
            }
        } catch (JSONException e) {
            // Should not see this
            log.warn("JSON exception: ", e);
        }
        return Optional.fromNullable(mongoCollection);
    }

    public Optional<String> getMongoDeviceStatusCollection(){
        if (! config.has(NSBarcodeConfigKeys.MONGO_CONFIG)) {
            return Optional.absent();
        }
        String deviceStatusCollection = null;
        try {
            if (config.has(NSBarcodeConfigKeys.MONGO_CONFIG) &&
                    config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).has(NSBarcodeConfigKeys.MONGO_COLLECTION)) {
                deviceStatusCollection = config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG)
                        .getString(NSBarcodeConfigKeys.MONGO_DEVICE_STATUS_COLLECTION);
            }
        } catch (JSONException e) {
            // Should not see this
            log.warn("JSON exception: ", e);
        }
        return Optional.fromNullable(deviceStatusCollection);
    }

    public boolean hasMongoConfig(){
        try {
            return config.has(NSBarcodeConfigKeys.MONGO_CONFIG) &&
                    config.getJSONObject(NSBarcodeConfigKeys.MONGO_CONFIG).has(NSBarcodeConfigKeys.MONGO_URI);
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean hasApiConfig(){
        try {
            return config.has(NSBarcodeConfigKeys.API_CONFIG) &&
                    config.getJSONObject(NSBarcodeConfigKeys.API_CONFIG)
                            .getJSONArray(NSBarcodeConfigKeys.API_URI).length() > 0;
        } catch (JSONException e) {
            return false;
        }
    }
}