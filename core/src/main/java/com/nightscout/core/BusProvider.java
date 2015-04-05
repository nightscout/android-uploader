package com.nightscout.core;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class BusProvider {
    private static Bus bus = new Bus(ThreadEnforcer.ANY);

    public static Bus getInstance() {
        return bus;
    }
}
