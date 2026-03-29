package com.cscd488seniorproject.syllabussyncproject.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.cscd488seniorproject.syllabussyncproject.service.CanvasCalendarService;


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

