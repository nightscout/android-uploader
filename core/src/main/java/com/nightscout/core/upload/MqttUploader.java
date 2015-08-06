package com.nightscout.core.upload;

import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class MqttUploader extends BaseUploader {
  public static final Logger log = LoggerFactory.getLogger(MqttUploader.class);

  private final MqttClient mqttClient;

  public MqttUploader(NightscoutPreferences preferences, MqttClient mqttClient) {
    super(preferences);
    this.mqttClient = checkNotNull(mqttClient);
  }

  @Override
  protected boolean doUpload(GlucoseDataSet glucoseDataSet) throws IOException {
    if (!mqttClient.isConnected()) {
      // TODO(this shouldn't really happen...?
      return false;
    }
    MqttMessage mqttMessage = new MqttMessage();
    try {
      mqttClient.publish("/downloads/protobuf", mqttMessage);
      return true;
    } catch (MqttException e) {
      log.error("Error handling mqtt upload", e);
    }
    return false;
  }
}