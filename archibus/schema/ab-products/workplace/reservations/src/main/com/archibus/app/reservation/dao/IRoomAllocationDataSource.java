package com.archibus.app.reservation.dao;

import java.util.*;

import com.archibus.app.reservation.domain.*;

/**
 * The Interface IRoomAllocationDataSource.
 */
public interface IRoomAllocationDataSource extends IAllocationDataSource<RoomAllocation> {

    /**
     * Get availabilities for the given room arrangements between the given dates.
     * 
     * @param roomArrangements the room arrangements to get the availabilities for
     * @param startDate beginning of the time period
     * @param endDate end of the time period
     * @return list of room availabilities
     */
    List<RoomAvailability> getRoomAvailabilities(final List<RoomArrangement> roomArrangements,
            final Date startDate, final Date endDate);
    
    /**
     * Gets the room allocations for a reservation.
     * 
     * @param reservation the reservation
     * @return the room allocations
     */
    List<RoomAllocation> getRoomAllocations(final IReservation reservation);
    
    /**
     * Gets the room allocations.
     * 
     * @param blId the bl id
     * @param flId the fl id
     * @param rmId the rm id
     * @param startDate the start date
     * @return the room allocations
     */
    List<RoomAllocation> getRoomAllocations(final String blId, final String flId,
            final String rmId, final Date startDate);
    
    /**
     * Gets the room allocations.
     * 
     * @param blId the bl id
     * @param flId the fl id
     * @param rmId the rm id
     * @param startDate the start date
     * @param endDate the end date
     * @return the room allocations
     */
    List<RoomAllocation> getRoomAllocations(final String blId, final String flId,
            final String rmId, final Date startDate, final Date endDate);
    
    /**
     * Get allocated rooms for a certain date.
     * 
     * @param startDate the start date
     * @param roomArrangement the room arrangement
     * @param reservationId the reservation id
     * @return list of allocated rooms
     */
    List<RoomAllocation> getAllocatedRooms(final Date startDate,
            final RoomArrangement roomArrangement, final Integer reservationId);
   
}
