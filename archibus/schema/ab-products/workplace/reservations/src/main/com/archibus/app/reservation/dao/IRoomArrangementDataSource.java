package com.archibus.app.reservation.dao;

import java.util.List;

import com.archibus.app.reservation.domain.*;
import com.archibus.core.dao.IDao;
import com.archibus.datasource.data.DataRecord;

/**
 * The Interface IRoomArrangementDataSource.
 */
public interface IRoomArrangementDataSource extends IDao<RoomArrangement> {
    
    /**
     * Find available room records.
     * 
     * @param receivedReservation the reservation
     * @param numberAttendees the number attendees
     * @param externalAllowed whether to return only rooms where external guests are allowed
     * @param fixedResourceStandards the fixed resource standards
     * @param allDayEvent true for all day events, false for regular reservations
     * @return the list
     * @throws ReservationException the reservation exception
     */
    List<DataRecord> findAvailableRoomRecords(final RoomReservation receivedReservation,
            final Integer numberAttendees, final boolean externalAllowed,
            final List<String> fixedResourceStandards, final boolean allDayEvent)
            throws ReservationException;
    
    /**
     * Find available rooms.
     * 
     * @param reservation the reservation
     * @param numberAttendees the number attendees
     * @param externalAllowed whether to return only rooms suitable for external guests
     * @param fixedResourceStandards the fixed resource standards
     * @param allDayEvent true for all day events, false for regular reservations
     * @param timeZone time zone to convert to
     * @return the list
     * @throws ReservationException the reservation exception
     */
    List<RoomArrangement> findAvailableRooms(final RoomReservation reservation,
            final Integer numberAttendees, final boolean externalAllowed,
            final List<String> fixedResourceStandards, final boolean allDayEvent,
            final String timeZone) throws ReservationException;
    
    /**
     * Find available rooms.
     * 
     * @param blId the bl id
     * @param flId the fl id
     * @param rmId the rm id
     * @param arrangeTypeId the arrange type id
     * @param timePeriod the time period within which the rooms must be available
     * @param numberAttendees the number attendees
     * @param fixedResourceStandards the fixed resource standards
     * @return the list
     */
    List<RoomArrangement> findAvailableRooms(final String blId, final String flId,
            final String rmId, final String arrangeTypeId, final TimePeriod timePeriod,
            final Integer numberAttendees, final List<String> fixedResourceStandards);
    
    /**
     * Gets the room arrangement.
     * 
     * @param blId the bl id
     * @param flId the fl id
     * @param rmId the rm id
     * @param configId the config id
     * @param arrangeTypeId the arrange type id
     * @return the room arrangement
     */
    RoomArrangement get(final String blId, final String flId, final String rmId,
            final String configId, final String arrangeTypeId);
    
}
