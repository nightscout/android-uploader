package com.nightscout.core.drivers.Medtronic.request;

import com.nightscout.core.drivers.Medtronic.OpCodes;

public class ProductInfoRequest extends RequestBase {
    public ProductInfoRequest() {
        OPCODE = OpCodes.PRODUCT_INFO;
        packet = OPCODE;
    }
}
