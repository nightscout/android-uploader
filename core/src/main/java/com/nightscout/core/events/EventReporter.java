package com.nightscout.core.events;

public interface EventReporter {
    public void report(EventType type, EventSeverity severity, String message);
}
