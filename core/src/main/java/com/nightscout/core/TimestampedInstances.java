package com.nightscout.core;

import com.nightscout.core.model.v2.G4Timestamp;

import net.tribe7.common.primitives.Longs;

import org.joda.time.DateTime;

public final class TimestampedInstances {

  private TimestampedInstances() {}

  private abstract static class AbstractTimestamped implements Timestamped {
    @Override
    public int compareTo(Timestamped o) {
      if (o == null) {
        return 1;
      }
      return Longs.compare(getTimestampSec(), o.getTimestampSec());
    }
  }

  public static Timestamped now() {
    return new AbstractTimestamped() {
      @Override
      public long getTimestampSec() {
        return new DateTime().getMillis() / 1000;
      }
    };
  }

  public static Timestamped epoch() {
    return new AbstractTimestamped() {
      @Override
      public long getTimestampSec() {
        return 0;
      }
    };
  }

  public static Timestamped fromG4Timestamp(final G4Timestamp timestamp) {
    return new AbstractTimestamped() {
      @Override
      public long getTimestampSec() {
        return timestamp.system_time_sec;
      }
    };
  }
}
