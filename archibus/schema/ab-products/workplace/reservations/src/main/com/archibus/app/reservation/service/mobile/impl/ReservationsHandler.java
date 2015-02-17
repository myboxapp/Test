package com.archibus.app.reservation.service.mobile.impl;

import static com.archibus.app.reservation.service.mobile.impl.Constants.*;

import java.util.*;

import com.archibus.app.reservation.dao.datasource.RoomArrangementDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.service.RoomReservationService;
import com.archibus.context.ContextStore;
import com.archibus.datasource.data.DataRecord;

/**
 * Handles the Reservations within the Workplace Services Portal mobile app.
 * 
 * @author Cristina Reghina
 * @since 21.2
 * 
 */
public class ReservationsHandler {
    
    /**
     * Constructor.
     */
    ReservationsHandler() {
        // Auto-generated constructor stub
    }
    
    /**
     * Searchs for available reservation rooms.
     * 
     * @param userName User Name
     * @param requestParameters parameters of the request
     * @return a list of room arrangements
     * 
     */
    public List<Map<String, String>> searchAvailableReservationRooms(final String userName,
            final Map<String, String> requestParameters) {
        
        final List<RoomArrangement> rooms = findAvailableRooms(requestParameters);
        
        return createAvailableRoomsResult(rooms, requestParameters);
    }
    
    /**
     * Finds available rooms.
     * 
     * @param requestParameters request parameters
     * @return list of room arrangements
     */
    protected List<RoomArrangement> findAvailableRooms(final Map<String, String> requestParameters) {
        final int capacity = Integer.valueOf(requestParameters.get(CAPACITY));
        final String rmArrangeTypeId = requestParameters.get(RM_ARRANGE_TYPE_ID);
        final String blId = requestParameters.get(BL_ID);
        final String flId = requestParameters.get(FL_ID);
        final String rmId = requestParameters.get(RM_ID);
        
        final DateAndTimeUtilities util = new DateAndTimeUtilities();
        final TimePeriod timePeriod = util.createTimePeriod(requestParameters);
        
        final RoomArrangementDataSource roomArrangementDataSource =
                (RoomArrangementDataSource) ContextStore.get().getBean("roomArrangementDataSource");
        List<RoomArrangement> rooms = null;
        rooms =
                roomArrangementDataSource.findAvailableRooms(blId, flId, rmId, rmArrangeTypeId,
                    timePeriod, capacity, null);
        
        return rooms;
    }
    
    /**
     * Reserves the room.
     * 
     * @param userName User Name
     * @param requestParameters parameters of the reservation request
     */
    public void reserveRoom(final String userName, final Map<String, String> requestParameters) {
        final DataRecord reservation =
                DataSourceUtilities.createRoomReservation(requestParameters);
        final DataRecord roomAllocation =
                DataSourceUtilities.createRoomAllocation(requestParameters);
        
        final RoomReservationService roomReservationService =
                (RoomReservationService) ContextStore.get().getBean(ROOM_RESERVATION_SERVICE);
        roomReservationService.saveRoomReservation(reservation, roomAllocation, null, null);
    }
    
    /**
     * Reserves the room.
     * 
     * @param userName User Name
     * @param requestParameters the reservation id
     */
    public void cancelRoomReservation(final String userName,
            final Map<String, String> requestParameters) {
        final Integer reservationId = Integer.valueOf(requestParameters.get(RES_ID));
        final RoomReservationService roomReservationService =
                (RoomReservationService) ContextStore.get().getBean(ROOM_RESERVATION_SERVICE);
        roomReservationService.cancelRoomReservation(reservationId, "");
    }
    
    /**
     * Creates the result list from the rooms list.
     * 
     * @param roomArrangements Rooms arrangements
     * @param requestParameters parameters of the reservation request
     * @return the room arrangements to be returned by the WFR
     */
    protected List<Map<String, String>> createAvailableRoomsResult(
            final List<RoomArrangement> roomArrangements,
            final Map<String, String> requestParameters) {
        final List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        
        for (final RoomArrangement roomArrangement : roomArrangements) {
            final Map<String, String> resultRoom = new HashMap<String, String>();
            // roomArrangement does not contain the date
            resultRoom.put(DAY_START, requestParameters.get(DAY_START));
            // roomArrangement has total availability times for the room
            resultRoom.put(TIME_START, requestParameters.get(TIME_START));
            resultRoom.put(TIME_END, requestParameters.get(TIME_END));
            resultRoom.put(BL_ID, roomArrangement.getBlId());
            resultRoom.put(FL_ID, roomArrangement.getFlId());
            resultRoom.put(RM_ID, roomArrangement.getRmId());
            resultRoom.put(CONFIG_ID, roomArrangement.getConfigId());
            resultRoom.put(RM_ARRANGE_TYPE_ID, roomArrangement.getArrangeTypeId());
            
            result.add(resultRoom);
        }
        
        return result;
    }
    
}
