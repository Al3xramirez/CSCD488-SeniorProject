package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.controller.dto.CanvasSubscribeRequest;
import com.cscd488seniorproject.syllabussyncproject.controller.dto.ExternalEventResponse;
import com.cscd488seniorproject.syllabussyncproject.entity.CalendarSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CanvasCalendarController{

    // TODO: Add dependencies (e.g., repositories, external API clients) as needed via constructor injection

    public CalendarSubscription subscribe(CanvasSubscribeRequest request) {
        // TODO: Implement subscription logic (e.g., save to database, call Canvas API)
        // Placeholder: Return a new CalendarSubscription based on request
        CalendarSubscription subscription = new CalendarSubscription();
        // Populate subscription fields from request (e.g., subscription.setUserId(request.getUserId());)
        return subscription;
    }

    public List<ExternalEventResponse> getMergedEventsForUserWindow(String userId) {
        
        return List.of();
    }
}