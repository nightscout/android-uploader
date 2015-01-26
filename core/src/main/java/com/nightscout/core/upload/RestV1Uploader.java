package com.nightscout.core.upload;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.model.CalibrationEntry;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.RestUriUtils;

import org.apache.http.message.AbstractHttpMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

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
        json.put("date", record.getDisplayTime().getTime());
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
        return json;
    }

    private JSONObject toJSONObject(MeterEntry record) throws JSONException {
        JSONObject json = new JSONObject();
        Date timestamp = Utils.receiverTimeToDate(record.disp_timestamp_sec);
        json.put("device", "dexcom");
        json.put("type", "mbg");
        json.put("date", timestamp.getTime());
        json.put("dateString", timestamp.toString());
        json.put("mbg", Integer.parseInt(String.valueOf(record.meter_bg_mgdl)));
        return json;
    }

    private JSONObject toJSONObject(CalibrationEntry record) throws JSONException {
        JSONObject json = new JSONObject();
        Date timestamp = Utils.receiverTimeToDate(record.disp_timestamp_sec);
        json.put("device", "dexcom");
        json.put("type", "cal");
        json.put("date", timestamp.getTime());
        json.put("dateString", timestamp.toString());
        json.put("slope", record.slope);
        json.put("intercept", record.intercept);
        json.put("scale", record.scale);
        return json;
    }

    private JSONObject toJSONObject(AbstractUploaderDevice deviceStatus) throws JSONException {
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
    protected boolean doUpload(MeterEntry meterRecord) throws IOException {
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
    protected boolean doUpload(CalibrationEntry calRecord) throws IOException {
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
