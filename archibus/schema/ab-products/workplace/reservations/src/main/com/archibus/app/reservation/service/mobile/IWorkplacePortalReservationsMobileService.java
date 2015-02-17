package com.archibus.app.reservation.service.mobile;

import java.util.*;

/**
 * API of the Workplace Portal Reservations Workflow Rule Service for mobile Workplace Services
 * Portal application.
 * <p>
 * Registered in the ARCHIBUS Workflow Rules table as
 * 'AbWorkplacePortal-WorkplacePortalReservationsMobileService'.
 * <p>
 * Provides methods for find, reserve and cancel rooms
 * <p>
 * Invoked by mobile client.
 * 
 * @author Cristina Moldovan
 * @since 21.2
 */
public interface IWorkplacePortalReservationsMobileService {
    /**
     * Searches available reservation rooms.
     * 
     * @param userName User Name
     * @param requestParameters parameters of the request
     * @return the list of room arrangements
     */
    List<Map<String, String>> searchAvailableReservationRooms(final String userName,
            Map<String, String> requestParameters);
    
    /**
     * Reserves the room.
     * 
     * @param userName User Name
     * @param requestParameters parameters of the reservation request
     */
    void reserveRoom(final String userName, Map<String, String> requestParameters);
    
    /**
     * Cancel room reservation.
     * 
     * @param userName User Name
     * @param requestParameters the reservation id
     */
    void cancelRoomReservation(final String userName, Map<String, String> requestParameters);
}
