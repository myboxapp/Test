package com.archibus.app.reservation.service;

import java.sql.Time;
import java.util.*;

import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.Recurrence;

/**
 * Interface for reservation service.  
 * 
 * @author Bart Vanderschoot
 * @since 20.1
 */
public interface IReservationService {


    /**
     * Find available rooms.
     * 
     * @param reservation the reservation
     * @param numberOfAttendees the number attendees
     * @param externalAllowed whether to return only rooms that are suitable for external guests
     * @param fixedResourceStandards the fixed resource standards
     * @param allDayEvent true for all day events, false for regular reservations
     * @param timeZone time zone to convert to
     * @return the list of available rooms
     * @throws ReservationException the reservation exception
     */
    List<RoomArrangement> findAvailableRooms(final RoomReservation reservation,
            final Integer numberOfAttendees, final boolean externalAllowed,
            final List<String> fixedResourceStandards, final boolean allDayEvent,
            final String timeZone) throws ReservationException;

    /**
     * Find available rooms.
     * 
     * @param reservation the reservation
     * @param numberOfAttendees the number attendees
     * @param externalAllowed whether to return only rooms that are suitable for external guests
     * @param fixedResourceStandards the fixed resource standards
     * @param allDayEvent true for all day events, false for regular reservations
     * @param recurrence the recurrence pattern
     * @param timeZone time zone to convert to
     * @return the list of available rooms
     * @throws ReservationException the reservation exception
     */
    List<RoomArrangement> findAvailableRoomsRecurrence(final RoomReservation reservation,
            final Integer numberOfAttendees, final boolean externalAllowed,
            final List<String> fixedResourceStandards, final boolean allDayEvent,
            final Recurrence recurrence, final String timeZone) throws ReservationException;

    /**
     * Find available rooms when editing recurrent reservations.
     *
     * @param roomReservation the room reservation
     * @param existingReservations the existing reservations
     * @param numberOfAttendees the number of attendees
     * @param externalAllowed the external allowed
     * @param fixedResourceStandards the fixed resource standards
     * @param allDayEvent the all day event
     * @param timeZone the time zone
     * @return the list of room arrangements
     * @throws ReservationException the reservation exception
     */
    List<RoomArrangement> findAvailableRooms(final RoomReservation roomReservation,
            final List<RoomReservation> existingReservations, final Integer numberOfAttendees,
            final boolean externalAllowed, final List<String> fixedResourceStandards,
            final boolean allDayEvent, final String timeZone)
                    throws ReservationException; 

    /**
     * Save recurring reservation.
     * 
     * @param reservation the reservation
     * @param recurrence the recurrence
     * @return list of created reservations
     * @throws ReservationException the reservation exception
     */
    List<RoomReservation> saveRecurringReservation(final IReservation reservation,
            final Recurrence recurrence) throws ReservationException;

    /**
     * Save a reservation.
     * 
     * @param reservation the reservation to save.
     * @throws ReservationException the reservation exception
     */
    void saveReservation(final IReservation reservation) throws ReservationException;

    /**
     * Verify whether the reservations with the given unique ID adhere to the given recurrence
     * pattern, i.e. every occurrence has a reservation and every reservation can be linked to an
     * occurrence.
     * 
     * @param uniqueId the unique id of the appointment series
     * @param pattern the recurrence pattern of the appointment series
     * @param startTime time of day that the appointments start
     * @param endTime time of day that the appointments end
     * @param timeZone the time zone used to specify the recurrence pattern and times
     * @return true if the reservations match the pattern, false otherwise
     * @throws ReservationException when an error occurs
     */
    boolean verifyRecurrencePattern(final String uniqueId, final Recurrence pattern,
            final Time startTime, final Time endTime, final String timeZone)
                    throws ReservationException;

    /**
     * Get active reservation.
     * 
     * @param reserveId reserve Id
     * @param timeZone timeZone
     * @return RoomReservation the room reservation (null if not found)
     */
    RoomReservation getActiveReservation(final Integer reserveId, final String timeZone);

    /**
     * Get room reservation by UniqueId.
     * 
     * @param uniqueId unique Id
     * @param timeZone timeZone
     * @return list of room reservations
     */
    List<RoomReservation> getByUniqueId(final String uniqueId, final String timeZone);

}
