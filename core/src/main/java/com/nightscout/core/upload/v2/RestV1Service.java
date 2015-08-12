package com.nightscout.core.upload.v2;

import org.json.JSONObject;

import java.util.List;

import retrofit.client.Response;
import retrofit.http.Header;
import retrofit.http.POST;

public interface RestV1Service {

  @POST("/entries")
  Response uploadEntries(@Header("api-secret") String apiSecret, List<JSONObject> jsonBlob);
}
