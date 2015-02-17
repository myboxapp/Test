package com.archibus.app.reservation.domain;

import java.util.*;

import microsoft.exchange.webservices.data.CalendarEvent;

import org.json.*;

/**
 * The Attendee Availability will be used in the free/busy view.
 * 
 * For a specific employee and time period, it will show all his calendar events. Free/busy can be
 * obtained from Exchange.
 * 
 * @author Bart Vanderschoot
 * 
 */
public class AttendeeAvailability {
    
    /**
     * Calendard events registered in Exchange.
     */
    private Collection<CalendarEvent> calendarEvents;
    
    /** The email. */
    private String email;
    
    /** The end date. */
    private Date endDate;
    
    /** The start date. */
    private Date startDate;
    
    /**
     * Default constructor.
     */
    public AttendeeAvailability() {
        // default constructor
    }
    
    /**
     * Constructor using parameters.
     * 
     * @param email the email
     * @param calendarEvents the calendar events
     */
    public AttendeeAvailability(final String email, final Collection<CalendarEvent> calendarEvents) {
        this.email = email;
        this.calendarEvents = calendarEvents;
    }
    
    /**
     * Gets the calendar events.
     * 
     * @return the calendar events
     */
    public final Collection<CalendarEvent> getCalendarEvents() {
        return this.calendarEvents;
    }
    
    // Disable StrictDuplicate CHECKSTYLE warning. Justification: AttendeeAvailability has common
    // properties.
    /**
     * Gets the attendee email.
     * 
     * @return the email
     */
    public final String getEmail() {
        return this.email;
    }
    
    /**
     * Gets the end date.
     * 
     * @return the end date
     */
    public final Date getEndDate() {
        return this.endDate;
    }
    
    /**
     * Gets the start date.
     * 
     * @return the start date
     */
    public final Date getStartDate() {
        return this.startDate;
    }
    
    /**
     * Sets the calendar events.
     * 
     * @param calendarEvents the new calendar events
     */
    public final void setCalendarEvents(final Collection<CalendarEvent> calendarEvents) {
        this.calendarEvents = calendarEvents;
    }
    
    // Disable StrictDuplicate CHECKSTYLE warning. Justification: this class has the same properties
    // as Visitor.
    
    /**
     * Sets the email.
     * 
     * @param email the new email
     */
    public final void setEmail(final String email) {
        this.email = email;
    }
    
    /**
     * Sets the end date.
     * 
     * @param endDate the new end date
     */
    public final void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }
    
    // Disable StrictDuplicate CHECKSTYLE warning. Justification: this class has common properties.
    /**
     * Sets the start date.
     * 
     * @param startDate the new start date
     */
    public final void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }
    
    /**
     * Convert oject to JSON format to be used in JavaScript.
     * 
     * @return JSON Object representing the availability object.
     */
    public final JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        json.put("email", this.email);
        json.put("startDate", this.startDate);
        json.put("endDate", this.endDate);
        
        final JSONArray events = new JSONArray();
        
        for (final CalendarEvent calendarEvent : this.calendarEvents) {
            final JSONObject event = new JSONObject();
            
            event.put("startTime", calendarEvent.getStartTime());
            event.put("endTime", calendarEvent.getEndTime());
            event.put("details", calendarEvent.getDetails());
            
            events.put(event);
        }
        
        json.put("events", events);
        
        return json;
    }
    
}
