package com.nightscout.core.events;

public interface EventReporter {
    void report(EventType type, EventSeverity severity, String message);
}
