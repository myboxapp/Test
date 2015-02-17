package com.archibus.app.reservation.service;

import junit.framework.Assert;
import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.dao.datasource.*;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.app.reservation.exchange.service.*;
import com.archibus.datasource.data.DataRecord;

/**
 * The Class RoomReservationServiceTestBase.
 *        <p>
 *        Suppress warning "PMD.TestClassWithoutTestCases".
 *        <p>
 *        Justification: this is a base class for other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class RoomReservationServiceTestBase extends AbstractReservationServiceTestBase {
    
    /** The reservation handler. */
    protected RoomReservationService roomReservationService;
    
    /** The room reservation data source. */
    protected RoomReservationDataSource roomReservationDataSource;
    
    /** The room allocation data source. */
    protected RoomAllocationDataSource roomAllocationDataSource;
    
    /** The resource allocation data source. */
    protected ResourceAllocationDataSource resourceAllocationDataSource;
    
    /** The Exchange Appointment helper. */
    protected AppointmentHelper appointmentHelper;
    
    /** The Exchange service helper. */
    protected ExchangeServiceHelper serviceHelper;
    
    /**
     * Check equivalence between an appointment and a reservation (regardless of recurrence).
     * 
     * @param roomReservation the reservation
     * @param appointment the appointment
     * @throws ServiceLocalException when an EWS error occurs
     */
    protected void checkEquivalence(final RoomReservation roomReservation,
            final Appointment appointment) throws ServiceLocalException {
        Assert.assertEquals(roomReservation.getStartDateTime(), appointment.getStart());
        Assert.assertEquals(roomReservation.getEndDateTime(), appointment.getEnd());
        Assert.assertEquals(roomReservation.getUniqueId(), appointment.getICalUid());
        Assert.assertEquals(roomReservation.getReservationName(), appointment.getSubject());
    }
    
    /**
     * Create a reservation.
     * 
     * @param recurrent true for a recurring reservation, false for a regular one
     * @return DataRecord containing the new reservation
     */
    protected DataRecord createAndSaveRoomReservation(final boolean recurrent) {
        final DataRecord reservation = this.roomReservationDataSource.createNewRecord();
        createReservation(reservation, recurrent);
        
        final DataRecord roomAllocation = createRoomAllocation();
        
        return this.roomReservationService.saveRoomReservation(reservation, roomAllocation, null,
            null);
    } 
    
    /**
     * Create a room allocation data record.
     * 
     * @return room allocation data record
     */
    protected DataRecord createRoomAllocation() {
        final DataRecord roomAllocation = this.roomAllocationDataSource.createNewRecord();
        roomAllocation.setValue("reserve_rm.date_start", this.startDate);
        roomAllocation.setValue("reserve_rm.time_start", this.startTime);
        roomAllocation.setValue("reserve_rm.time_end", this.endTime);
        
        roomAllocation.setValue("reserve_rm.bl_id", BL_ID);
        roomAllocation.setValue("reserve_rm.fl_id", FL_ID);
        roomAllocation.setValue("reserve_rm.rm_id", RM_ID);
        roomAllocation.setValue("reserve_rm.config_id", "A1");
        roomAllocation.setValue("reserve_rm.rm_arrange_type_id", "THEATER");
        return roomAllocation;
    }
    
    /**
     * Gets the room reservation service.
     * 
     * @return the room reservation service
     */
    public RoomReservationService getRoomReservationService() {
        return this.roomReservationService;
    }
    
    /**
     * Sets the room reservation service.
     * 
     * @param roomReservationService the new room reservation service
     */
    public void setRoomReservationService(final RoomReservationService roomReservationService) {
        this.roomReservationService = roomReservationService;
    }
    
    /**
     * Gets the room reservation data source.
     * 
     * @return the room reservation data source
     */
    public RoomReservationDataSource getRoomReservationDataSource() {
        return this.roomReservationDataSource;
    }
    
    /**
     * Set the room reservation data source for this test.
     * 
     * @param roomReservationDataSource the new room reservation data source
     */
    public void setRoomReservationDataSource(
            final RoomReservationDataSource roomReservationDataSource) {
        this.roomReservationDataSource = roomReservationDataSource;
    }
    
    /**
     * Gets the room allocation data source.
     * 
     * @return the room allocation data source
     */
    public RoomAllocationDataSource getRoomAllocationDataSource() {
        return this.roomAllocationDataSource;
    }
    
    /**
     * Sets the data source for room allocations for this service test.
     * @param roomAllocationDataSource the room allocation data source for this service test
     */
    public void setRoomAllocationDataSource(final RoomAllocationDataSource roomAllocationDataSource) {
        this.roomAllocationDataSource = roomAllocationDataSource;
    }
    
    /**
     * Sets the resource allocation data source.
     * 
     * @param resourceAllocationDataSource the new room allocation data source
     */
    public void setResourceAllocationDataSource(
            final ResourceAllocationDataSource resourceAllocationDataSource) {
        this.resourceAllocationDataSource = resourceAllocationDataSource;
    }
    
    /**
     * Set the Exchange service helper.
     * 
     * @param serviceHelper the new service helper
     */
    public void setServiceHelper(final ExchangeServiceHelper serviceHelper) {
        this.serviceHelper = serviceHelper;
    }
    
    /**
     * Sets the appointment helper.
     * 
     * @param appointmentHelper the new appointment helper
     */
    public void setAppointmentHelper(final AppointmentHelper appointmentHelper) {
        this.appointmentHelper = appointmentHelper;
    }
    
}