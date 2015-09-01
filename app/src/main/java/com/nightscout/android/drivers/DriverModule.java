package com.nightscout.android.drivers;

import android.app.Application;

import com.nightscout.android.*;
import com.nightscout.android.DexcomDriverSupplier;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.preferences.NightscoutPreferences;

import net.tribe7.common.base.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module(
    injects = {
        CollectorService.class,
    },
    library = true
)
public class DriverModule {

  @Inject
  Application application;

  @Inject
  NightscoutPreferences preferences;

  @Provides
  @Named("dexcomDriverSupplier")
  public Supplier<DeviceTransport> providesDexcomDriverSupplier() {
    return new DexcomDriverSupplier(application.getApplicationContext(), preferences);
  }
}
