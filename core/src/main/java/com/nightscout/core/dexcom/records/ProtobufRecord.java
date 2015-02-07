package com.nightscout.core.dexcom.records;

import com.google.common.base.Optional;

public interface ProtobufRecord {
    public <T> T toProtoBuf();

    public Optional<? extends GenericTimestampRecord> fromProtoBuf(byte[] protoArray);
}
