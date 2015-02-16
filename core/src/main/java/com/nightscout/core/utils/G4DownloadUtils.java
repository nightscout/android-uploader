package com.nightscout.core.utils;

import com.squareup.wire.Message;

import java.util.List;

public class G4DownloadUtils {
  public static <T extends Message> T getLatest(List<T> list) {
    return list.get(list.size() - 1);
  }
}
