package com.nightscout.core.queue;

import com.squareup.tape.FileObjectQueue;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.IOException;
import java.io.OutputStream;

public class ProtoBufConverter<T extends Message> implements FileObjectQueue.Converter<T> {

  private final Wire wire;
  private final Class<T> clazz;

  public ProtoBufConverter(Wire wire, Class<T> clazz) {
    this.wire = wire;
    this.clazz = clazz;
  }
  @Override
  public T from(byte[] bytes) throws IOException {
    return wire.parseFrom(bytes, clazz);
  }

  @Override
  public void toStream(T o, OutputStream bytes) throws IOException {
    bytes.write(o.toByteArray());
  }
}
