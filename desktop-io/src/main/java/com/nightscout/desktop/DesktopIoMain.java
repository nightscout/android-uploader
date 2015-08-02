package com.nightscout.desktop;

import com.embeddedunveiled.serial.SerialComManager;
import com.nightscout.core.drivers.AbstractUploader;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.DexcomG4;
import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.events.LoggingEventReporter;
import com.nightscout.core.preferences.TestPreferences;
import com.squareup.wire.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesktopIoMain {

  private static final Logger log = LoggerFactory.getLogger(DesktopIoMain.class);

  // usage: ./gradlew clean desktop-io:run
  public static void main(String[] args) throws Exception {
    SerialComManager scm = new SerialComManager();
    log.info("Available com ports:");
    for (String s : scm.listAvailableComPorts()) {
      log.info("\t{}", s);
    }
    String comPort = "/dev/cu.usbmodem1411";
    if (args.length > 0) {
      comPort = args[0];
    }
    log.info("Using com port {}.", comPort);
    DeviceTransport transport = new DesktopSerialTransport(comPort);
    transport.open();
    TestPreferences preferences = new TestPreferences();
    preferences.setCalibrationUploadEnabled(true);
    preferences.setDeviceType(DeviceType.DEXCOM_G4);
    preferences.setRawEnabled(true);
    preferences.setMeterUploadEnabled(true);
    preferences.setInsertionUploadEnabled(true);

    DexcomG4 dexcomG4 = new DexcomG4(transport, preferences, new AbstractUploader() {
      @Override
      public int getBatteryLevel() {
        return 100;
      }
    });
    dexcomG4.setNumOfPages(20);
    dexcomG4.setReporter(new LoggingEventReporter());
    Message download = dexcomG4.download();

    log.info("Results: {}", download.toString());
    transport.close();
  }
}
