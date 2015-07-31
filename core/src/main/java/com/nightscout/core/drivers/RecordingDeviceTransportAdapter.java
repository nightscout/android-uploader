package com.nightscout.core.drivers;

import net.tribe7.common.base.Stopwatch;
import net.tribe7.common.collect.Lists;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;

public class RecordingDeviceTransportAdapter implements DeviceTransport {

  private final DeviceTransport wrappedTransport;
  private List<DeviceInteraction> actions;

  public RecordingDeviceTransportAdapter(DeviceTransport wrappedTransport) {
    this.wrappedTransport = wrappedTransport;
    actions = Lists.newArrayList();
  }

  @Override
  public void open() throws IOException {
    Stopwatch watch = Stopwatch.createStarted();
    wrappedTransport.open();
    watch.stop();
    actions.add(new DeviceInteraction(DeviceInteraction.Type.OPEN, null, 0, watch.elapsed(TimeUnit.MILLISECONDS)));
  }

  @Override
  public void close() throws IOException {
    Stopwatch watch = Stopwatch.createStarted();
    wrappedTransport.close();
    watch.stop();
    actions.add(
        new DeviceInteraction(DeviceInteraction.Type.CLOSE, null, 0, watch.elapsed(TimeUnit.MILLISECONDS)));
  }

  @Override
  public byte[] read(int size, int timeoutMillis) throws IOException {
    Stopwatch watch = Stopwatch.createStarted();
    byte[] output = wrappedTransport.read(size, timeoutMillis);
    watch.stop();
    actions.add(
        new DeviceInteraction(DeviceInteraction.Type.READ, output, size,
                         watch.elapsed(TimeUnit.MILLISECONDS)));
    return output;
  }

  @Override
  public int write(byte[] src, int timeoutMillis) throws IOException {
    Stopwatch watch = Stopwatch.createStarted();
    int len = wrappedTransport.write(src, timeoutMillis);
    watch.stop();
    actions.add(
        new DeviceInteraction(DeviceInteraction.Type.WRITE, src, len,
                         watch.elapsed(TimeUnit.MILLISECONDS)));
    return len;
  }

  public List<DeviceInteraction> getActions() {
    return actions;
  }

  public void clearActions() {
    actions.clear();
  }

  @Override
  public boolean isConnected() {
    return wrappedTransport.isConnected();
  }

  @Override
  public void registerConnectionListener(Action1<G4ConnectionState> connectionListener) {
    wrappedTransport.registerConnectionListener(connectionListener);
  }
}
