package com.archibus.app.reservation.dao.datasource;

import java.util.*;

import com.archibus.app.reservation.dao.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.util.*;
import com.archibus.context.*;
import com.archibus.datasource.data.*;
import com.archibus.model.view.datasource.ParsedRestrictionDef;
import com.archibus.model.view.datasource.ClauseDef.Operation;
import com.archibus.utility.*;

/**
 * The Class RoomReservationDataSource.
 * 
 * @author Bart Vanderschoot
 */
public class RoomReservationDataSource extends AbstractReservationDataSource<RoomReservation>
implements IRoomReservationDataSource {
    
    /** Error message indicating a specific resource is not available for the reservation. */
    // @translatable
    private static final String RESOURCE_NOT_AVAILABLE =
            "The resource {0} is not available for this reservation";

    /** The room allocation data source. */
    protected IRoomAllocationDataSource roomAllocationDataSource;

    /** The room arrangement data source. */
    protected IRoomArrangementDataSource roomArrangementDataSource;

    /**
     * Instantiates a new room reservation data source.
     */
    public RoomReservationDataSource() {
        super("roomReservation", "reserve");
    }

    /**
     * {@inheritDoc}
     */
    public RoomReservation convertRecordToObject(final DataRecord reservationRecord,
            final DataRecord roomAllocationRecord, final List<DataRecord> resourceAllocationRecords) {
        final RoomReservation roomReservation =
                this.convertRecordToObject(reservationRecord, resourceAllocationRecords);

        // expecting only one room
        final RoomAllocation roomAllocation =
                this.roomAllocationDataSource.convertRecordToObject(roomAllocationRecord);
        roomReservation.addRoomAllocation(roomAllocation);
        return roomReservation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoomReservation get(final Object reserveId) {
        final RoomReservation reservation = super.get(reserveId);
        if (reservation != null) {
            final List<RoomAllocation> roomAllocations =
                    this.roomAllocationDataSource.getRoomAllocations(reservation);
            reservation.setRoomAllocations(roomAllocations);
        }
        return reservation;
    }

    /**
     * {@inheritDoc}
     */
    public RoomReservation getActiveReservation(final Object reserveId, final String timeZoneId) {
        final RoomReservation reservation = super.getActiveReservation(reserveId);
        if (reservation != null) {
            final List<RoomAllocation> roomAllocations =
                    this.roomAllocationDataSource.getRoomAllocations(reservation);

            reservation.setRoomAllocations(roomAllocations);
            if (StringUtil.notNullOrEmpty(timeZoneId)) {
                reservation.convertToTimeZone(timeZoneId);
            }
        }

        return reservation;
    }

    /**
     * {@inheritDoc}
     */
    public List<RoomReservation> getByUniqueId(final String uniqueId, final String timeZoneId)
            throws ReservationException {
        this.clearRestrictions();
        final ParsedRestrictionDef restriction = new ParsedRestrictionDef();
        restriction.addClause(this.tableName, Constants.UNIQUE_ID, uniqueId, Operation.EQUALS);
        final List<RoomReservation> reservations = this.findActiveReservations(restriction);
        return addRoomAllocations(reservations, timeZoneId);
    }
    
    /**
     * Clear the unique id.
     * 
     * @param reservation reservation
     * @return reservation
     * @throws ReservationException ReservationException
     */
    public final RoomReservation clearUniqueId(final RoomReservation reservation) throws ReservationException {
        // Get the unmodified reservation, so we do not change anything else (KB 3037586).
        final RoomReservation storedReservation = this.get(reservation.getReserveId());
        
        if (storedReservation.getUniqueId() != null) {
            final User user = ContextStore.get().getUser();
            // this will insert a NULL value
            storedReservation.setUniqueId("");
            storedReservation.setLastModifiedBy(user.getEmployee().getId());
            // TODO: current date or use timezone ?
            storedReservation.setLastModifiedDate(Utility.currentDate());
            // every reservation can be casted to AbstractReservation
            super.update(storedReservation);
        }
        return storedReservation;
    }

    /**
     * Add the room allocations to a list of reservations.
     * 
     * @param reservations the list of reservations to add room allocations to
     * @param timeZoneId the time zone id to convert to
     * @return list of reservations including their room allocations
     */
    private List<RoomReservation> addRoomAllocations(final List<RoomReservation> reservations,
            final String timeZoneId) {
        for (final RoomReservation reservation : reservations) {
            final List<RoomAllocation> roomAllocations =
                    this.roomAllocationDataSource.getRoomAllocations(reservation);

            reservation.setRoomAllocations(roomAllocations);
            if (timeZoneId != null) {
                reservation.convertToTimeZone(timeZoneId);
            }
        }
        return reservations;
    }

    /**
     * {@inheritDoc}
     */
    public List<RoomReservation> getByParentId(final Integer parentId, final String timeZoneId, final Date startDate, 
            final Date endDate) {
        List<RoomReservation> result = null;

        if (parentId != null) { 
            final List<RoomReservation> reservations =
                    super.getActiveReservationsByParentId(parentId, startDate, endDate);
            result = this.addRoomAllocations(reservations, timeZoneId); 

        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoomReservation save(final RoomReservation roomReservation) throws ReservationException {
        if (roomReservation.getRoomAllocations() != null) {
            // we assume there is only one room allocation
            for (final RoomAllocation roomAllocation : roomReservation.getRoomAllocations()) {
                // check time zone, if not provided, we assume the requestor is making the
                // reservation in local building time.
                if (roomReservation.getTimeZone() != null) {
                    Date startDateTime =
                            Utility.toDatetime(roomReservation.getStartDate(),
                                    roomReservation.getStartTime());
                    startDateTime =
                            TimeZoneConverter.calculateDateTimeForBuilding(
                                    roomAllocation.getBlId(), startDateTime,
                                    roomReservation.getTimeZone(), true);

                    Date endDateTime =
                            Utility.toDatetime(roomReservation.getEndDate(),
                                    roomReservation.getEndTime());
                    endDateTime =
                            TimeZoneConverter.calculateDateTimeForBuilding(
                                    roomAllocation.getBlId(), endDateTime,
                                    roomReservation.getTimeZone(), true);

                    final TimePeriod timePeriod = new TimePeriod();
                    timePeriod.setStartDateTime(startDateTime);
                    timePeriod.setEndDateTime(endDateTime);
                    roomReservation.setTimePeriod(timePeriod);

                    roomAllocation.setStartDateTime(startDateTime);
                    roomAllocation.setEndDateTime(endDateTime);
                }

                // also loop through all resource allocations to set the room reference
                // and to verify that the resource is available for that room at that time
                if (roomReservation.getResourceAllocations() != null) {
                    moveResourceAllocations(roomReservation, roomAllocation);
                }
            }
        }

        // check for status
        checkApprovalRequired(roomReservation);

        // calculate costs for all allocations and total before saving.
        calculateCosts(roomReservation);

        // save reservation and resources
        super.checkAndSave(roomReservation);

        if (roomReservation.getRoomAllocations() != null) {
            saveRoomAllocations(roomReservation);
        }

        return roomReservation;
    }

    /**
     * {@inheritDoc}
     */
    public double calculateCosts(final RoomReservation roomReservation) {

        for (final RoomAllocation allocation : roomReservation.getRoomAllocations()) {
            this.roomAllocationDataSource.calculateCost(allocation);
        }

        final List<ResourceAllocation> activeResourceAllocations =
                roomReservation.getActiveResourceAllocations();

        for (final ResourceAllocation allocation : activeResourceAllocations) {
            this.resourceAllocationDataSource.calculateCost(allocation);
        }

        roomReservation.calculateTotalCost();

        return roomReservation.getCost();
    }

    /**
     * {@inheritDoc}
     */
    public final void canBeCancelledByCurrentUser(final RoomReservation roomReservation)
            throws ReservationException {
        final User user = ContextStore.get().getUser();
        if (!user.isMemberOfGroup(Constants.RESERVATION_SERVICE_DESK)
                && !user.isMemberOfGroup(Constants.RESERVATION_MANAGER)) {
            checkCancelling(roomReservation);

            // Get the active resource allocations and check whether they can be cancelled.
            final List<ResourceAllocation> activeResourceAllocations =
                    roomReservation.getActiveResourceAllocations();
            for (final ResourceAllocation resourceAllocation : activeResourceAllocations) {
                this.resourceAllocationDataSource.checkCancelling(resourceAllocation);
            }

            // Check whether the connected room allocation can be cancelled.
            if (roomReservation.getRoomAllocations() != null) {
                for (final RoomAllocation roomAllocation : roomReservation.getRoomAllocations()) {
                    this.roomAllocationDataSource.checkCancelling(roomAllocation);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel(final RoomReservation roomReservation) throws ReservationException {
        // Get the unmodified reservation, so we do not change anything else (KB 3037585).
        final RoomReservation unmodifiedReservation = this.get(roomReservation.getReserveId());

        // First cancel the room allocation, this updates its cost as well.
        if (unmodifiedReservation.getRoomAllocations() != null) {
            for (final RoomAllocation roomAllocation : unmodifiedReservation.getRoomAllocations()) {
                this.roomAllocationDataSource.cancel(roomAllocation);
            }
        }
        // Then call the super method.
        super.cancel(unmodifiedReservation); 

    }

    /**
     * {@inheritDoc}
     */
    public final void setRoomAllocationDataSource(
            final RoomAllocationDataSource roomAllocationDataSource) {
        this.roomAllocationDataSource = roomAllocationDataSource;
    }

    /**
     * {@inheritDoc}
     */
    public final void setRoomArrangementDataSource(
            final IRoomArrangementDataSource roomArrangementDataSource) {
        this.roomArrangementDataSource = roomArrangementDataSource;
    }

    /**
     * Save a reservation's room allocations.
     * 
     * @param reservation the reservation
     * @throws ReservationException when the save failed
     */
    private void saveRoomAllocations(final RoomReservation reservation) throws ReservationException {
        final String attendees = reservation.getAttendees();
        int internalGuests = 0;
        int externalGuests = 0;
        if (attendees != null) {
            final String[] attendeeArr = attendees.split(";");
            for (final String attendeeEmail : attendeeArr) {
                // check for a valid email
                if (StringUtil.isNullOrEmpty(attendeeEmail)) {
                    continue;
                }
                if (DataSourceUtils.isEmployeeEmail(attendeeEmail)) {
                    ++internalGuests;
                } else {
                    ++externalGuests;
                }
            }
        }
        for (final RoomAllocation roomAllocation : reservation.getRoomAllocations()) {
            roomAllocation.setReservation(reservation);
            roomAllocation.setInternalGuests(internalGuests);
            roomAllocation.setExternalGuests(externalGuests);

            if (roomAllocation.getId() == null || roomAllocation.getId() == 0) {
                final RoomAllocation savedAllocation =
                        this.roomAllocationDataSource.save(roomAllocation);
                roomAllocation.setId(savedAllocation.getId());
            } else {
                this.roomAllocationDataSource.checkAndUpdate(roomAllocation);
            }
        }
    }

    /**
     * Move Resource Allocations according to their availability for the selected room. Resource
     * allocations that are not available for the new location are cancelled.
     * 
     * @param roomReservation roomReservation
     * @param roomAllocation roomAllocation
     */
    private void moveResourceAllocations(final RoomReservation roomReservation,
            final RoomAllocation roomAllocation) {
        for (final ResourceAllocation resourceAllocation : roomReservation
                .getActiveResourceAllocations()) {
            final Resource resource =
                    this.resourceDataSource.get(resourceAllocation.getResourceId());
            final RoomArrangement arrangement =
                    this.roomArrangementDataSource.get(roomAllocation.getBlId(),
                            roomAllocation.getFlId(), roomAllocation.getRmId(),
                            roomAllocation.getConfigId(), roomAllocation.getArrangeTypeId());

            // Update the resource allocation time period before checking availability.
            resourceAllocation.setReservation(roomReservation);

            // check if the resource is allowed in the new room and if it is not reserved for the
            // new reservation date
            final boolean allowed =
                    arrangement.allowsResourceStandard(resource.getResourceStandard())
                            && this.resourceDataSource.checkResourceAvailable(
                                resourceAllocation.getResourceId(), roomReservation,
                                resourceAllocation.getTimePeriod());

            if (!allowed) {
                throw new ReservableNotAvailableException(resource, RESOURCE_NOT_AVAILABLE,
                    RoomReservationDataSource.class, resourceAllocation.getResourceId());
            }

            this.resourceDataSource.checkQuantityAllowed(roomReservation.getReserveId(),
                resourceAllocation, resource);

            resourceAllocation.setBlId(roomAllocation.getBlId());
            resourceAllocation.setFlId(roomAllocation.getFlId());
            resourceAllocation.setRmId(roomAllocation.getRmId());
        } // end for
    }

    /**
     * Check Approval Required and set the status for the reservation and all of its allocations.
     * 
     * @param roomReservation room reservation, only with its active allocations (i.e. without
     *            cancelled and rejected ones)
     */
    protected void checkApprovalRequired(final RoomReservation roomReservation) {
        // if status is rejected or cancelled, don't change the status
        if (StringUtil.isNullOrEmpty(roomReservation.getStatus())
                || Constants.STATUS_AWAITING_APP.equals(roomReservation.getStatus())
                || Constants.STATUS_CONFIRMED.equals(roomReservation.getStatus())) {

            boolean approvalRequired = checkResourcesApprovalRequired(roomReservation);

            for (final RoomAllocation roomAllocation : roomReservation.getRoomAllocations()) {
                final RoomArrangement roomArrangement =
                        this.roomArrangementDataSource.get(roomAllocation.getBlId(),
                                roomAllocation.getFlId(), roomAllocation.getRmId(),
                                roomAllocation.getConfigId(), roomAllocation.getArrangeTypeId());
                if (roomArrangement.getApprovalRequired() == 1) {
                    approvalRequired = true;
                    roomAllocation.setStatus(Constants.STATUS_AWAITING_APP);
                } else {
                    roomAllocation.setStatus(Constants.STATUS_CONFIRMED);
                }
            }

            roomReservation.setStatus(approvalRequired ? Constants.STATUS_AWAITING_APP
                    : Constants.STATUS_CONFIRMED);
        }
    }

}
