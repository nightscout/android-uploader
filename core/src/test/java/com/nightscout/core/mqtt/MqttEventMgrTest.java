package com.nightscout.core.mqtt;


import com.nightscout.core.events.EventReporter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MqttEventMgrTest {
    private MqttClient mockClient;
    private MqttPinger mockPinger;
    private MqttTimer mockTimer;
    private MqttConnectOptions options;
    private MqttEventMgr manager;
    private EventReporter mockReporter;


    @Before
    public void setup() {
        options = new MqttConnectOptions();
        options.setUserName("test");
        options.setPassword("pass".toCharArray());
        options.setKeepAliveInterval(Constants.KEEPALIVE_INTERVAL);
        options.setCleanSession(Constants.MQTT_CLEAN_SESSION);
        mockClient = mock(MqttClient.class);
        mockPinger = mock(MqttPinger.class);
        mockTimer = mock(MqttTimer.class);
        mockReporter = mock(EventReporter.class);
        manager = new MqttEventMgr(mockClient, options, mockPinger, mockTimer, mockReporter);
    }

    private void setupConnectWithSecurityException() throws Exception {
        doThrow(new MqttSecurityException(1)).when(mockClient).connect((MqttConnectOptions) anyObject());
    }

    @Test
    public void connectWithSecurityExceptionShouldNotReconnectDelayed() throws Exception {
        setupConnectWithSecurityException();
        manager.connect();
        verify(mockTimer, never()).setTimer(anyInt());
    }

    @Test
    public void connectWithMqttExceptionShouldReconnectDelayed() throws Exception {
        doThrow(new MqttException(1)).when(mockClient).connect((MqttConnectOptions) anyObject());
        manager.setShouldReconnect(true);
        manager.connect();
        verify(mockTimer).setTimer(anyInt());
    }

    @Test
    public void connectWithSecurityExceptionShouldStopPingerIfActive() throws Exception {
        setupConnectWithSecurityException();
        when(mockPinger.isActive()).thenReturn(true);
        manager.connect();
        verify(mockPinger, times(1)).stop();
    }

    @Test
    public void connectWithSecurityExceptionShouldDeactivateTimerIfActive() throws Exception {
        setupConnectWithSecurityException();
        when(mockTimer.isActive()).thenReturn(true);
        manager.connect();
        verify(mockTimer, times(1)).deactivate();
    }


    @Test
    public void connectWithInactivePingerShouldActivatePinger() throws Exception {
        when(mockPinger.isActive()).thenReturn(false);
        manager.connect();
        verify(mockPinger, times(1)).start();
    }

    @Test
    public void reconnectDelayedWithInactiveTimerShouldActivateTimer() throws Exception {
        when(mockTimer.isActive()).thenReturn(false);
        manager.setShouldReconnect(true);
        manager.delayedReconnect();
        verify(mockTimer, times(1)).activate();
    }

    @Test
    public void reconnectWithInActiveNetworkShouldNotReconnect() throws Exception {
        when(mockPinger.isNetworkActive()).thenReturn(false);
        manager.reconnect();
        verify(mockClient, never()).close();
        verify(mockClient, never()).connect();
    }

    @Test
    public void reconnectInConnectedStateWithActiveNetworkShouldReconnect() throws Exception {
        when(mockPinger.isNetworkActive()).thenReturn(true);
        when(mockClient.isConnected()).thenReturn(true);
        manager.reconnect();
        verify(mockClient, times(1)).disconnect();
        verify(mockClient, times(1)).connect((MqttConnectOptions) anyObject());
    }

    @Test
    public void disconnectWhileNotConnectedShouldDeactivateTimer() {
        when(mockClient.isConnected()).thenReturn(false);
        manager.disconnect();
        verify(mockTimer, times(1)).deactivate();
    }

    @Test
    public void disconnectWhileNotConnectedShouldStopPinger() {
        when(mockClient.isConnected()).thenReturn(false);
        manager.disconnect();
        verify(mockPinger, times(1)).stop();
    }

    @Test
    public void disconnectWhileNotConnectedShouldNotDisconnect() throws Exception {
        when(mockClient.isConnected()).thenReturn(false);
        manager.disconnect();
        verify(mockClient, never()).disconnect();
    }

    @Test
    public void disconnectWhileConnectedShouldDisconnect() throws Exception {
        when(mockClient.isConnected()).thenReturn(true);
        manager.disconnect();
        verify(mockClient, times(1)).disconnect();
    }

    @Test
    public void closeWhileClientActiveShouldDisconnect() throws Exception {
        when(mockClient.isConnected()).thenReturn(true);
        manager.close();
        verify(mockClient, times(1)).disconnect();
    }

    @Test
    public void regiseringTwiceShouldNotAddToObserversTwice() throws Exception {
        MqttMgrObserver observer = mock(MqttMgrObserver.class);
        manager.registerObserver(observer);
        manager.registerObserver(observer);
        assertThat(manager.getNumberOfObservers(), is(1));
    }

    @Test
    public void unregisteringWithSingleRegisterShouldRemoveAllObservers() throws Exception {
        MqttMgrObserver observer = mock(MqttMgrObserver.class);
        manager.registerObserver(observer);
        manager.unregisterObserver(observer);
        assertThat(manager.getNumberOfObservers(), is(0));
    }

    @Test
    public void unregisteringWithMultipleRegisterShouldRemoveOneObservers() throws Exception {
        MqttMgrObserver observer1 = mock(MqttMgrObserver.class);
        MqttMgrObserver observer2 = mock(MqttMgrObserver.class);
        manager.registerObserver(observer1);
        manager.registerObserver(observer2);
        manager.unregisterObserver(observer1);
        assertThat(manager.getNumberOfObservers(), is(1));
    }

    // Moved some of the reconnect logic out from lostConnection to prevent double reconnects.
    // May readd it later.
    @Test
    public void lostConnectionShouldNotifyObserver() {
        MqttMgrObserver observer = mock(MqttMgrObserver.class);
        manager.registerObserver(observer);
        manager.connectionLost(new Throwable("Some random throwable"));
        verify(observer, times(1)).onDisconnect();
    }

    // Moved some of the reconnect logic out from lostConnection to prevent double reconnects.
    // May readd it later.
    @Test
    public void lostConnectionShouldSetTimerToReconnect() {
        MqttMgrObserver observer = mock(MqttMgrObserver.class);
        manager.setShouldReconnect(true);
        manager.connectionLost(new Throwable("Some random throwable"));
        verify(mockTimer).setTimer(anyInt());
    }

    @Test
    public void onMessageShouldNotifyObservers() {
        MqttMgrObserver observer = mock(MqttMgrObserver.class);
        manager.registerObserver(observer);
        byte[] message = "my message".getBytes();
        manager.messageArrived("/topic", new MqttMessage(message));
        verify(observer).onMessage(anyString(), (MqttMessage) anyObject());
    }

    @Test
    public void onMessageShouldNotifyMultipleObservers() {
        MqttMgrObserver observer1 = mock(MqttMgrObserver.class);
        MqttMgrObserver observer2 = mock(MqttMgrObserver.class);
        manager.registerObserver(observer1);
        manager.registerObserver(observer2);
        byte[] message = "my message".getBytes();
        manager.messageArrived("/topic", new MqttMessage(message));
        verify(observer1).onMessage(anyString(), (MqttMessage) anyObject());
        verify(observer2).onMessage(anyString(), (MqttMessage) anyObject());
    }

    @Test
    public void onMessageShouldNotifyMultipleObserversWithExceptions() {
        MqttMgrObserver observer1 = mock(MqttMgrObserver.class);
        MqttMgrObserver observer2 = mock(MqttMgrObserver.class);
        doThrow(new NullPointerException("NPE")).when(observer1).onMessage(anyString(), (MqttMessage) anyObject());
        manager.registerObserver(observer1);
        manager.registerObserver(observer2);
        manager.messageArrived("/topic", new MqttMessage("my message".getBytes()));
        verify(observer2).onMessage(anyString(), (MqttMessage) anyObject());
    }

    @Test
    public void onMessageShouldResetPinger() {
        manager.messageArrived("/topic", new MqttMessage("my message".getBytes()));
        verify(mockPinger, times(1)).reset();
    }

    @Test
    public void messageDeliveredShouldResetPinger() {
        IMqttDeliveryToken token = mock(IMqttDeliveryToken.class);
        manager.deliveryComplete(token);
        verify(mockPinger, times(1)).reset();
    }

    @Test
    public void subscribeMultipleTopicsShouldSubscribeToAllTopics() throws Exception {
        manager.subscribe("/topic/1", "/topic/2", "/topic/3");
        verify(mockClient, times(3)).subscribe(anyString(), anyInt());
    }

    @Test
    public void subscribeMultipleTopicsShouldSubscribeWithExceptions() throws Exception {
        doThrow(new MqttException(1)).when(mockClient).subscribe(eq("/topic/2"), anyInt());
        manager.subscribe("/topic/1", "/topic/2", "/topic/3");
        verify(mockClient, times(3)).subscribe(anyString(), anyInt());
    }

    @Test
    public void subscribeMultipleTopicsShouldReconnectWithExceptions() throws Exception {
        doThrow(new MqttException(1)).when(mockClient).subscribe(anyString(), anyInt());
        manager.setShouldReconnect(true);
        manager.subscribe("/topic/1", "/topic/2", "/topic/3");
        verify(mockTimer, times(1)).setTimer(anyInt());
    }

    @Test
    public void publishWithExceptionShouldReconnectDelayed() throws Exception {
        doThrow(new MqttException(1)).when(mockClient).publish(anyString(), (byte[]) anyObject(), anyInt(), anyBoolean());
        manager.setShouldReconnect(true);
        manager.publish("my message".getBytes(), "/topic/1");
        verify(mockTimer, times(1)).setTimer(anyInt());
    }

    @Test
    public void closeShouldUnregisterPinger() throws Exception {
        manager.close();
        verify(mockPinger).unregisterObserver((MqttPingerObserver) anyObject());
    }

    @Test
    public void closeShouldUnregisterTimer() throws Exception {
        manager.close();
        verify(mockTimer).unregisterObserver((MqttTimerObserver) anyObject());
    }

    @Test
    public void connectShouldRegisterWithPinger() throws Exception {
        when(mockPinger.isActive()).thenReturn(false);
        manager.connect();
        verify(mockPinger, times(1)).registerObserver(manager);
    }

    @Test
    public void reconnectDelayedShouldRegisterWithTimer() throws Exception {
        when(mockTimer.isActive()).thenReturn(false);
        manager.setShouldReconnect(true);
        manager.delayedReconnect();
        verify(mockTimer, times(1)).registerObserver(manager);
    }


}
