package com.nightscout.core.queue;

import com.squareup.tape.FileObjectQueue;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;

public class ProtoBufQueue<T extends Message> extends FileObjectQueue<T> {

  public ProtoBufQueue(File file, Wire wire, Class<T> clazz) throws IOException {
    super(file, new ProtoBufConverter<T>(wire, clazz));
  }
}
