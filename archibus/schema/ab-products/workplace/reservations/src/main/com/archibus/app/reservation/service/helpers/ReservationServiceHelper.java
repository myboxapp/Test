package com.archibus.app.reservation.service.helpers;

import java.util.*;

import com.archibus.app.reservation.dao.*; 
import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.Recurrence;
import com.archibus.utility.StringUtil;

/**
 * The Class ReservationServiceHelper.
 */
public final class ReservationServiceHelper {
    
    /** Error message when a room is not available. */
    // @translatable
    private static final String ROOM_NOT_AVAILABLE = "The room is not available.";
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private ReservationServiceHelper() {
    }
    
    /**
     * Mark the given reservation as recurring.
     * 
     * @param roomReservationDataSource the room reservation data source
     * @param reservation the room reservation to mark as recurring
     * @param existingReservations other reservations in the recurrence series sorted by start date
     */
    public static void markRecurring(final IRoomReservationDataSource roomReservationDataSource,
            final RoomReservation reservation,
            final List<RoomReservation> existingReservations) {
        Integer parentId = null;
        // Find the active reservation with the earliest start date.
        // Start by assuming it's the new one.
        Date startDate = reservation.getStartDate();
        parentId = reservation.getReserveId();
        for (RoomReservation existingReservation : existingReservations) {
            if (existingReservation.getStartDate().before(startDate)) {
                startDate = existingReservation.getStartDate();
                parentId = existingReservation.getParentId();
            }
        }

        roomReservationDataSource.markRecurring(reservation, parentId, reservation
                .getRecurrence().toString());

        // Update the parent id in the other reservations.
        for (RoomReservation existingReservation : existingReservations) {
            roomReservationDataSource.markRecurring(existingReservation, parentId,
                    existingReservation.getRecurringRule());
        }
    } 


    /**
     * Update existing reservations for room reservation.
     *
     * @param roomReservationDataSource the room reservation data source
     * @param roomArrangementDataSource the room arrangement data source
     * @param savedReservations the saved reservations
     * @param roomReservation the room reservation
     * @param existingReservations the existing reservations
     */
    public static void updateExistingReservations(
            final IRoomReservationDataSource roomReservationDataSource,
            final IRoomArrangementDataSource roomArrangementDataSource,
            final List<RoomReservation> savedReservations,
            final RoomReservation roomReservation,
            final List<RoomReservation> existingReservations) {
        final RoomAllocation roomAllocation = roomReservation.getRoomAllocations().get(0);
        final RoomArrangement roomArrangement = roomAllocation.getRoomArrangement();

         // when editing we loop over existing reservations
        for (RoomReservation existingReservation : existingReservations) {
            if (Constants.STATUS_CANCELLED.equals(existingReservation.getStatus())
                    || Constants.STATUS_REJECTED.equals(existingReservation.getStatus())) {
                // go to the next and skip this one
                continue;                        
            }

            // only change attributes that are allowed when editing
            roomReservation.copyTo(existingReservation, false);
            final RoomAllocation existingRoomAllocation =
                    existingReservation.getRoomAllocations().get(0);
            existingRoomAllocation.setReservation(existingReservation);
            existingRoomAllocation.setRoomArrangement(roomArrangement);

            final List<RoomArrangement> roomArrangements =
                    roomArrangementDataSource.findAvailableRooms(existingReservation,
                            null, false, null, false, null);
            if (roomArrangements.isEmpty()) {
                throw new ReservableNotAvailableException(roomReservation.getRoomAllocations()
                    .get(0).getRoomArrangement(), ROOM_NOT_AVAILABLE,
                    ReservationServiceHelper.class);
            }

            copyResourceAllocations(roomReservation, existingReservation);
            // save all
            roomReservationDataSource.save(existingReservation);

            savedReservations.add(existingReservation);
        }
    }

    
    /**
     * Update existing reservations for resource reservation.
     *
     * @param resourceReservationDataSource the resource reservation data source
     * @param savedReservations the saved reservations
     * @param resourceReservation the resource reservation
     * @param existingReservations the existing reservations
     */
    public static void updateExistingReservations(
            final IResourceReservationDataSource resourceReservationDataSource, 
            final List<ResourceReservation> savedReservations,
            final ResourceReservation resourceReservation,
            final List<ResourceReservation> existingReservations) { 

         // when editing we loop over existing reservations
        for (ResourceReservation existingReservation : existingReservations) {
            if (Constants.STATUS_CANCELLED.equals(existingReservation.getStatus())
                    || Constants.STATUS_REJECTED.equals(existingReservation.getStatus())) {
                // go to the next and skip this one
                continue;                        
            }

            // only change attributes that are allowed when editing
            resourceReservation.copyTo(existingReservation, false);   
            copyResourceAllocations(resourceReservation, existingReservation);
            
            resourceReservationDataSource.checkResourcesAvailable(existingReservation);
            // save all
            resourceReservationDataSource.save(existingReservation);

            savedReservations.add(existingReservation);
        } // end for
    }
    
    /**
     * Check whether resource allocations are all within the time period of the main reservation.
     * 
     * @param reservation the reservation to check the resource allocations for
     */
    public static void checkResourceAllocations(final AbstractReservation reservation) {
        for (final ResourceAllocation resourceAllocation : reservation.getResourceAllocations()) {
            // check within room reservation time window
            if (resourceAllocation.getStartTime().before(reservation.getStartTime())
                    || resourceAllocation.getEndTime().after(reservation.getEndTime())) {
                // @translatable
                throw new ReservationException(
                    "Resource allocation exceeds the main reservation timeframe",
                    ReservationServiceHelper.class);
            }
        }
    }
    
    /**
     * Copy resource allocations from one reservation to another reservation. The date is adjusted. The time frame is
     * only modified if required, i.e. only if the original time frame is outside the target
     * reservation time frame. Existing allocations for the same resources in the target reservation
     * are modified, existing allocations for other resources are removed.
     * 
     * @param sourceReservation the source reservation
     * @param targetReservation the target reservation
     */
    public static void copyResourceAllocations(final AbstractReservation sourceReservation,
            final AbstractReservation targetReservation) {
        
        // create a hash map to check if the resources are in the target reservation
        final Map<String, ResourceAllocation> targetResources = new HashMap<String, ResourceAllocation>();
        
        // take only the ones that are not cancelled or rejected
        for (ResourceAllocation resourceAllocation : targetReservation
            .getActiveResourceAllocations()) {
            targetResources.put(resourceAllocation.getResourceId(), resourceAllocation);
        }
        
        // if the target reservation has the same resources, update and not insert
        for (final ResourceAllocation sourceAllocation : sourceReservation.getResourceAllocations()) {
            // Create a new resource allocation for the same resource and time period.
            ResourceAllocation targetAllocation = null;
            
            if (targetResources.containsKey(sourceAllocation.getResourceId())) {
                targetAllocation = targetResources.remove(sourceAllocation.getResourceId());
            } else {
                targetAllocation = new ResourceAllocation();
                targetReservation.addResourceAllocation(targetAllocation);
            }
            
            // Assign the new allocation to the recurring reservation. The date is
            // modified, the time frame stays the same.
            sourceAllocation.copyTo(targetAllocation);
        }
        
        // Remove all remaining allocations from the target reservation.
        for (final String resourceId : targetResources.keySet()) {
            targetReservation.removeResourceAllocation(targetResources.get(resourceId));
        }
    }
    
    /**
     * Check whether the given reservation record is a new recurring reservation or is meant to edit
     * a series of occurrences.
     * 
     * @param reservation the reservation to check
     * @return true if it's a new recurring reservation or an edit of multiple occurrences, false
     *         otherwise
     */
    public static boolean isNewRecurrence(final AbstractReservation reservation) {
        // Only create recurring reservations for a new reservation or when the start date is
        // not the same as the end date. Otherwise update the occurrence specified by the
        // reservation id.
        return Constants.TYPE_RECURRING.equalsIgnoreCase(reservation.getReservationType())
                && (reservation.getReserveId() == null || !reservation.getStartDate().equals(
                    reservation.getEndDate()));
    }
    
    /**
     * Parse the recurrence defined by the reservation object if required. Uses the start date and
     * end date of the reservation. After parsing, the end date is reset to equal the start date.
     * The recurrence pattern is stored in the reservation and returned.
     * 
     * @param reservation the reservation to parse the recurrence for
     * @return the recurrence pattern
     */
    public static Recurrence prepareNewRecurrence(final AbstractReservation reservation) {
        // We only need to parse the recurrence for a new recurring reservation.
        final Integer reservationId = reservation.getReserveId();
        Recurrence recurrence = null;
        if (reservationId == null) {
            recurrence =
                    Recurrence.parseRecurrence(reservation.getStartDate(),
                        reservation.getEndDate(), reservation.getRecurringRule());
            reservation.setRecurrence(recurrence);
        }
        // The end date in the reservation was used to specify the recurrence end date.
        // Change it back to the reservation date before saving the reservations.
        reservation.setEndDate(reservation.getStartDate());
        return recurrence;
    }
    
    /**
     * Check whether the reservation email and attendees are valid email addresses. This is a simple
     * check, it only checks that each email is of the format someone@something.
     * 
     * @param reservation the reservation to validate the emails for
     * @throws ReservationException when an invalid email is found
     */
    public static void validateEmails(final AbstractReservation reservation) throws ReservationException {
        if (!validateEmail(reservation.getEmail())) {
            // @translatable
            throw new ReservationException("Invalid requestor email: {0}",
                ReservationServiceHelper.class, reservation.getEmail());
        }
        if (StringUtil.notNullOrEmpty(reservation.getAttendees())) {
            for (String attendee : reservation.getAttendees().split(";")) {
                if (!validateEmail(attendee)) {
                    // @translatable
                    throw new ReservationException("Invalid attendee email: {0}",
                        ReservationServiceHelper.class, attendee);
                }
            }
        }
    }
    
    /**
     * Check that the email address contains an @ and it's not the first or last character.
     * 
     * @param emailToCheck the email to check
     * 
     * @return true if valid, false if invalid
     */
    private static boolean validateEmail(final String emailToCheck) {
        final int atIndex = emailToCheck.indexOf('@');
        return atIndex >= 1 && atIndex < emailToCheck.length() - 1;
    }
    
}
