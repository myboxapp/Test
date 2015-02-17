package com.archibus.app.reservation.domain;

import java.sql.Time;
import java.util.*;

import com.archibus.app.reservation.domain.recurrence.Recurrence;

/**
 * Interface for a reservation.
 * 
 * @author Bart Vanderschoot
 * 
 */
public interface IReservation extends ITimePeriodBased {
    
    /**
     * Gets the attendees.
     * 
     * @return the attendees
     */
    String getAttendees();
    
    /**
     * Gets the requested by.
     * 
     * @return the requested by
     */
    String getRequestedBy();
    
    /**
     * Gets the requested for.
     * 
     * @return the requested for
     */
    String getRequestedFor();
    
    /**
     * Gets the email.
     * 
     * @return the email
     */
    String getEmail();
    
    /**
     * Sets the reservation name.
     * 
     * @param reservationName the new reservation name
     */
    void setReservationName(String reservationName);
    
    /**
     * Gets the reservation name.
     * 
     * @return the reservation name
     */
    String getReservationName();
    
    /**
     * Gets the comments.
     * 
     * @return the comments
     */
    String getComments();
    
    /**
     * Gets the time period.
     * 
     * @return the time period
     */
    TimePeriod getTimePeriod();
    
    /**
     * Gets the reserve id.
     * 
     * @return the reserve id
     */
    Integer getReserveId();
    
    /**
     * Gets the resource allocations.
     * 
     * @return the resource allocations
     */
    List<ResourceAllocation> getResourceAllocations();
    
    /**
     * Gets the status.
     * 
     * @return the status
     */
    String getStatus();
    
    /**
     * Gets the unique id coming from Exchange/Outlook.
     * 
     * @return the unique id
     */
    String getUniqueId();
    
    /**
     * 
     * Set the unique Id coming from Exchange/Outlook.
     * 
     * @param uniqueId unique Id
     */
    void setUniqueId(String uniqueId);
    
    /**
     * Set the last modified by.
     * 
     * @param employeeId employee id
     */
    void setLastModifiedBy(String employeeId);
    
    /**
     * Set the last modified date.
     * 
     * @param date date
     */
    void setLastModifiedDate(Date date);
    
    /**
     * Sets the reserve id.
     * 
     * @param id the new reserve id
     */
    void setReserveId(Integer id);
    
    /**
     * Gets the time zone.
     * 
     * @return the time zone
     */
    String getTimeZone();
    
    /**
     * Determines the current local date for this reservation.
     * 
     * @return the current local date
     */
    Date determineCurrentLocalDate();
    
    /**
     * Determines the current local time for this reservation.
     * 
     * @return the current local time
     */
    Time determineCurrentLocalTime();    
    
    /**
     * Gets the recurring rule.
     * 
     * @return the recurring rule
     */
    String getRecurringRule(); 
    
    /**
     * Gets the parent id.
     * 
     * @return the parent id
     */
    Integer getParentId();
    
    /**
     * Set the recurrence pattern (not for database persistence).
     * 
     * @param recurrence the recurrence pattern
     */
    void setRecurrence(Recurrence recurrence);
    
    /**
     * Get the temporary recurrence pattern.
     * 
     * @return the recurrence pattern
     */
    Recurrence getRecurrence();

    /**
     * Get the time period of the reservation in the specified time zone. Converts from building
     * time to the given time zone, so use this only if the reservation is still in building time.
     * 
     * @param timeZoneId the time zone identifier
     * @return time period in the given time zone
     */
    TimePeriod getTimePeriodInTimeZone(final String timeZoneId);
    
    /**
     * Calculate total cost for the reservation including all allocations.
     * The value is set in the object and returned.
     * 
     * @return total cost
     */
    double calculateTotalCost();

    /**
     * Sets the attendees.
     * 
     * @param attendees the new attendees
     */
    void setAttendees(final String attendees);
    
    /**
     * Sets the comments.
     * 
     * @param comments the new comments
     */
    void setComments(final String comments);
    
    
    /**
     * Gets the recurring date modified.
     *
     * @return the recurring date modified
     */
    int getRecurringDateModified();
}
