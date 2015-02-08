package com.nightscout.core.model;


import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.OPTIONAL;

public final class TestObject extends Message {

  @ProtoField(tag = 1, type = STRING, label = OPTIONAL)
  public String name;

  private TestObject(Builder builder) {
    this.name = builder.name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TestObject that = (TestObject) o;

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  public static final class Builder extends Message.Builder<TestObject> {

    public String name;

    public Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public TestObject build() {
      return new TestObject(this);
    }
  }
}
