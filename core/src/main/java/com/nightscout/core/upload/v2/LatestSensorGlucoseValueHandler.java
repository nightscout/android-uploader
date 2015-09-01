package com.nightscout.core.upload.v2;

import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.utils.DexcomG4Utils;
import com.nightscout.core.utils.ListUtils;
import com.squareup.wire.Message;

import net.tribe7.common.base.Optional;
import net.tribe7.common.collect.Lists;

import java.util.List;

public abstract class LatestSensorGlucoseValueHandler extends G4DataHandler {

  protected abstract boolean handleLastValue(Optional<SensorGlucoseValue> value);

  @Override
  protected List<Message> handleG4Data(G4Data download) {
    List<Message> output = Lists.newArrayList();
    if (handleLastValue(ListUtils.lastOrEmpty(download.sensor_glucose_values))) {
      DexcomG4Utils.addAllEntriesAsMessages(download, output);
    }
    return output;
  }
}
