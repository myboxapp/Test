package com.archibus.app.reservation.service.mobile.impl;

import java.util.*;

import com.archibus.app.reservation.service.mobile.IWorkplacePortalReservationsMobileService;

/**
 * Implementation of the WorkplacePortal Reservations Workflow Rule Service for Workplace Services
 * Portal mobile application.
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
 * 
 */
public class WorkplacePortalReservationsMobileService implements
        IWorkplacePortalReservationsMobileService {
    
    /**
     * {@inheritDoc}.
     * 
     */
    public List<Map<String, String>> searchAvailableReservationRooms(final String userName,
            final Map<String, String> requestParameters) {
        
        final ReservationsHandler reservationsUpdate = new ReservationsHandler();
        final List<Map<String, String>> result =
                reservationsUpdate.searchAvailableReservationRooms(userName, requestParameters);
        
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    public void reserveRoom(final String userName, final Map<String, String> requestParameters) {
        final ReservationsHandler reservationsUpdate = new ReservationsHandler();
        
        reservationsUpdate.reserveRoom(userName, requestParameters);
    }
    
    /**
     * {@inheritDoc}
     */
    public void cancelRoomReservation(final String userName,
            final Map<String, String> requestParameters) {
        final ReservationsHandler reservationsUpdate = new ReservationsHandler();
        
        reservationsUpdate.cancelRoomReservation(userName, requestParameters);
    }
    
}
