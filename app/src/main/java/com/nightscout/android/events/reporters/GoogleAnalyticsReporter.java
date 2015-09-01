package com.nightscout.android.events.reporters;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.nightscout.core.events.reporters.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class GoogleAnalyticsReporter implements EventReporter {

  private final Tracker tracker;

  public GoogleAnalyticsReporter(Tracker tracker) {
    this.tracker = checkNotNull(tracker);
  }

  @Override
  public void report(EventType type, EventSeverity severity, String message) {
    if (severity != EventSeverity.ERROR) {
      return;
    }

    tracker.send(new HitBuilders.ExceptionBuilder().setDescription(message)
                     .setFatal(false)
                     .build());
  }
}
