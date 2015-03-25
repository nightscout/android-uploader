package com.nightscout.core.upload;

import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.RestUriUtils;

import org.apache.http.message.AbstractHttpMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;

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

    private JSONObject toJSONObject(GlucoseDataSet record) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("device", "dexcom");
        json.put("date", record.getDisplayTime().getMillis());
        json.put("dateString", record.getDisplayTime().toString());
        json.put("sgv", Integer.parseInt(String.valueOf(record.getBgMgdl())));
        json.put("direction", record.getTrend().friendlyTrendName());
        json.put("type", "sgv");
        if (getPreferences().isSensorUploadEnabled()) {
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
//        Date timestamp = Utils.receiverTimeToDate(record.disp_timestamp_sec);
        json.put("device", "dexcom");
        json.put("type", "mbg");
        json.put("date", record.getWallTime().getMillis());
        json.put("dateString", record.getWallTime());
//        json.put("mbg", Integer.parseInt(String.valueOf(record.meter_bg_mgdl)));
        json.put("mbg", record.getBgMgdl());
        log.error("Json: {}", json);
        return json;
    }

    private JSONObject toJSONObject(CalRecord record) throws JSONException {
        JSONObject json = new JSONObject();
//        Date timestamp = Utils.receiverTimeToDate(record.disp_timestamp_sec);
        json.put("device", "dexcom");
        json.put("type", "cal");
        json.put("date", record.getWallTime().getMillis());
        json.put("dateString", record.getWallTime());
        json.put("slope", record.getSlope());
        json.put("intercept", record.getIntercept());
        json.put("scale", record.getScale());
        log.error("Json: {}", json);
        return json;
    }

    private JSONObject toJSONObject(AbstractUploaderDevice deviceStatus) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uploaderBattery", deviceStatus.getBatteryLevel());
        log.error("Json: {}", json);
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
    protected boolean doUpload(AbstractUploaderDevice deviceStatus) throws IOException {
        try {
            return doPost("devicestatus", toJSONObject(deviceStatus));
        } catch (JSONException e) {
            log.error("Could not create JSON object for rest v1 device status.", e);
            return false;
        }
    }
}
