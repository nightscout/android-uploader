package com.nightscout.core.drivers.Medtronic.request;

import com.nightscout.core.drivers.Medtronic.OpCodes;

public class UsbInterfaceStatsRequest extends RequestBase {
    public UsbInterfaceStatsRequest() {
        OPCODE = OpCodes.USB_STATS;
        packet = OPCODE;
    }
}
