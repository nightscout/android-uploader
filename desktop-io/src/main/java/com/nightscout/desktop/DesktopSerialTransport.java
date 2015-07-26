package com.nightscout.desktop;

import com.embeddedunveiled.serial.SerialComManager;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.G4ConnectionState;

import java.io.IOException;

import rx.functions.Action1;

public class DesktopSerialTransport implements DeviceTransport {

  private final SerialComManager scm;
  private final String commPort;
  private long handle;

  public DesktopSerialTransport(String commPort) {
    this.scm = new SerialComManager();
    this.commPort = commPort;
    this.handle = -1;
  }

  @Override
  public void open() throws IOException {
    handle = scm.openComPort(commPort, true, true, false);
    scm.configureComPortData(handle, SerialComManager.DATABITS.DB_8, SerialComManager.STOPBITS.SB_1, SerialComManager.PARITY.P_NONE, SerialComManager.BAUDRATE.B115200, 0);
    scm.configureComPortControl(handle, SerialComManager.FLOWCONTROL.NONE, 'x', 'x', false, false);
  }

  @Override
  public void close() throws IOException {
    if (handle > -1) {
      scm.closeComPort(handle);
      handle = -1;
    }
  }

  @Override
  public byte[] read(int size, int timeoutMillis) throws IOException {
    byte[] output = new byte[size];
    int bytesRead = 0;
    while (bytesRead < size) {
      byte[] read = scm.readBytes(handle, size - bytesRead);
      System.arraycopy(read, 0, output, bytesRead, read.length);
      bytesRead += read.length;
    }
    return output;
  }

  @Override
  public int write(byte[] src, int timeoutMillis) throws IOException {
    if (scm.writeBytes(handle, src)) {
      return src.length;
    } else {
      return 0;
    }
  }

  @Override
  public boolean isConnected() {
    return handle > -1;
  }

  @Override
  public void registerConnectionListener(Action1<G4ConnectionState> connectionListener) {
    // no-op
  }
}
