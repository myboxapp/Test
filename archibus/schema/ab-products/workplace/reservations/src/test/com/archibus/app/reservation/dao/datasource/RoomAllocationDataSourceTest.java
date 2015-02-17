package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.RoomAllocation;
import com.archibus.app.reservation.domain.RoomArrangement;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.app.reservation.domain.TimePeriod;
import com.archibus.datasource.data.DataRecord;
import com.archibus.model.view.datasource.ParsedRestrictionDef;
import com.archibus.utility.Utility;

/**
 * Test class for RoomAllocationDataSource.
 */
public class RoomAllocationDataSourceTest extends ReservationDataSourceTestBase {
    
    /**
     * Test getting all room allocations within a time period.
     */
    public void testGetRoomAllocationsByDate() {
        Date startDate = this.existingReservation.getStartDate();
        
        List<RoomAllocation> roomAllocations =
                this.roomAllocationDataSource.getRoomAllocations(BL_ID, FL_ID, RM_ID, startDate);
        
        Assert.assertNotNull(roomAllocations);
        Assert.assertFalse(roomAllocations.isEmpty());
        
        final Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.MONTH, -1);
        startDate = cal.getTime();
        
        cal.add(Calendar.MONTH, 2);
        final Date endDate = cal.getTime();
        
        roomAllocations =
                this.roomAllocationDataSource.getRoomAllocations(BL_ID, FL_ID, RM_ID, startDate,
                    endDate);
        
        Assert.assertNotNull(roomAllocations);
        Assert.assertFalse(roomAllocations.isEmpty());
    }
    
    /**
     * Test getting room allocations linked to a reservation.
     */
    public void testGetRoomAllocationsByReservation() {
        // get all room allocations
        final List<DataRecord> roomAllocations = this.roomAllocationDataSource.getAllRecords();
        
        Assert.assertNotNull(roomAllocations);
        Assert.assertFalse(roomAllocations.isEmpty());
        
        List<RoomAllocation> rooms =
                this.roomAllocationDataSource.find((ParsedRestrictionDef) null);
        Assert.assertNotNull(rooms);
        Assert.assertFalse(rooms.isEmpty());
        
        final RoomReservation reservation =
                new RoomReservation(this.existingReservation.getReserveId());
        rooms = this.roomAllocationDataSource.find(reservation);
        Assert.assertNotNull(rooms);
        Assert.assertFalse(rooms.isEmpty());
        RoomAllocation alloc = rooms.get(0);
        Assert.assertEquals(this.existingReservation.getReserveId(), alloc.getReserveId());
        
        alloc = this.roomAllocationDataSource.get(alloc.getId());
        Assert.assertEquals(this.existingReservation.getReserveId(), alloc.getReserveId());
    }
    
    /**
     * Test saving and deleting modified room allocation.
     */
    public void testSaveRoomAllocation() {
        RoomAllocation roomAllocation =
                this.roomAllocationDataSource.find(this.existingReservation).get(0);
        Assert.assertEquals(ALLOCATION_COMMENTS, roomAllocation.getComments());
        
        roomAllocation.setComments(ALLOCATION_COMMENTS_CHANGED);
        this.roomAllocationDataSource.update(roomAllocation);
        
        roomAllocation = this.roomAllocationDataSource.find(this.existingReservation).get(0);
        Assert.assertEquals(ALLOCATION_COMMENTS_CHANGED, roomAllocation.getComments());
        
        this.roomAllocationDataSource.delete(roomAllocation);
        
        assertTrue("After delete, no more room allocation linked to the reservation.",
            this.roomAllocationDataSource.find(this.existingReservation).isEmpty());
    }
    
    /**
     * Test total cost calculation for standard.
     */
    public void testRoomAllocationCostReservation() {
        
        final RoomArrangement roomArrangement =
                this.roomArrangementDataSource.get(BL_ID, FL_ID, RM_ID, CONFIG_ID, ARRANGE_TYPE_ID);
        
        final RoomAllocation roomAllocation =
                this.roomAllocationDataSource.find(this.existingReservation).get(0);
        
        if (roomArrangement.getCostUnit() == Constants.COST_UNIT_RESERVATION) {
            Assert.assertEquals(roomArrangement.getCostPerUnit(), roomAllocation.getCost());
        }
        
    }
    
    /**
     * Test total cost calculation for unit per hours.
     * 
     * @throws ParseException ParseException
     */
    public void testRoomAllocationCostHours() throws ParseException {
        
        final RoomArrangement roomArrangement =
                this.roomArrangementDataSource.get(BL_ID, "18", "109", "AAA", "CLASSROOM");
        
        final Calendar cal = Calendar.getInstance();
        cal.setTime(Utility.currentDate());
        cal.add(Calendar.DATE, DAYS_IN_ADVANCE);
        
        final Date startDate = TimePeriod.clearTime(cal.getTime());
        
        final Time startTime = new Time(this.timeFormatter.parse("1899-12-30 15:00:00").getTime());
        final Time endTime = new Time(this.timeFormatter.parse("1899-12-30 16:00:00").getTime());
        
        final TimePeriod timePeriod = new TimePeriod(startDate, startDate, startTime, endTime);
        
        final RoomReservation roomReservation = new RoomReservation(timePeriod, roomArrangement);
        roomReservation.setReservationName("Test");
        roomReservation.setRequestedBy(USER_ID);
        roomReservation.setRequestedFor(USER_ID);
        roomReservation.setCreatedBy(USER_ID);
        
        this.roomReservationDataSource.save(roomReservation);
        
        if (roomArrangement.getCostUnit() == Constants.COST_UNIT_HOUR) {
            Assert.assertEquals(roomArrangement.getCostPerUnit() * timePeriod.getHoursDifference(),
                roomReservation.getCost());
        }
        
    }
     
}
