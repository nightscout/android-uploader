package com.nightscout.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventReporter implements EventReporter {

  private static final Logger log = LoggerFactory.getLogger(LoggingEventReporter.class);

  @Override
  public void report(EventType type, EventSeverity severity, String message) {
    String fullMessage = type.name() + " - " + message;
    switch (severity) {
      case DEBUG:
        log.debug(fullMessage);
        break;
      case INFO:
        log.info(fullMessage);
        break;
      case WARN:
        log.warn(fullMessage);
        break;
      case ERROR:
        log.error(fullMessage);
        break;
    }
  }
}
