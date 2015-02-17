package com.archibus.app.reservation.dao.datasource;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.util.TimeZoneConverter;
import com.archibus.context.ContextStore;
import com.archibus.datasource.data.DataRecord;

/**
 * Test class for RoomReservationDataSource.
 */
public class RoomReservationDataSourceTest extends ReservationDataSourceTestBase {
    
    /** Requestor time zone for testing time zone conversion. */
    private static final String REQUESTOR_TIMEZONE = "Europe/Brussels";
    
    /** ID of the resource to allocate. */
    private static final String RESOURCE_ID = "LCD-PROJECTOR1";
    
    /**
     * Test getting all room reservations.
     */
    public void testGetRoomReservations() {
        Assert.assertNotNull(ContextStore.get().getDatabase());
        
        final List<DataRecord> records = this.roomReservationDataSource.getAllRecords();
        
        Assert.assertNotNull(records);
        
        // Assert.assertFalse(records.isEmpty());
        
        final List<RoomReservation> reservations = this.roomReservationDataSource.find(null);
        
        Assert.assertNotNull(reservations);
        
        // Assert.assertFalse(reservations.isEmpty());
        
        Assert.assertEquals("All records are converted to objects.", records.size(),
            reservations.size());
    }
    
    /**
     * Test saving a room reservation.
     */
    public void testSaveRoomReservation() {
        try {
            final RoomReservation roomReservation = insertRoomReservation();
            
            // update room reservation
            this.roomReservationDataSource.save(roomReservation);
            
            Assert.assertNotNull(this.existingReservation);
            Assert.assertNotNull(this.existingReservation.getRoomAllocations());
            
        } catch (final ReservationException e) {
            Assert.fail(e.toString());
        }
    }
    
    /**
     * Test whether the unique id is removed when saved with null value.
     */
    public void testRemoveUniqueId() {
        final RoomReservation roomReservation = insertRoomReservation();
        Assert.assertEquals(UNIQUE_ID, roomReservation.getUniqueId());
        roomReservation.setUniqueId(null);
        roomReservation.setComments(RESOURCE_ID);
        this.roomReservationDataSource.save(roomReservation);
        
        final RoomReservation updatedReservation =
                this.roomReservationDataSource.get(roomReservation.getReserveId());
        Assert.assertEquals(UNIQUE_ID, updatedReservation.getUniqueId());
        Assert.assertEquals(RESOURCE_ID, updatedReservation.getComments());
    }

    /**
     * Insert a room reservation into the database.
     * 
     * @return the room reservation that has been inserted
     */
    private RoomReservation insertRoomReservation() {
        // insert room reservation
        this.roomReservationDataSource.save(this.existingReservation);
        
        Assert.assertNotNull(this.existingReservation);
        
        Assert.assertNotNull(this.existingReservation.getRoomAllocations());
        
        return this.roomReservationDataSource.get(this.existingReservation.getReserveId());
    }
    
    /**
     * Test saving a room reservation with resources.
     */
    public void testSaveRoomReservationWithResources() {
        try {
            
            final Resource resource = this.resourceDataSource.get(RESOURCE_ID);
            
            final ResourceAllocation resourceAllocation =
                    new ResourceAllocation(resource, this.existingReservation, 1);
            
            // the resource allocation should be in a room
            resourceAllocation.setBlId(BL_ID);
            resourceAllocation.setFlId(FL_ID);
            resourceAllocation.setRmId(RM_ID);
            
            this.existingReservation.addResourceAllocation(resourceAllocation);
            
            // update room reservation
            this.roomReservationDataSource.save(this.existingReservation);
            
            Assert.assertNotNull(this.existingReservation);
            Assert.assertNotNull(this.existingReservation.getRoomAllocations());
            
        } catch (final ReservationException e) {
            Assert.fail(e.toString());
        }
    }
    
    /**
     * Test getting a room reservation.
     */
    public void testGetRoomReservation() {
        try {
            RoomReservation roomReservation =
                    this.roomReservationDataSource.get(this.existingReservation.getReserveId());
            
            assertEquals("Same start date", this.existingReservation.getStartDate(),
                roomReservation.getStartDate());
            assertEquals("Same end date", this.existingReservation.getEndDate(),
                roomReservation.getEndDate());
            assertEquals("Same start time", this.existingReservation.getStartTime().toString(),
                roomReservation.getStartTime().toString());
            assertEquals("Same end time", this.existingReservation.getEndTime().toString(),
                roomReservation.getEndTime().toString());
            assertEquals("Same reserve id", this.existingReservation.getReserveId(),
                roomReservation.getReserveId());
            assertEquals("Same unique id", this.existingReservation.getUniqueId(),
                roomReservation.getUniqueId());
            assertEquals("Same room allocation count", this.existingReservation
                .getRoomAllocations().size(), roomReservation.getRoomAllocations().size());
            assertEquals("Same resource allocation count", this.existingReservation
                .getResourceAllocations().size(), roomReservation.getResourceAllocations().size());
            
            roomReservation =
                    this.roomReservationDataSource.getActiveReservation(
                        this.existingReservation.getReserveId(),
                        RoomReservationDataSourceTest.REQUESTOR_TIMEZONE);
            
            final List<RoomReservation> temporaryList = new ArrayList<RoomReservation>();
            temporaryList.add(this.existingReservation);
            TimeZoneConverter.toRequestorTimeZone(temporaryList,
                RoomReservationDataSourceTest.REQUESTOR_TIMEZONE);
            
            assertEquals("Same start date in requestor time zone",
                this.existingReservation.getStartDate(), roomReservation.getStartDate());
            assertEquals("Same end date in requestor time zone",
                this.existingReservation.getEndDate(), roomReservation.getEndDate());
            assertEquals("Same start time in requestor time zone", this.existingReservation
                .getStartTime().toString(), roomReservation.getStartTime().toString());
            assertEquals("Same end time in requestor time zone", this.existingReservation
                .getEndTime().toString(), roomReservation.getEndTime().toString());
            assertEquals("Same reserve id with time zone conversion",
                this.existingReservation.getReserveId(), roomReservation.getReserveId());
            assertEquals("Same unique id with time zone conversion",
                this.existingReservation.getUniqueId(), roomReservation.getUniqueId());
        } catch (final ReservationException e) {
            Assert.fail(e.toString());
        }
    }
    
    /**
     * Test cancelling a reservation.
     */
    public void testCancelReservation() {
        try {
            this.roomReservationDataSource.save(this.existingReservation);
            RoomReservation roomReservation =
                    this.roomReservationDataSource.get(this.existingReservation.getReserveId());
            
            this.roomReservationDataSource.cancel(roomReservation);
            
            roomReservation =
                    this.roomReservationDataSource.get(this.existingReservation.getReserveId());
            
            Assert.assertEquals(Constants.STATUS_CANCELLED, roomReservation.getStatus());
            Assert.assertEquals(this.existingReservation.getReserveId(),
                roomReservation.getReserveId());
            
            for (final RoomAllocation roomAllocation : roomReservation.getRoomAllocations()) {
                Assert.assertEquals(Constants.STATUS_CANCELLED, roomAllocation.getStatus());
            }
            
            for (final ResourceAllocation resourceAllocation : roomReservation
                .getResourceAllocations()) {
                Assert.assertEquals(Constants.STATUS_CANCELLED, resourceAllocation.getStatus());
            }
            
        } catch (final ReservationException e) {
            Assert.fail(e.toString());
        }
    }
    
    /**
     * Test cancelling a reservation that is specified in UTC time (KB 3037585).
     */
    public void testCancelReservationInTimeZone() {
        try {
            // Get the start and end in the time zone of the building.
            RoomReservation roomReservation =
                    this.roomReservationDataSource.get(this.existingReservation.getReserveId());
            final Date startDateTimeBuildingTimeZone = roomReservation.getStartDateTime();
            final Date endDateTimeBuildingTimeZone = roomReservation.getEndDateTime();
            
            // Get the reservation in a different time zone to use for canceling.
            roomReservation =
                    this.roomReservationDataSource.getActiveReservation(
                        this.existingReservation.getReserveId(), Constants.TIMEZONE_UTC);
            this.roomReservationDataSource.cancel(roomReservation);
            
            // Get the cancelled reservation (without time conversion, i.e. building time zone).
            roomReservation =
                    this.roomReservationDataSource.get(this.existingReservation.getReserveId());
            
            Assert.assertEquals(Constants.STATUS_CANCELLED, roomReservation.getStatus());
            Assert.assertEquals(this.existingReservation.getReserveId(),
                roomReservation.getReserveId());
            
            // Check the date/time in the building time zone.
            Assert.assertNull(roomReservation.getTimeZone());
            Assert.assertEquals(startDateTimeBuildingTimeZone, roomReservation.getStartDateTime());
            Assert.assertEquals(endDateTimeBuildingTimeZone, roomReservation.getEndDateTime());
            
            for (final RoomAllocation roomAllocation : roomReservation.getRoomAllocations()) {
                Assert.assertEquals(Constants.STATUS_CANCELLED, roomAllocation.getStatus());
                Assert.assertNull(roomAllocation.getTimeZone());
                Assert.assertEquals(startDateTimeBuildingTimeZone,
                    roomAllocation.getStartDateTime());
            }
            
            for (final ResourceAllocation resourceAllocation : roomReservation
                .getResourceAllocations()) {
                Assert.assertEquals(Constants.STATUS_CANCELLED, resourceAllocation.getStatus());
                Assert.assertNull(resourceAllocation.getTimeZone());
                Assert.assertEquals(startDateTimeBuildingTimeZone,
                    resourceAllocation.getStartDateTime());
            }
            
        } catch (final ReservationException e) {
            Assert.fail(e.toString());
        }
    }
    
}
