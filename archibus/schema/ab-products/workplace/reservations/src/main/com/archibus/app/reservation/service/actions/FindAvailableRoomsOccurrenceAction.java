package com.archibus.app.reservation.service.actions;

import java.util.*;

import com.archibus.app.reservation.dao.IRoomArrangementDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.AbstractIntervalPattern;

/**
 * Provides a method to find available rooms for all occurrences in an IntervalPattern, via
 * implementation of the OccurrenceAction interface.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class FindAvailableRoomsOccurrenceAction implements AbstractIntervalPattern.OccurrenceAction {
    
    /**
     * The remaining possible room arrangements. Only the the rooms that are available for all
     * occurrences are retained.
     */
    private final List<RoomArrangement> roomArrangements;
    
    /** The room arrangement data source, used to look for available rooms. */
    private final IRoomArrangementDataSource roomArrangementDataSource;
    
    /** Map with the existing existingReservations in the recurrence pattern. */
    private final Map<Date, RoomReservation> existingReservations =
            new HashMap<Date, RoomReservation>();
    
    /** The room reservation used to look for available rooms. */
    private final RoomReservation reservation;
    
    /** The minimum numberAttendees of the rooms to find. */
    private final Integer numberAttendees;
    
    /** The required fixed resource standards. */
    private final List<String> fixedResourceStandards;
    
    /** Whether we are looking for rooms available for an all day event. */
    private final boolean allDayEvent;
    
    /**
     * Constructor.
     * 
     * @param firstReservation the reservation object representing the first occurrence of the
     *            interval pattern
     * @param numberAttendees the number of attendees
     * @param fixedResourceStandards the required fixed resource standards
     * @param allDayEvent whether we are looking for rooms that will be booked for an all day event
     * @param existingOccurrences the existing occurrences of the recurring reservation
     * @param roomArrangements the room arrangements available for the first occurrence; rooms not
     *            available for other occurrences will be removed from this list
     * @param roomArrangementDataSource the room arrangement data source to use for finding
     *            available rooms
     */
    public FindAvailableRoomsOccurrenceAction(final RoomReservation firstReservation,
            final Integer numberAttendees, final List<String> fixedResourceStandards,
            final boolean allDayEvent, final List<RoomReservation> existingOccurrences,
            final List<RoomArrangement> roomArrangements,
            final IRoomArrangementDataSource roomArrangementDataSource) {
        this.reservation = firstReservation;
        this.numberAttendees = numberAttendees;
        this.fixedResourceStandards = fixedResourceStandards;
        this.allDayEvent = allDayEvent;
        this.roomArrangements = roomArrangements;
        this.roomArrangementDataSource = roomArrangementDataSource;
        
        if (existingOccurrences != null) {
            for (final RoomReservation occurrence : existingOccurrences) {
                this.existingReservations.put(occurrence.getStartDate(), occurrence);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean handleOccurrence(final Date date) throws ReservationException {
        this.reservation.setStartDate(date);
        this.reservation.setEndDate(date);
        
        final RoomReservation occurrence = this.existingReservations.get(date);
        Integer reservationId = null;
        if (occurrence != null) {
            reservationId = occurrence.getReserveId();
        }
        this.reservation.setReserveId(reservationId);
        
        // No need to pass timezone here, it would only be used for converting dayEnd and dayStart
        // properties. Only the pkeys are used for the retainAll.
        final List<RoomArrangement> rooms =
                this.roomArrangementDataSource.findAvailableRooms(this.reservation,
                    this.numberAttendees, false, this.fixedResourceStandards, this.allDayEvent,
                    null);
        this.roomArrangements.retainAll(rooms);
        return !this.roomArrangements.isEmpty();
    }
}
