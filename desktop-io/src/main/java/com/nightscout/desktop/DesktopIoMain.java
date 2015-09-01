package com.nightscout.desktop;

import com.embeddedunveiled.serial.SerialComManager;
import com.nightscout.core.TimestampedInstances;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.DexcomG4;
import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.events.reporters.LoggingEventReporter;
import com.nightscout.core.preferences.TestPreferences;
import com.squareup.wire.Message;

import net.tribe7.common.base.Optional;
import net.tribe7.common.base.Suppliers;

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

    DexcomG4 dexcomG4 = new DexcomG4(Suppliers.ofInstance(transport), new LoggingEventReporter());

    Message download = dexcomG4.downloadAllAfter(Optional.of(TimestampedInstances.epoch()));

    log.info("Results: {}", download.toString());
    transport.close();
  }
}
