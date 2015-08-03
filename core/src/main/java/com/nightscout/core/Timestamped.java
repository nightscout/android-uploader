package com.nightscout.core;

public interface Timestamped extends Comparable<Timestamped> {
  long getTimestampSec();
}
