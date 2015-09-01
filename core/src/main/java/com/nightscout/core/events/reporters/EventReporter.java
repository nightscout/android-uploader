package com.nightscout.core.events.reporters;

import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

public interface EventReporter {
    void report(EventType type, EventSeverity severity, String message);
}
