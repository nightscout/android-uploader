package com.nightscout.core.upload;

import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.RestUriUtils;

import org.apache.http.message.AbstractHttpMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;

import static net.tribe7.common.base.Preconditions.checkArgument;

public class RestV1Uploader extends AbstractRestUploader {
    private final String secret;

    public RestV1Uploader(NightscoutPreferences preferences, URI uri) {
        super(preferences, RestUriUtils.removeToken(uri));
        checkArgument(RestUriUtils.hasToken(uri), "Rest API v1 requires a token.");
        secret = RestUriUtils.generateSecret(uri.getUserInfo());
    }

    protected String getSecret() {
        return secret;
    }

    @Override
    protected void setExtraHeaders(AbstractHttpMessage post) {
        post.setHeader("api-secret", secret);
    }

    private JSONObject toJSONObjectEgv(GlucoseDataSet record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", deviceStr);
        json.put("date", record.getWallTime().getMillis());
        json.put("sysTime", record.getRawSysemTimeEgv());
        json.put("dateString", record.getWallTime().toString());
        json.put("sgv", record.getBgMgdl());
        json.put("direction", record.getTrend().friendlyTrendName());
        json.put("type", "sgv");
        return json;
    }

    private JSONObject toJSONObjectSensor(GlucoseDataSet record, JSONObject json) throws JSONException {
        json.put("filtered", record.getFiltered());
        json.put("unfiltered", record.getUnfiltered());
        json.put("rssi", record.getRssi());
        json.put("noise", record.getNoise());
        return json;
    }


    private JSONObject toJSONObject(GlucoseDataSet record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", deviceStr);
        json.put("date", record.getWallTime().getMillis());
        json.put("dateString", record.getWallTime().toString());
        json.put("sgv", record.getBgMgdl());
        json.put("direction", record.getTrend().friendlyTrendName());
        json.put("type", "sgv");
        if (getPreferences().isRawEnabled()) {
            json.put("filtered", record.getFiltered());
            json.put("unfiltered", record.getUnfiltered());
            json.put("rssi", record.getRssi());
            json.put("noise", record.getNoise());
        }
        log.error("Json: {}", json);
        return json;
    }

    private JSONObject toJSONObject(MeterRecord record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", deviceStr);
        json.put("type", "mbg");
        json.put("date", record.getWallTime().getMillis());
        json.put("sysTime", record.getRawSystemTimeSeconds());
        json.put("dateString", record.getWallTime().toString());
        json.put("mbg", record.getBgMgdl());
        log.error("Json: {}", json);
        return json;
    }

    private JSONObject toJSONObject(CalRecord record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", deviceStr);
        json.put("type", "cal");
        json.put("date", record.getWallTime().getMillis());
        json.put("sysTime", record.getRawSystemTimeSeconds());
        json.put("dateString", record.getWallTime().toString());
        json.put("slope", record.getSlope());
        json.put("intercept", record.getIntercept());
        json.put("scale", record.getScale());
        log.error("Json: {}", json);
        return json;
    }

    private JSONObject toJSONObject(InsertionRecord insertionRecord) throws JSONException {
        JSONObject output = new JSONObject();
        output.put("sysTime", insertionRecord.getRawSystemTimeSeconds());
        output.put("date", insertionRecord.getWallTime().getMillis());
        output.put("dateString", insertionRecord.getWallTime().toString());
        output.put("state", insertionRecord.getState().name());
        output.put("type", insertionRecord.getRecordType());
        return output;
    }

    private JSONObject toJSONObject(AbstractUploaderDevice deviceStatus, int rcvrBat) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uploaderBattery", deviceStatus.getBatteryLevel());
        json.put("receiverBattery", rcvrBat);
        log.error("Json: {}", json);
        return json;
    }

    @Override
    protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
        try {
            JSONObject json = toJSONObjectEgv(glucoseDataSet);
            log.error("Json: {}", json);
            if (!preferences.isRawEnabled()) {
                log.debug("Raw not enabled. JSON: {}", json);
                return doPost("entries", json);
            } else {
                if (glucoseDataSet.areRecordsMatched()) {
                    json = toJSONObjectSensor(glucoseDataSet, json);
                    log.debug("Records matched Json: {}", json);
                    return doPost("entries", json);
                } else {
                    log.error("Records not matched Json: {}", json);
                    boolean result = doPost("entries", json);
                    return result;
                }
            }
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
    protected boolean doUpload(InsertionRecord insertionRecord) throws IOException {
        try {
            // TODO(trhodeos): in Uploader.java, this method still used 'entries' as the endpoint,
            // but this seems like a bug to me.
            JSONObject json = toJSONObject(insertionRecord);
            log.info("Insertion JSON object to upload: {}", json.toString());
            return doPost("entries", toJSONObject(insertionRecord));
        } catch (JSONException e) {
            log.error("Could not create JSON object for rest v1 cal record.", e);
            return false;
        }
    }

    @Override
    protected boolean doUpload(AbstractUploaderDevice deviceStatus, int rcvrBat) throws IOException {
        try {
            return doPost("devicestatus", toJSONObject(deviceStatus, rcvrBat));
        } catch (JSONException e) {
            log.error("Could not create JSON object for rest v1 device status.", e);
            return false;
        }
    }
}
