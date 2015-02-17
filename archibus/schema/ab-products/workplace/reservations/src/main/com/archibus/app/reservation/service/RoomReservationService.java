package com.archibus.app.reservation.service;

import java.sql.Time;
import java.util.*;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.Recurrence;
import com.archibus.app.reservation.service.helpers.ReservationServiceHelper;
import com.archibus.app.reservation.util.*;
import com.archibus.datasource.data.*;
import com.archibus.utility.LocalDateTimeUtil;

/**
 * Room Reservation Service for workflow rules in the new reservation module.
 * <p>
 * This class provided all workflow rule for the room reservation creation view: Called from
 * ab-rr-create-room-reservation.axvw<br/>
 * Called from ab-rr-create-room-reservation-confirm.axvw<br/>
 * <p>
 * <p>
 * The class will be defined as a Spring bean and will reference other Spring beans. <br/>
 * The Calendar service can have different implementations that implement the ICalendar interface. <br/>
 * All Spring beans are defined as prototype.
 * </p>
 * 
 * @author Bart Vanderschoot
 * @since 21.2
 * 
 */
public class RoomReservationService extends RoomReservationServiceBase { 

    /** Whitespace between two parts of error message. */
    private static final String SPACE = " ";

    /** Error message when no reservation id is provided. */
    // @translatable
    private static final String NO_RESERVATION_ID = "No reservation id provided";

    /** Error message displayed when a calendar copy failed. */
    // @translatable
    private static final String CALENDAR_COPY_ERROR =
            "Reservation copied but an error occurred updating the requestor’s calendar and notifying attendees.";

    /** Error message displayed when a calendar create failed. */
    // @translatable
    private static final String CALENDAR_CREATE_ERROR =
            "Reservation created but an error occurred updating the requestor’s calendar and notifying attendees.";

    /** Error message displayed when a calendar cancel failed. */
    // @translatable
    private static final String CALENDAR_CANCEL_ERROR =
            "Reservation [{0}] cancelled but an error occurred updating the requestor’s calendar and notifying attendees.";
    
    /** Error message displayed when a calendar cancel failed. */
    // @translatable
    private static final String CALENDAR_CANCEL_RECURRING_ERROR =
            "Recurring reservation cancelled but an error occurred updating the requestor’s calendar and notifying attendees.";

    /** Error message displayed when a calendar update failed. */
    // @translatable
    private static final String CALENDAR_UPDATE_ERROR =
            "Reservation [{0}] updated but an error occurred updating the requestor’s calendar and notifying attendees.";

    /** The Constant RESERVE_RES_ID. */
    private static final String RESERVE_RES_ID = "reserve.res_id";

    /** The Constant RESERVE_DATE_START. */
    private static final String RESERVE_DATE_START = "reserve.date_start";

    /** The Constant RESERVE_DATE_END. */
    private static final String RESERVE_DATE_END = "reserve.date_end";
 
    /** The reservations service. */
    private IReservationService reservationService;
    
    /** The cancel service. */
    private CancelReservationService cancelReservationService;

    /** The logger. */
    private final Logger logger = Logger.getLogger(getClass().getName()); 


    /**
     * Save room reservation.
     * 
     * The room reservation can be a single or a recurrent reservation.
     * When editing a recurrent reservation, the recurrence pattern and reservation dates cannot change. 
     * When editing a single occurrence, the date might change.
     * 
     * 
     * @param reservation the reservation
     * @param roomAllocation the room allocation
     * @param resourceList the resource list
     * @param cateringList the catering list
     * @return the reservation record
     */
    public DataRecord saveRoomReservation(final DataRecord reservation,
            final DataRecord roomAllocation, final DataSetList resourceList,
            final DataSetList cateringList) {

        if (reservation.getDate(RESERVE_DATE_END) == null) {
            reservation.setValue(RESERVE_DATE_END, reservation.getDate(RESERVE_DATE_START));
        } 

        final RoomReservation roomReservation =
                (RoomReservation) this.roomReservationDataSource.convertRecordToObject(reservation);   

        // make sure the date of time values are set to 1899 
        roomReservation.setStartTime(new Time(TimePeriod.clearDate(roomReservation.getStartTime()).getTime()));
        roomReservation.setEndTime(new Time(TimePeriod.clearDate(roomReservation.getEndTime()).getTime()));       

        // add the room allocation to the reservation
        roomReservation.addRoomAllocation(this.roomAllocationDataSource
                .convertRecordToObject(roomAllocation));

        this.roomReservationDataSource.addResourceList(roomReservation, cateringList);
        this.roomReservationDataSource.addResourceList(roomReservation, resourceList);
        ReservationServiceHelper.validateEmails(roomReservation);

        // check the start and end time window
        ReservationServiceHelper.checkResourceAllocations(roomReservation);

        Recurrence recurrence = null;
        List<RoomReservation> createdReservations = null;

        final List<RoomReservation> originalReservations = getOriginalReservations(roomReservation);

        if (ReservationServiceHelper.isNewRecurrence(roomReservation)) {
            // prepare for new recurring reservation and correct the end date
            recurrence = ReservationServiceHelper.prepareNewRecurrence(roomReservation);

            // Room and Resource availability is verified by RoomReservationDataSource.
            createdReservations =
                    this.reservationService.saveRecurringReservation(roomReservation, recurrence); 
        } else {
            // Room and Resource availability is verified by RoomReservationDataSource.
            this.reservationService.saveReservation(roomReservation);
            createdReservations = new ArrayList<RoomReservation>();
            createdReservations.add(roomReservation);
        }
        // store the generated reservation instances in the reservation
        roomReservation.setCreatedReservations(createdReservations);

        // determine the time zone of the building and set the local time zone
        final String buildingId = roomAllocation.getString("reserve_rm.bl_id");
        roomReservation.setTimeZone(TimeZoneConverter.getTimeZoneIdForBuilding(buildingId));

        saveCalendarEvent(reservation, roomReservation, originalReservations);

        // Save the reservation(s) again to persist the appointment unique id.
        if (recurrence == null) {
            final RoomReservation storedReservation =
                    this.roomReservationDataSource.get(roomReservation.getReserveId());
            storedReservation.setUniqueId(roomReservation.getUniqueId());
            this.roomReservationDataSource.update(storedReservation);
        } else {
            // set the unique id for all reservation occurrences
            for (final RoomReservation createdReservation : createdReservations) {
                final RoomReservation storedReservation =
                        this.roomReservationDataSource.get(createdReservation.getReserveId());
                storedReservation.setUniqueId(roomReservation.getUniqueId());
                this.roomReservationDataSource.update(storedReservation);
            }
        }

        // update the reservation record to return
        reservation.setValue(this.roomReservationDataSource.getMainTableName() + Constants.DOT
                + Constants.RES_ID, roomReservation.getReserveId());
        reservation.setValue(this.roomReservationDataSource.getMainTableName() + Constants.DOT
                + Constants.UNIQUE_ID, roomReservation.getUniqueId());
        // reservation.setValue("reserve.res_parent", roomReservation.getParentId());
        reservation.setNew(false);

        ReservationsContextHelper.ensureResultMessageIsSet();

        return reservation;
    } 
    
 
    /**
     * Cancel single room reservation.
     *
     * @param reservationId reservation id
     * @param comments the comments
     */
    public void cancelRoomReservation(final Integer reservationId, final String comments) {
        RoomReservation roomReservation = null;
        if (reservationId != null && reservationId > 0) {
            // Get the reservation in the building time zone.
            roomReservation = this.reservationService.getActiveReservation(reservationId, null);
        } else {
            throw new ReservationException(NO_RESERVATION_ID, RoomReservationService.class);
        }
        if (roomReservation != null) {
            this.cancelReservationService.cancelReservation(roomReservation);
            cancelCalendarEvent(roomReservation, comments);
        }
        ReservationsContextHelper.ensureResultMessageIsSet();
    }

   

    /**
     * Cancel multiple reservations.
     *
     * @param reservations the reservations to cancel
     * @param message the message
     * @return list of reservation ids that could not be cancelled.
     */
    public List<Integer> cancelMultipleRoomReservations(final DataSetList reservations, final String message) {
        final List<Integer> failures = new ArrayList<Integer>();
        for (DataRecord record : reservations.getRecords()) {
            // get the active reservation and all allocations
            final int reservationId = record.getInt(RESERVE_RES_ID);
            final RoomReservation roomReservation = this.roomReservationDataSource.get(reservationId);

            if (roomReservation == null
                    || Constants.STATUS_REJECTED.equals(roomReservation.getStatus())) {
                failures.add(record.getInt(RESERVE_RES_ID));
            } else if (!Constants.STATUS_CANCELLED.equals(roomReservation.getStatus())) {
                try {
                    this.roomReservationDataSource.canBeCancelledByCurrentUser(roomReservation);
                } catch (ReservationException exception) {
                    // this one can't be cancelled, so skip and report
                    failures.add(roomReservation.getReserveId());
                    continue;
                }
                this.cancelReservationService.cancelReservation(roomReservation);
                cancelCalendarEvent(roomReservation, message);
            } 
        }

        ReservationsContextHelper.ensureResultMessageIsSet();
        return failures;
    }


    /**
     * Cancel recurring room reservation.
     *
     * @param reservationId reservation id of the first occurrence in the series to cancel
     * @param comments the comments
     * @return the list of id that failed
     */
    public List<Integer> cancelRecurringRoomReservation(final Integer reservationId, final String comments) {
        final List<Integer> failures = new ArrayList<Integer>();
        if (reservationId != null && reservationId > 0) {
            final RoomReservation reservation = this.roomReservationDataSource.get(reservationId);

            final List<List<IReservation>> cancelResult =
                    this.cancelReservationService.cancelRecurringReservation(reservation);
            final List<IReservation> cancelledReservations = cancelResult.get(0);
            final List<IReservation> failedReservations = cancelResult.get(1);
            
            // Check if this is the parent reservation, then all occurrences are cancelled.
            // If there are no more active reservations with same parent id, the meeting could
            // be removed from the calendar in a single operation.
            if (reservationId.equals(reservation.getParentId()) && failedReservations.isEmpty()) {
                try {
                    this.calendarService.cancelAppointment(reservation, comments);
                } catch (CalendarException exception) {
                    final String localizedMessage =
                            ReservationsContextHelper.localizeString(
                                CALENDAR_CANCEL_RECURRING_ERROR, RoomReservationService.class);
                    logger.warn(localizedMessage, exception);
                    ReservationsContextHelper.appendResultError(localizedMessage + SPACE
                            + exception.getPattern());
                }
            } else {
                for (IReservation roomReservation : cancelledReservations) {
                    this.cancelCalendarEvent(roomReservation, comments);
                }
            }

            for (IReservation failure : failedReservations) {
                failures.add(failure.getReserveId());
            }
        } else {
            throw new ReservationException(NO_RESERVATION_ID, RoomReservationService.class);
        }
        ReservationsContextHelper.ensureResultMessageIsSet();
        return failures;
    }

    /**
     * Calculate total cost of the reservation.
     * 
     * The total cost per reservation is calculated and multiplied by the number of occurrences.
     *
     * @param reservation the reservation.
     * @param roomAllocation the room allocation.
     * @param resources the equipment and services to be reserved
     * @param caterings the catering resources
     * @param numberOfOccurrences the number of occurrences
     * @return total cost of all occurrences
     */
    public Double calculateTotalCost(final DataRecord reservation, final DataRecord roomAllocation,
            final DataSetList resources, final DataSetList caterings, final int numberOfOccurrences) {

        final RoomReservation roomReservation =
                (RoomReservation) this.roomReservationDataSource.convertRecordToObject(reservation);
        // make sure the date of time values are set to 1899 
        roomReservation.setStartTime(new Time(TimePeriod.clearDate(roomReservation.getStartTime()).getTime()));
        roomReservation.setEndTime(new Time(TimePeriod.clearDate(roomReservation.getEndTime()).getTime()));

        // add the room allocation to the reservation
        roomReservation.addRoomAllocation(this.roomAllocationDataSource
                .convertRecordToObject(roomAllocation));

        // add the resources and catering
        this.roomReservationDataSource.addResourceList(roomReservation, caterings);
        this.roomReservationDataSource.addResourceList(roomReservation, resources);

        return this.roomReservationDataSource.calculateCosts(roomReservation) * numberOfOccurrences;
    }

    /**
     * Create a copy of the room reservation.
     *
     * @param reservationId the reservation id
     * @param reservationName the reservation name
     * @param startDate the start date
     */

    public void copyRoomReservation(final int reservationId, final String reservationName, final Date startDate) {
        final RoomReservation sourceReservation =
                this.reservationService.getActiveReservation(reservationId, null);

        if (sourceReservation == null) {
            // @translatable
            throw new ReservationException("Room reservation has been cancelled or rejected.",
                RoomReservationService.class);
        }

        if (sourceReservation.getRoomAllocations().isEmpty()) {
            // @translatable
            throw new ReservationException("Room reservation has no room allocated.",
                RoomReservationService.class);
        } 

        final RoomReservation newReservation = new RoomReservation();
        sourceReservation.copyTo(newReservation, true);

        newReservation.setStartDate(startDate);
        newReservation.setEndDate(startDate); 
        newReservation.setReservationName(reservationName);
        // when copying the new reservation is always a single regular reservation
        newReservation.setReservationType("regular");
        newReservation.setRecurringRule("");
        newReservation.setParentId(null);
        newReservation.setUniqueId(null);
        
        String timeZoneId = null;

        // copy room allocations
        for (RoomAllocation roomAllocation : sourceReservation.getRoomAllocations()) {
            // get the room arrangement
            final RoomArrangement roomArrangement = roomAllocation.getRoomArrangement();
            newReservation.addRoomAllocation(new RoomAllocation(roomArrangement, newReservation));

            // determine the time zone of the building and set the local time zone
            final String buildingId = roomAllocation.getBlId();
            timeZoneId = TimeZoneConverter.getTimeZoneIdForBuilding(buildingId);
        }

        // copy resource allocations
        ReservationServiceHelper.copyResourceAllocations(sourceReservation, newReservation);

        // availability will be checked
        this.reservationService.saveReservation(newReservation);
        // set the time zone to match the building
        newReservation.setTimeZone(timeZoneId);
        // for a new reservation create the appointment
        try {
            this.calendarService.createAppointment(newReservation);
        } catch (CalendarException exception) {
            final String localizedMessage =
                    ReservationsContextHelper.localizeString(CALENDAR_COPY_ERROR,
                        RoomReservationService.class);
            logger.warn(localizedMessage, exception);
            ReservationsContextHelper.appendResultError(localizedMessage + SPACE
                    + exception.getPattern());
        }

        // Set the unique id of the new reservation.
        final RoomReservation storedReservation =
                this.roomReservationDataSource.get(newReservation.getReserveId());
        storedReservation.setUniqueId(newReservation.getUniqueId());
        this.roomReservationDataSource.update(storedReservation);

        ReservationsContextHelper.ensureResultMessageIsSet();
    }

    /**
     * Get the attendees response status for the reservation with given id.
     * 
     * @param reservationId the reservation id
     * @return response status array
     */
    public List<JSONObject> getAttendeesResponseStatus(final int reservationId) {
        final List<JSONObject> results = new ArrayList<JSONObject>();

        final RoomReservation reservation =
                this.roomReservationDataSource.get(reservationId);

        try {
            final List<AttendeeResponseStatus> responses =
                    this.calendarService.getAttendeesResponseStatus(reservation);
            for (final AttendeeResponseStatus response : responses) {
                final JSONObject result = new JSONObject();
                result.put("name", response.getName());
                result.put("email", response.getEmail());
                result.put("response", response.getResponseStatus().toString());
                results.add(result);
            }
        } catch (CalendarException exception) {
            logger.error("Error retrieving attendee response status", exception);
            ReservationsContextHelper.appendResultError(exception.getPattern());
        }
        return results;
    }
    
    /**
     * Get the current local date/time for the given buildings.
     * 
     * @param buildingIds list of building IDs
     * @return JSON mapping of each building id to the current date/time in that building
     */
    public JSONObject getCurrentLocalDateTime(final List<String> buildingIds) {
        final Set<String> uniqueBuildingIds = new HashSet<String>(buildingIds);
        final JSONObject localDateTimes = new JSONObject();
        
        for (final String buildingId : uniqueBuildingIds) {
            final JSONObject buildingDateTime = new JSONObject();
            buildingDateTime.put("date",
                LocalDateTimeUtil.currentLocalDate(null, null, null, buildingId).toString());
            buildingDateTime.put("time",
                LocalDateTimeUtil.currentLocalTime(null, null, null, buildingId).toString());
            
            localDateTimes.put(buildingId, buildingDateTime);
        }
        
        return localDateTimes;
    }

    /**
     * Setter for the reservationService property.
     * 
     * @param reservationService the reservationService to set
     * @see reservationService
     */

    public void setReservationService(final IReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Setter for the cancelReservationService property.
     *
     * @param cancelReservationService the cancelReservationService to set
     * @see cancelReservationService
     */
    public void setCancelReservationService(
            final CancelReservationService cancelReservationService) {
        this.cancelReservationService = cancelReservationService;
    }

    /**
     * Gets the original reservations.
     *
     * @param roomReservation the room reservation
     * @return the original reservations
     */
    private List<RoomReservation> getOriginalReservations(
            final RoomReservation roomReservation) {
        // If this is an update in a recurrence series, get the original before updating.
        List<RoomReservation> originalReservations = null;
        if (roomReservation.getParentId() != null) {
            if (roomReservation.getStartDate().equals(roomReservation.getEndDate())) {
                // for edit single occurrence, get original date from database
                originalReservations = new ArrayList<RoomReservation>();
                originalReservations.add(this.roomReservationDataSource.getActiveReservation(
                    roomReservation.getReserveId(), null));
            } else {
                originalReservations =
                        this.roomReservationDataSource.getByParentId(roomReservation.getParentId(),
                            null, roomReservation.getStartDate(), roomReservation.getEndDate());
            }
        }
        return originalReservations;
    } 

    /**
     * Save the reservation as a calendar event using the Calendar Service.
     * 
     * @param reservation the DataRecord representing the reservation
     * @param roomReservation the object representing the reservation
     * @param originalReservations the original reservations
     */
    private void saveCalendarEvent(final DataRecord reservation,
            final RoomReservation roomReservation, final List<RoomReservation> originalReservations) {
        // check new using the incoming reservation data record
        if (reservation.isNew() || reservation.getInt(RESERVE_RES_ID) == 0) {
            roomReservation.setUniqueId(null);
            // for a new reservation create the appointment
            try {
                this.calendarService.createAppointment(roomReservation);
            } catch (CalendarException exception) {
                // Do not block the workflow, only report the error.
                final String localizedMessage =
                        ReservationsContextHelper.localizeString(CALENDAR_CREATE_ERROR,
                            RoomReservationService.class);
                logger.warn(localizedMessage, exception);
                ReservationsContextHelper.appendResultError(localizedMessage + SPACE
                        + exception.getPattern());
            }
        } else if (roomReservation.getParentId() == null) {
            // Update a regular reservation.
            try {
                this.calendarService.updateAppointment(roomReservation);
            } catch (CalendarException exception) {
                // Do not block the workflow, only report the error.
                final String localizedMessage =
                        ReservationsContextHelper.localizeString(CALENDAR_UPDATE_ERROR,
                            RoomReservationService.class, roomReservation.getReserveId());
                logger.warn(localizedMessage, exception);
                ReservationsContextHelper.appendResultError(localizedMessage + SPACE
                        + exception.getPattern());
            }
        } else {
            // Update a recurring reservation.
            final List<RoomReservation> createdReservations =
                    roomReservation.getCreatedReservations();
            for (int index = 0; index < originalReservations.size(); ++index) {
                final RoomReservation createdReservation = createdReservations.get(index);
                createdReservation.setTimeZone(roomReservation.getTimeZone());
                try {
                    this.calendarService.updateAppointmentOccurrence(createdReservation,
                        originalReservations.get(index));
                } catch (CalendarException exception) {
                    // Do not block the workflow, only report the error.
                    final String localizedMessage =
                            ReservationsContextHelper.localizeString(CALENDAR_UPDATE_ERROR,
                                RoomReservationService.class, createdReservation.getReserveId());
                    logger.warn(localizedMessage, exception);
                    ReservationsContextHelper.appendResultError(localizedMessage + SPACE
                            + exception.getPattern());
                }
            }
        }
    }
    
    
    /**
     * Cancel a single calendar event.
     * 
     * @param roomReservation the room reservation linked to the calendar event
     * @param comments the comments to send with the cancellation
     */
    private void cancelCalendarEvent(final IReservation roomReservation, final String comments) {
        final Integer parentId = roomReservation.getParentId();
        try {
            if (parentId != null && parentId > 0) {
                // Cancel a single occurrence.
                this.calendarService.cancelAppointmentOccurrence(roomReservation, comments);
            } else {
                this.calendarService.cancelAppointment(roomReservation, comments);
            }
        } catch (CalendarException exception) {
            final String localizedMessage =
                    ReservationsContextHelper.localizeString(CALENDAR_CANCEL_ERROR,
                        RoomReservationService.class, roomReservation.getReserveId());
            logger.warn(localizedMessage, exception);
            ReservationsContextHelper.appendResultError(localizedMessage + SPACE
                    + exception.getPattern());
        }
    }

}
