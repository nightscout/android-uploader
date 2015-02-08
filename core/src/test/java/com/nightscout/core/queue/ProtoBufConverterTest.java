package com.nightscout.core.queue;

import com.nightscout.core.model.TestObject;
import com.squareup.wire.Wire;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProtoBufConverterTest {

  private ProtoBufConverter<TestObject> converter;

  @Before
  public void setUp() {
    converter = new ProtoBufConverter<>(new Wire(), TestObject.class);
  }

  @Test
  public void shouldConvertToAndFromProtobuf() throws IOException {
    TestObject toBeConverted = new TestObject.Builder().name("test").build();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    converter.toStream(toBeConverted, outputStream);
    outputStream.flush();
    assertThat(converter.from(outputStream.toByteArray()), is(toBeConverted));
  }
}
