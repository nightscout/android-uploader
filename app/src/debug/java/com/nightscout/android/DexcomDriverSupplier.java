package com.nightscout.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;

import com.nightscout.android.drivers.BluetoothTransport;
import com.nightscout.android.drivers.USB.CdcAcmSerialDriver;
import com.nightscout.android.drivers.USB.UsbSerialProber;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.drivers.DexcomG4;

import net.tribe7.common.base.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DexcomDriverSupplier implements Supplier<DeviceTransport>, SharedPreferences.OnSharedPreferenceChangeListener{

  public static final Logger log = LoggerFactory.getLogger(DexcomDriverSupplier.class);

  private final Context applicationContext;
  private final AndroidPreferences preferences;

  private DeviceTransport currentDeviceTransport;

  public DexcomDriverSupplier(Context applicationContext, AndroidPreferences preferences) {
    this.applicationContext = applicationContext;
    PreferenceManager.getDefaultSharedPreferences(applicationContext).registerOnSharedPreferenceChangeListener(
        this);
    this.preferences = preferences;
  }

  @Override
  public DeviceTransport get() {
    if (currentDeviceTransport != null) {
      if (!currentDeviceTransport.isConnected()) {
        try {
          currentDeviceTransport.open();
          return currentDeviceTransport;
        } catch (IOException e) {
          log.error("Error re-opening device. Re-initializing.");
        }
      } else {
        return currentDeviceTransport;
      }
    }

    // TODO(trhodeos): connecting unknown to USB seems wrong here.
    if (preferences.getDeviceType() == DeviceType.DEXCOM_G4 || preferences.getDeviceType() == DeviceType.UNKNOWN) {
      currentDeviceTransport = UsbSerialProber.acquire(
          (UsbManager) applicationContext.getSystemService(Context.USB_SERVICE), applicationContext);
      if (currentDeviceTransport == null) {
        return null;
      }
      ((CdcAcmSerialDriver) currentDeviceTransport)
          .setPowerManagementEnabled(preferences.isRootEnabled());
      ((CdcAcmSerialDriver) currentDeviceTransport).setUsbCriteria(DexcomG4.VENDOR_ID,
                                                                   DexcomG4.PRODUCT_ID,
                                                                   DexcomG4.DEVICE_CLASS,
                                                                   DexcomG4.DEVICE_SUBCLASS,
                                                                   DexcomG4.PROTOCOL);
    } else if (preferences.getDeviceType() == DeviceType.DEXCOM_G4_SHARE2) {
      currentDeviceTransport = new BluetoothTransport(applicationContext);
    } else {
      log.error("Unknown device type encountered {}. Returning null.", preferences.getDeviceType().name());
      return null;
    }
    try {
      currentDeviceTransport.open();
    } catch (IOException e) {
      log.error("Could not open device transport for device {}. Returning null.",
                preferences.getDeviceType().name());
      currentDeviceTransport = null;
    }
    return currentDeviceTransport;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(applicationContext.getString(R.string.dexcom_device_type))) {
      log.debug("Interesting value changed! {}", key);
      if (currentDeviceTransport != null && currentDeviceTransport.isConnected()) {
        try {
          currentDeviceTransport.close();
        } catch (IOException e) {
          log.error("Error closing device.", e);
        }
        currentDeviceTransport = null;
      }
    } else {
      log.debug("Meh... something uninteresting changed");
    }
  }
}
