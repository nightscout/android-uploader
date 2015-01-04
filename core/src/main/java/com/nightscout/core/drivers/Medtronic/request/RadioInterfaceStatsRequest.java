package com.nightscout.core.drivers.Medtronic.request;

import com.nightscout.core.drivers.Medtronic.OpCodes;

public class RadioInterfaceStatsRequest extends RequestBase {
    public RadioInterfaceStatsRequest() {
        OPCODE = OpCodes.RADIO_STATS;
        packet = OPCODE;
    }
}
