package com.nightscout.core.drivers.Medtronic.request;

import com.nightscout.core.drivers.Medtronic.OpCodes;

public class SignalStrengthRequest extends RequestBase {
    public SignalStrengthRequest() {
        OPCODE = OpCodes.SIGNAL_STRENGTH;
        packet = OPCODE;
    }
}