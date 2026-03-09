package com.cscd488seniorproject.syllabussyncproject.scheduler;

import com.cscd488seniorproject.syllabussyncproject.service.CanvasCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class CalendarSyncScheduler {

    private final CanvasCalendarService canvasCalendarService;

    @Scheduled(fixedDelayString = "${app.calendar.sync-interval-ms:1800000}")
    public void syncAllEnabledSubscriptions() {
        canvasCalendarService.syncAllEnabled();
    }
}

