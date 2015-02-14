package com.nightscout.android;

import com.nightscout.android.debug.DebugNightscoutModule;
import com.nightscout.android.modules.NightscoutModule;
import com.nightscout.android.modules.UploaderModule;

public final class Modules {

  private Modules() {
  }

  static Object[] list(Nightscout app) {
    return new Object[]{
        new NightscoutModule(app),
        new DebugNightscoutModule(),
        new UploaderModule()
    };
  }
}
