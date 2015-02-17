package com.archibus.app.reservation.service;

import com.archibus.app.reservation.dao.datasource.RoomAllocationDataSource;
import com.archibus.app.reservation.dao.datasource.RoomReservationDataSource;


/**
 * The Class RoomReservationServiceBase.
 */
public class RoomReservationServiceBase {
    
    /** The room reservation data source. */
    protected RoomReservationDataSource roomReservationDataSource;
    
    /** The room allocation data source. */
    protected RoomAllocationDataSource roomAllocationDataSource;
    
    /** The Calendar Service. */
    protected ICalendarService calendarService;
    
    /**
     * Sets the room reservation data source.
     *
     * @param roomReservationDataSource the new room reservation data source
     */
    public void setRoomReservationDataSource(
            final RoomReservationDataSource roomReservationDataSource) {
        this.roomReservationDataSource = roomReservationDataSource;
    }

    /**
     * Sets the room allocation data source.
     *
     * @param roomAllocationDataSource the new room allocation data source
     */
    public void setRoomAllocationDataSource(
            final RoomAllocationDataSource roomAllocationDataSource) {
        this.roomAllocationDataSource = roomAllocationDataSource;
    }
    
    /**
     * Sets the calendar service.
     * 
     * @param calendarService the new calendar service
     */
    public void setCalendarService(final ICalendarService calendarService) {
        this.calendarService = calendarService;
    } 

}
