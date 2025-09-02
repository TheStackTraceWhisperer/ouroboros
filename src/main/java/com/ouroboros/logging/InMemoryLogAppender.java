package com.ouroboros.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_LOGS = 100;
    private static final List<ILoggingEvent> events = Collections.synchronizedList(new LinkedList<>());

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (events.size() >= MAX_LOGS) {
            events.remove(0);
        }
        events.add(eventObject);
    }

    public static List<ILoggingEvent> getEvents() {
        return new LinkedList<>(events); // Return a copy
    }
}