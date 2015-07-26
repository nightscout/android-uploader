package com.nightscout.core.drivers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import rx.functions.Action1;

public class ReplayingDeviceTransport implements DeviceTransport {

  private final List<DeviceInteraction> actions;
  private int currentActionIndex;

  public ReplayingDeviceTransport(final List<DeviceInteraction> actions) {
    this.actions = actions;
    this.currentActionIndex = 0;
  }

  private DeviceInteraction getNextAction(DeviceInteraction.Type expectedType) {
    DeviceInteraction action = actions.get(currentActionIndex++);
    if (action.getAction() != expectedType) {
      throw new RuntimeException("Unexpected " + action.getAction().name());
    }
    return action;
  }

  @Override
  public void open() throws IOException {
  }

  @Override
  public void close() throws IOException {
  }

  private void copyIntoArray(byte[] src, byte[] dest) {
    if (src.length != dest.length) {
      throw new RuntimeException("Trying to copy arrays with differing lengths! " + src.length + " into " + dest.length);
    }
    System.arraycopy(src, 0, dest, 0, src.length);
  }

  @Override
  public byte[] read(int size, int timeoutMillis) throws IOException {
    DeviceInteraction action = getNextAction(DeviceInteraction.Type.READ);
    if (size != action.getByteLength()) {
      throw new RuntimeException("Trying to read differing length arrays! Recorded: " + action.getByteLength() + " vs Received: " + size);
    }
    return action.getByteArray();
  }

  private String b64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Override
  public int write(byte[] src, int timeoutMillis) throws IOException {
    DeviceInteraction action = getNextAction(DeviceInteraction.Type.WRITE);
    if (!Arrays.equals(src, action.getByteArray())) {
      throw new RuntimeException("Trying to write an unexpected array! Recorded: " + b64(action.getByteArray()) + " vs Received: " + b64(src));
    }
    return action.getByteLength();
  }

  @Override
  public boolean isConnected() {
    return false;
  }

  @Override
  public void registerConnectionListener(Action1<G4ConnectionState> connectionListener) {

  }
}
