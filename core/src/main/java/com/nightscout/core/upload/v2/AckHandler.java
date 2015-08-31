package com.nightscout.core.upload.v2;

import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.utils.DexcomG4Utils;
import com.squareup.wire.Message;

import net.tribe7.common.collect.Lists;

import java.util.List;

public class AckHandler extends G4DataHandler {

  @Override
  protected boolean isEnabled() {
    return true;
  }

  @Override
  protected List<Message> handleG4Data(G4Data download) {
    List<Message> output = Lists.newArrayList();
    DexcomG4Utils.addAllEntriesAsMessages(download, output);
    return output;
  }
}
