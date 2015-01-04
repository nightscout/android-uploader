package com.nightscout.core.drivers.Medtronic.request;

import com.nightscout.core.drivers.Medtronic.OpCodes;

public class LinkStatusRequest extends RequestBase {
    public LinkStatusRequest() {
        OPCODE = OpCodes.LINK_STATUS;
        packet = OPCODE;
    }
}
