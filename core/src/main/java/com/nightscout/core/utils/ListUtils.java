package com.nightscout.core.utils;

import net.tribe7.common.base.Optional;

import java.util.List;

public final class ListUtils {
  private ListUtils() {}

  public static <T> Optional<T> lastOrEmpty(final List<T> list) {
    if (list == null || list.size() < 1) {
      return Optional.absent();
    }
    return Optional.of(list.get(list.size() - 1));
  }
}
