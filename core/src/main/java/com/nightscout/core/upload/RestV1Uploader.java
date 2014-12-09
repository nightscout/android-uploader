package com.nightscout.core.upload;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.records.DeviceStatus;
import com.squareup.okhttp.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkArgument;

public class RestV1Uploader extends AbstractRestUploader {
    private final String token;

    public RestV1Uploader(NightscoutPreferences preferences, URL url, String secret) {
        super(preferences, url);
        checkArgument(!Strings.isNullOrEmpty(secret), "Rest API v1 requires a secret.");
        token = generateToken(secret);
    }

    private String generateToken(String secret) {
        return HashCode.fromBytes(secret.getBytes(Charsets.UTF_8)).toString();
    }

    protected String getToken() {
        return token;
    }

    @Override
    protected void setExtraHeaders(Request.Builder post) {
        post.addHeader("api-secret", getToken());
    }

    private JSONObject toJSONObject(GlucoseDataSet record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", "dexcom");
        json.put("date", record.getDisplayTime().getTime());
        json.put("dateString", record.getDisplayTime().toString());
        json.put("sgv", Integer.parseInt(String.valueOf(record.getBGValue())));
        json.put("direction", record.getTrend().friendlyTrendName());
        json.put("type", "sgv");
        if (getPreferences().isSensorUploadEnabled()) {
            json.put("filtered", record.getFiltered());
            json.put("unfiltered", record.getUnfiltered());
            json.put("rssi", record.getRssi());
        }
        return json;
    }

    private JSONObject toJSONObject(MeterRecord record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", "dexcom");
        json.put("type", "mbg");
        json.put("date", record.getDisplayTime().getTime());
        json.put("dateString", record.getDisplayTime().toString());
        json.put("mbg", Integer.parseInt(String.valueOf(record.getMeterBG())));
        return json;
    }

    private JSONObject toJSONObject(CalRecord record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", "dexcom");
        json.put("type", "cal");
        json.put("date", record.getDisplayTime().getTime());
        json.put("dateString", record.getDisplayTime().toString());
        json.put("slope", record.getSlope());
        json.put("intercept", record.getIntercept());
        json.put("scale", record.getScale());
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
            log.error("Could not create JSON object for rest v1 glucose data set.", e);
            return false;
        }
    }

    @Override
    protected boolean doUpload(MeterRecord meterRecord) throws IOException {
        try {
            // TODO(trhodeos): in Uploader.java, this method still used 'entries' as the endpoint,
            // but this seems like a bug to me.
            return doPost("entries", toJSONObject(meterRecord));
        } catch (JSONException e) {
            log.error("Could not create JSON object for rest v1 meter record.", e);
            return false;
        }
    }

    @Override
    protected boolean doUpload(CalRecord calRecord) throws IOException {
        try {
            // TODO(trhodeos): in Uploader.java, this method still used 'entries' as the endpoint,
            // but this seems like a bug to me.
            return doPost("entries", toJSONObject(calRecord));
        } catch (JSONException e) {
            log.error("Could not create JSON object for rest v1 cal record.", e);
            return false;
        }
    }

    @Override
    protected boolean doUpload(DeviceStatus deviceStatus) throws IOException {
        try {
            return doPost("devicestatus", toJSONObject(deviceStatus));
        } catch (JSONException e) {
            log.error("Could not create JSON object for rest v1 device status.", e);
            return false;
        }
    }
}
