package com.nightscout.core.events.reporters;

import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IteratingEventReporter implements com.nightscout.core.events.reporters.EventReporter {
  private static final Logger log = LoggerFactory.getLogger(IteratingEventReporter.class);

  private final Iterable<com.nightscout.core.events.reporters.EventReporter> eventReporters;

  public IteratingEventReporter(final Iterable<com.nightscout.core.events.reporters.EventReporter> eventReporters) {
    this.eventReporters = eventReporters;
  }

  @Override
  public void report(EventType type, EventSeverity severity, String message) {
    for (com.nightscout.core.events.reporters.EventReporter eventReporter : eventReporters) {
      try {
        eventReporter.report(type, severity, message);
      } catch (Exception e) {
        // Catch exceptions, just in case.
        log.error("Error encountered while reporting event '{}'.", message, e);
      }
    }
  }
}
