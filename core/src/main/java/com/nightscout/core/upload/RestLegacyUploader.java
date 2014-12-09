package com.nightscout.core.upload;

import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.records.DeviceStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class RestLegacyUploader extends AbstractRestUploader {

    public RestLegacyUploader(NightscoutPreferences preferences, URL url) {
        super(preferences, url);
    }

    private JSONObject toJSONObject(GlucoseDataSet record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", "dexcom");
        json.put("date", record.getDisplayTime().getTime());
        json.put("dateString", record.getDisplayTime().toString());
        json.put("sgv", Integer.parseInt(String.valueOf(record.getBGValue())));
        json.put("direction", record.getTrend().friendlyTrendName());
        return json;
    }

    private JSONObject toJSONObject(DeviceStatus deviceStatus) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uploaderBattery", deviceStatus.getBatteryLevel());
        return json;
    }

    @Override
    protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
        try {
            return doPost("entries", toJSONObject(glucoseDataSet));
        } catch (JSONException e) {
            log.error("Could not create JSON object for legacy rest glucose data set.", e);
            return false;
        }
    }

    // TODO(trhodeos): is devicestatus supported in legacy apis?
    @Override
    protected boolean doUpload(DeviceStatus deviceStatus) throws IOException {
        try {
            return doPost("devicestatus", toJSONObject(deviceStatus));
        } catch (JSONException e) {
            log.error("Could not create JSON object for legacy rest device status.", e);
            return false;
        }
    }
}
