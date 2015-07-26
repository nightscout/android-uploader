package com.nightscout.core.drivers;

import net.tribe7.common.base.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class DeviceInteraction {

  enum Type {
    OPEN,
    CLOSE,
    READ,
    WRITE
  }

  private final long executionTimeMs;
  private final int byteLength;
  private final byte[] byteArray;
  private final Type action;

  public DeviceInteraction(final Type action, final byte[] byteArray, final int byteLength, final long executionTimeMs) {
    this.action = action;
    this.byteArray = byteArray;
    this.byteLength = byteLength;
    this.executionTimeMs = executionTimeMs;
  }

  public long getExecutionTimeMs() {
    return executionTimeMs;
  }

  public int getByteLength() {
    return byteLength;
  }

  public byte[] getByteArray() {
    return byteArray;
  }

  public Type getAction() {
    return action;
  }

  public JSONObject toJson() throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("action", action.ordinal());
    jsonObject.put("byteArray", Base64.getEncoder().encode(byteArray));
    jsonObject.put("byteLength", byteLength);
    jsonObject.put("executionTimeMs", executionTimeMs);
    return jsonObject;
  }

  public Optional<DeviceInteraction> fromJson(JSONObject jsonObject) {
    try {
      Type type = Type.values()[jsonObject.getInt("action")];
      byte[] bytes = Base64.getDecoder().decode(jsonObject.getString("byteArray"));
      int len = jsonObject.getInt("byteLength");
      long execTimeMs = jsonObject.getLong("executionTimeMs");
      return Optional.of(new DeviceInteraction(type, bytes, len, execTimeMs));
    } catch (Exception e) {
      // Eat the exception.
      return Optional.absent();
    }
  }
}
