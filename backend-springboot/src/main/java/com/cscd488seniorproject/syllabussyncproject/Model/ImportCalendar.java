package com.cscd488seniorproject.syllabussyncproject.Model;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


public class ImportCalendar {
    
    public List<Map<String, String>> importCanvasCalendar(InputStream icsFile) throws Exception {
        List<Map<String, String>> events = new ArrayList<>();
        
        try {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(icsFile);
            
            for (Object component : calendar.getComponents(Component.VEVENT)) {
                VEvent event = (VEvent) component;
                Map<String, String> eventData = new HashMap<>();
                
                eventData.put("summary", getPropertyValue(event, Property.SUMMARY));
                eventData.put("description", getPropertyValue(event, Property.DESCRIPTION));
                eventData.put("startDate", getPropertyValue(event, Property.DTSTART));
                eventData.put("endDate", getPropertyValue(event, Property.DTEND));
                eventData.put("location", getPropertyValue(event, Property.LOCATION));
                eventData.put("uid", getPropertyValue(event, Property.UID));
                
                events.add(eventData);
            }
        } catch (IOException e) {
            throw new Exception("Error parsing iCalendar file: " + e.getMessage());
        }
        
        return events;
    }
    
    private String getPropertyValue(VEvent event, String propertyName) {
        Property property = event.getProperty(propertyName);
        return property != null ? property.getValue() : "";
    }
}