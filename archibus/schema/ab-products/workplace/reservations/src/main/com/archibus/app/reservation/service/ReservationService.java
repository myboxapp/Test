package com.archibus.app.reservation.service;

import java.sql.Time;
import java.util.*;

import com.archibus.app.reservation.dao.*;
import com.archibus.app.reservation.dao.datasource.RoomReservationDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.*;
import com.archibus.app.reservation.service.actions.*;
import com.archibus.app.reservation.service.helpers.ReservationServiceHelper;
import com.archibus.app.reservation.util.TimeZoneConverter;
import com.archibus.utility.StringUtil;

/**
 * Reservation Service class.
 * 
 * The service class is a business logic layer used for different front-end handlers. Both event
 * handlers and remote services can use service class.
 * 
 * @author Bart Vanderschoot
 * 
 */ 
public class ReservationService implements IReservationService {

    /** Error message when a room is not available. */
    // @translatable
    private static final String ROOM_NOT_AVAILABLE = "The room is not available.";
  
    /** The room arrangement data source. */
    private IRoomArrangementDataSource roomArrangementDataSource;

    /** The room reservation data source. */
    private IRoomReservationDataSource roomReservationDataSource;
 
    /** The Work Request service. */
    private WorkRequestService workRequestService;
    
    /** {@inheritDoc} */
    public RoomReservation getActiveReservation(final Integer reserveId, final String timeZone) {
        return this.roomReservationDataSource.getActiveReservation(reserveId, timeZone);
    }

    /**
     * {@inheritDoc}
     */
    public List<RoomReservation> getByUniqueId(final String uniqueId, final String timeZone) {
        return this.roomReservationDataSource.getByUniqueId(uniqueId, timeZone);
    }
 
    /**
     * {@inheritDoc}
     */
    public final List<RoomArrangement> findAvailableRooms(final RoomReservation reservation,
            final Integer numberAttendees, final boolean externalAllowed,
            final List<String> fixedResourceStandards, final boolean allDayEvent,
            final String timeZone) throws ReservationException {

        return this.roomArrangementDataSource.findAvailableRooms(reservation, numberAttendees,
                externalAllowed, fixedResourceStandards, allDayEvent, timeZone);
    }

    /**
     * {@inheritDoc}
     */
    public final List<RoomArrangement> findAvailableRoomsRecurrence(
            final RoomReservation reservation, final Integer numberOfAttendees,
            final boolean externalAllowed, final List<String> fixedResourceStandards,
            final boolean allDayEvent, final Recurrence recurrence, final String timeZone)
                    throws ReservationException {

        // Get the room arrangements in the correct requested time zone.
        final List<RoomArrangement> roomArrangements =
                this.roomArrangementDataSource.findAvailableRooms(reservation, numberOfAttendees,
                        externalAllowed, fixedResourceStandards, allDayEvent, timeZone);

        if (!roomArrangements.isEmpty() && recurrence instanceof AbstractIntervalPattern) {
            // when editing provide the existing occurrences, so their allocations can be ignored
            // in the availability check
            List<RoomReservation> existingOccurrences = null;

            if (reservation.getParentId() != null) {
                // look for all linked reservations
                // No need for timezone conversion, we only need to get the reservation ids;
                // dates and times are fixed by the reservation and recurrence object parameters.
                existingOccurrences =
                        this.roomReservationDataSource.getByParentId(reservation.getParentId(),
                            null, null);
            }

            /*
             * Loop through the pattern to further restrict the list of available arrangements. We
             * do not need to consider the requested time zone here, because no new arrangements are
             * added to the list.
             */
            final AbstractIntervalPattern pattern = (AbstractIntervalPattern) recurrence;
            pattern.loopThroughRepeats(new FindAvailableRoomsOccurrenceAction(reservation,
                    numberOfAttendees, fixedResourceStandards, allDayEvent, existingOccurrences,
                    roomArrangements, this.roomArrangementDataSource));
        }

        return roomArrangements;
    }


    /**
     * {@inheritDoc}
     */
    public final List<RoomArrangement> findAvailableRooms(final RoomReservation roomReservation,
            final List<RoomReservation> existingReservations, final Integer numberOfAttendees,
            final boolean externalAllowed, final List<String> fixedResourceStandards,
            final boolean allDayEvent, final String timeZone)
                    throws ReservationException {

        // Get the room arrangements in the correct requested time zone.
        List<RoomArrangement> roomArrangements = null;

        for (RoomReservation reservation : existingReservations) {  

            roomReservation.setStartDate(reservation.getStartDate());
            roomReservation.setEndDate(reservation.getEndDate()); 

            roomReservation.setReserveId(reservation.getReserveId());

            final List<RoomArrangement> rooms =
                    this.roomArrangementDataSource.findAvailableRooms(roomReservation, numberOfAttendees,
                            externalAllowed, fixedResourceStandards, allDayEvent, timeZone);

            if (roomArrangements == null) {
                roomArrangements = rooms;
            } else {
                roomArrangements.retainAll(rooms);  
            }

        } 

        return roomArrangements;
    }

    /**
     * {@inheritDoc}
     */
    public final List<RoomReservation> saveRecurringReservation(final IReservation reservation,
            final Recurrence recurrence) throws ReservationException {

        final List<RoomReservation> savedReservations = new ArrayList<RoomReservation>();

        if (reservation instanceof RoomReservation) {            
            RoomReservation roomReservation = (RoomReservation) reservation;

            if (roomReservation.getRoomAllocations().isEmpty()) {
                // @translatable
                throw new ReservationException("Room reservation has no room allocated.",
                        ReservationService.class);
            }

            // if creating new reservation, make sure there are no leftover reservations with the
            // same unique id
            boolean leftOverReservations = false;
            if (StringUtil.notNullOrEmpty(reservation.getUniqueId())
                    && (reservation.getReserveId() == null || reservation.getReserveId() == 0)) {
                final List<RoomReservation> reservations =
                        this.roomReservationDataSource.getByUniqueId(reservation.getUniqueId(), null);
                leftOverReservations = reservations != null && !reservations.isEmpty();
            }
            if (leftOverReservations) {
                // @translatable
                throw new ReservationException(
                    "Reservation creation has timed out. \n\nTo resolve this please contact your ARCHIBUS Administrator to reduce the maximum number of occurrences that can be created. Afterwards, delete the appointment to cancel all orphaned reservations.\n",
                        ReservationService.class);
            } 

            // when editing, fetch the existing reservation on this date
            // no need for timezone conversion, timezone is copied from new reservation object
            final List<RoomReservation> existingReservations =
                    this.roomReservationDataSource.getByParentId(roomReservation.getParentId(),
                        null, roomReservation.getStartDate());

            if (existingReservations == null) {
                roomReservation = insertRecurringReservations(recurrence,
                        savedReservations, roomReservation);
            } else {
                ReservationServiceHelper.updateExistingReservations(this.roomReservationDataSource,
                    this.roomArrangementDataSource, savedReservations, roomReservation,
                    existingReservations);
            }

            // create or update the work request 
            this.workRequestService.createWorkRequest(roomReservation, true);     
        } 

        return savedReservations;
    }



 
    /**
     * {@inheritDoc}
     */
    public final void saveReservation(final IReservation reservation) throws ReservationException {

        if (reservation instanceof RoomReservation) {
            final RoomReservation roomReservation = (RoomReservation) reservation;

            // check possible conflicts for rooms
            final List<RoomArrangement> roomArrangements =
                    this.roomArrangementDataSource.findAvailableRooms(roomReservation, null, false,
                            null, false, null);
            if (roomArrangements.isEmpty()) {
                throw new ReservableNotAvailableException(roomReservation.getRoomAllocations()
                    .get(0).getRoomArrangement(), ROOM_NOT_AVAILABLE,
                        ReservationService.class);
            }

            // Mark the reservation as recurring if it contains a recurrence pattern.
            if (roomReservation.getRecurrence() != null && roomReservation.getParentId() == null
                    && StringUtil.notNullOrEmpty(roomReservation.getUniqueId())) {
                final List<RoomReservation> existingReservations =
                        getByUniqueId(roomReservation.getUniqueId(), null); 
                
                // save the reservation and all allocations
                this.roomReservationDataSource.save(roomReservation);

                ReservationServiceHelper.markRecurring(this.roomReservationDataSource,
                    roomReservation, existingReservations);
            } else {
                // when a single occurrence is updated, check the date is changed
                checkRecurringDateModified(roomReservation);
                // Resource availability is checked in the RoomReservationDataSource.
                // if no conflicts, is safe to save
                this.roomReservationDataSource.save(roomReservation);               
            }

            // create or update the work request
            this.workRequestService.createWorkRequest(roomReservation, false);         

        } else {
            // @translatable
            throw new ReservationException("This is no room reservation.", ReservationService.class);
        }
    }


   
    /**
     * {@inheritDoc}
     */
    public final boolean verifyRecurrencePattern(final String uniqueId, final Recurrence pattern,
            final Time startTime, final Time endTime, final String timeZone)
                    throws ReservationException {

        final List<RoomReservation> reservations =
                this.roomReservationDataSource.getByUniqueId(uniqueId, null);

        // Begin by assuming the reservations match the pattern.
        boolean reservationsMatchThePattern = true;

        if (pattern.getNumberOfOccurrences() == null
                || pattern.getNumberOfOccurrences() != reservations.size()) {
            // The number of reservations doesn't equal the the number of occurrences.
            reservationsMatchThePattern = false;
        } else if (pattern instanceof AbstractIntervalPattern) {
            final Map<Date, RoomReservation> reservationMap =
                    TimeZoneConverter.toRequestorTimeZone(reservations, timeZone);

            final AbstractIntervalPattern intervalPattern = (AbstractIntervalPattern) pattern;

            final RoomReservation reservation = reservationMap.get(intervalPattern.getStartDate());
            if (reservation == null
                    || !reservation.getStartTime().toString().equals(startTime.toString())
                    || !reservation.getEndTime().toString().equals(endTime.toString())) {
                reservationsMatchThePattern = false;
            } else {
                final VerifyRecurrencePatternOccurrenceAction action =
                        new VerifyRecurrencePatternOccurrenceAction(startTime, endTime,
                                reservationMap);
                intervalPattern.loopThroughRepeats(action);
                reservationsMatchThePattern = action.getFirstDateWithoutReservation() == null;
            }
        }

        return reservationsMatchThePattern;
    }

    /**
     * 
     * Setter RoomReservationDataSource.
     * 
     * @param roomReservationDataSource roomReservationDataSource to set
     */
    public final void setRoomReservationDataSource(
            final RoomReservationDataSource roomReservationDataSource) {
        this.roomReservationDataSource = roomReservationDataSource;
    }

    /**
     * 
     * Setter for RoomArrangementDataSource.
     * 
     * @param roomArrangementDataSource roomArrangementDataSource to set
     */
    public final void setRoomArrangementDataSource(
            final IRoomArrangementDataSource roomArrangementDataSource) {
        this.roomArrangementDataSource = roomArrangementDataSource;
    } 

 
    /** 
     * Sets the work request service.
     *
     * @param workRequestService the new work request service
     */
    public void setWorkRequestService(final WorkRequestService workRequestService) {
        this.workRequestService = workRequestService;
    } 
    
    /**
     * Insert recurring reservations.
     * 
     * @param recurrence the recurrence
     * @param savedReservations all saved reservations will be added to this list in local time
     * @param roomReservation the room reservation
     * @return the room reservation
     */
    private RoomReservation insertRecurringReservations(
            final Recurrence recurrence,
            final List<RoomReservation> savedReservations,
            final RoomReservation roomReservation) {
        
        if (recurrence == null) {
            // @translatable
            throw new ReservationException("Recurrence is not defined.", ReservationService.class);
        }

        // for a new recurrent reservation, the dates are calculated
        if (recurrence instanceof AbstractIntervalPattern) {
            final AbstractIntervalPattern pattern = (AbstractIntervalPattern) recurrence;
            final String requestorTimeZone = roomReservation.getTimeZone();
            // save the first base reservation
            saveReservation(roomReservation);
            // Set its parent reservation ID.                
            final int parentId = roomReservation.getParentId() == null 
                    ? roomReservation.getReserveId() : roomReservation.getParentId();  
            this.roomReservationDataSource.markRecurring(roomReservation,
                    parentId, pattern.toString());
            savedReservations.add(roomReservation);

            // get the saved copy with the proper time zone
            final RoomReservation activeReservation =
                    this.roomReservationDataSource.getActiveReservation(
                            roomReservation.getReserveId(), requestorTimeZone);

            // loop through the pattern using the saved copy
            pattern.loopThroughRepeats(new SaveRecurringReservationOccurrenceAction(
                    savedReservations, this.roomReservationDataSource,
                    this.roomArrangementDataSource, activeReservation));
        }
        return roomReservation;
    }
    
    
    /**
     * Check recurring date modified.
     *
     * @param roomReservation the room reservation
     */
    private void checkRecurringDateModified(final RoomReservation roomReservation) {
        // if a single occurrence is updated and the date is changed set the reserve.recurring_date_modified to 1  
        if (roomReservation.getReserveId() != null  && roomReservation.getRecurringRule() != null
                    && roomReservation.getParentId() != null) {
            final RoomReservation originalReservation =
                    this.roomReservationDataSource.get(roomReservation.getReserveId());
            if (!originalReservation.getStartDate().equals(roomReservation.getStartDate())) {
              // Log.debug("Reservation occurrence date changed from " + originalReservation.getStartDate() + " to " + roomReservation.getStartDate());
               roomReservation.setRecurringDateModified(1);
           }
        }
    }

 
}
