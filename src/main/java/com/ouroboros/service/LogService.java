package com.ouroboros.service;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ouroboros.logging.InMemoryLogAppender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogService {

    public List<String> getRecentLogs() {
        return InMemoryLogAppender.getEvents().stream()
                .map(this::formatEvent)
                .collect(Collectors.toList());
    }

    private String formatEvent(ILoggingEvent event) {
        String timestamp = Instant.ofEpochMilli(event.getTimeStamp())
                                  .atZone(ZoneId.systemDefault())
                                  .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return String.format("%s [%s] %s - %s",
                timestamp,
                event.getLevel(),
                event.getLoggerName(),
                event.getFormattedMessage());
    }
}