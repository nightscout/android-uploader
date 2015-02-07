package com.nightscout.android;

import com.nightscout.android.modules.NightscoutModule;

public final class Modules {
    private Modules() { }

    static Object[] list(Nightscout app) {
        return new Object[] {
                new NightscoutModule(app)
        };
    }
}
