package com.nightscout.core.upload.v2;

import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.DexcomG4Utils;
import com.squareup.wire.Message;

import net.tribe7.common.collect.Lists;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class MqttHandler extends G4DataHandler {
  private static final Logger log = LoggerFactory.getLogger(MqttHandler.class);

  private final NightscoutPreferences preferences;
  private final String mqttClientId;
  private final Object mqttClientLock = new Object();
  private MqttClient mqttClient;
  private String mqttEndpoint;
  private String mqttUser;
  private String mqttPass;

  public MqttHandler(NightscoutPreferences preferences, String mqttClientId) {
    this.preferences = checkNotNull(preferences);
    this.mqttClientId = checkNotNull(mqttClientId);
    handleSettingsRefresh();
  }

  @Override
  protected boolean isEnabled() {
    return preferences.isMqttEnabled();
  }

  @Override
  protected List<Message> handleG4Data(G4Data download) {
    MqttMessage mqttMessage = new MqttMessage(download.toByteArray());
    mqttMessage.setQos(2);
    try {
      synchronized (mqttClientLock) {
        mqttClient.publish("/protobuf/g4data", mqttMessage);
      }
    } catch (MqttException e) {
      log.error("Error publishing g4 data to mqtt topic.", e);
      return Lists.newArrayList();
    }

    List<Message> outputMessages = Lists.newArrayList();
    DexcomG4Utils.addAllEntriesAsMessages(download, outputMessages);
    return outputMessages;
  }

  @Override
  protected void handleSettingsRefresh() {
    boolean somethingChanged = false;
    if (mqttEndpoint == null || !mqttEndpoint.equals(preferences.getMqttEndpoint())) {
      mqttEndpoint = preferences.getMqttEndpoint();
      somethingChanged = true;
    }
    if (mqttUser == null || !mqttUser.equals(preferences.getMqttUser())) {
      mqttUser = preferences.getMqttUser();
      somethingChanged = true;
    }
    if (mqttPass == null || !mqttPass.equals(preferences.getMqttPass())) {
      mqttPass = preferences.getMqttPass();
      somethingChanged = true;
    }
    if (!somethingChanged) {
      return;
    }
    try {
      synchronized (mqttClientLock) {
        if (mqttClient != null && mqttClient.isConnected()) {
          mqttClient.close();
        }
        mqttClient = new MqttClient(mqttEndpoint, mqttClientId, new MemoryPersistence());
        mqttClient.connect(getMqttConnectionOptions(mqttUser, mqttPass));
      }
    } catch (MqttSecurityException e) {
      log.error("Error with credentials.", e);
    } catch (MqttException e) {
      log.error("Unknown mqtt exception.", e);
    }
  }

  private MqttConnectOptions getMqttConnectionOptions(String mqttUser, String mqttPass) {
    MqttConnectOptions mqttOptions = new MqttConnectOptions();
    mqttOptions.setCleanSession(true);
    mqttOptions.setKeepAliveInterval(150000);
    mqttOptions.setUserName(mqttUser);
    mqttOptions.setPassword(mqttPass.toCharArray());
    return mqttOptions;
  }
}
