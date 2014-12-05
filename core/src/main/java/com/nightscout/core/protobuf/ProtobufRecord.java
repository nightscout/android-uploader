package com.nightscout.core.protobuf;

public interface ProtobufRecord {
    public <T> T toProtobuf();
}
