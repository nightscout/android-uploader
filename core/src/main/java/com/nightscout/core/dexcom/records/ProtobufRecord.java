package com.nightscout.core.dexcom.records;

import net.tribe7.common.base.Optional;

public interface ProtobufRecord {
    public <T> T toProtoBuf();

    public Optional<? extends GenericTimestampRecord> fromProtoBuf(byte[] protoArray);
}
