package com.archibus.app.reservation.service;

import java.util.*;

import com.archibus.app.reservation.domain.*;
import com.archibus.utility.ExceptionBase;

/**
 * Provides Calendar information from a remote service.<br/>
 * There can be several implementations, for now there is only Exchange and Web Central for no
 * integration.
 * 
 * <p>
 * Represents services to create and update appointments and find free/busy calendar events for
 * attendees.
 * </p>
 * 
 * <p>
 * Managed by Spring. All beans are defined with scope prototype.
 * </p>
 * 
 * @author Bart Vanderschoot
 * @since 21.2
 * 
 */
public interface ICalendarService {
    
    
    /**
     * Check service available.  
     *
     * @throws ExceptionBase the exception base
     */
    void checkServiceAvailable() throws ExceptionBase;
    
    /**
     * Find the attendee availability. This routine returns the list of the calendar events for the
     * attendee.
     * 
     * @param startDate start date in the specified requestedTimeZone
     * @param endDate end date in the specified requestedTimeZone
     * @param requestedTimeZone the time zone to present the availability information in
     * @param email email of the attendee
     * @return List of Calendar Events
     * @throws ExceptionBase translated exception
     */
    List<ICalendarEvent> findAttendeeAvailability(final Date startDate, final Date endDate,
            final TimeZone requestedTimeZone, final String email) throws ExceptionBase;
    
    /**
     * Get the attendees' response status.
     * 
     * @param reservation the reservation to get the response status for
     * @return list of attendee response status
     * @throws ExceptionBase translated exception
     */
    List<AttendeeResponseStatus> getAttendeesResponseStatus(final IReservation reservation)
            throws ExceptionBase;
    
    /**
     * Create an appointment that doesn't have a unique id yet.
     * 
     * @param reservation the reservation
     * @return unique id (remote service identifier)
     * 
     * @throws ExceptionBase translated exception
     */
    String createAppointment(final IReservation reservation) throws ExceptionBase;
    
    /**
     * Update an appointment. This can be a single appointment or a whole appointment series.
     * 
     * @param reservation the reservation
     * 
     * @throws ExceptionBase translated exception
     */
    void updateAppointment(final IReservation reservation) throws ExceptionBase;
    
    /**
     * Update an appointment that is part of a recurrence series.
     *
     * @param reservation the reservation
     * @param originalReservation the original reservation
     * @throws ExceptionBase translated exception
     */
    void updateAppointmentOccurrence(final IReservation reservation, final IReservation originalReservation) 
            throws ExceptionBase;
    
    /**
     * Cancel the appointment.
     * 
     * @param reservation the reservation
     * @param message cancellation message
     * 
     * @throws ExceptionBase translated exception
     */
    void cancelAppointment(final IReservation reservation, final String message)
            throws ExceptionBase;
    
    /**
     * Cancel single appointment in a recurrent meeting.
     * 
     * @param reservation the reservation
     * @param message cancellation message
     * 
     * @throws ExceptionBase translated exception
     */
    void cancelAppointmentOccurrence(final IReservation reservation, final String message)
            throws ExceptionBase;
    
}