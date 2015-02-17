package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.text.ParseException;
import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.data.DataRecord;

/**
 * Test for ResourceDataSource.
 */
public class ResourceDataSourceTest extends ReservationDataSourceTestBase {
    
    /** start time as string. */
    private static final String END_TIME = "1899-12-30 14:00:00";
    
    /** end time as string. */
    private static final String START_TIME = "1899-12-30 10:00:00";
    
    /** coffee resource JFQ. */
    private static final String COFFEE_JFK = "COFFEE JFK";
    
    /** coffee resource HQ. */
    private static final String COFFEE_HQ = "COFFEE HQ";
    
    /** Resource ID used for testing. */
    private static final String TEST_ID = "TEST";
    
    /** Resource standard used for testing. */
    private static final String RESOURCE_STANDARD = "SOFT DRINKS";
    
    /**
     * test if a resource is allowed in a building.
     */
    public void testCheckAvailableResource() {
        final boolean allowed =
                this.resourceDataSource.checkResourceAvailable(COFFEE_HQ, this.existingReservation,
                    this.existingReservation.getTimePeriod());
        Assert.assertTrue(allowed);
        
        final boolean notAllowed =
                this.resourceDataSource.checkResourceAvailable(COFFEE_JFK,
                    this.existingReservation, this.existingReservation.getTimePeriod());
        Assert.assertFalse(notAllowed);
    }
    
    /**
     * Test saving a resource.
     * 
     * @throws ParseException when the test dates cannot be parsed
     */
    public void testSaveResource() throws ParseException {
        final Time startTime = new Time(this.timeFormatter.parse("1899-12-30 8:00:00").getTime());
        final Time endTime = new Time(this.timeFormatter.parse("1899-12-30 17:00:00").getTime());
        
        final Resource resource = new Resource();
        resource.setResourceType(ResourceType.UNLIMITED.toString());
        resource.setResourceId(TEST_ID);
        resource.setAnnounceTime(new Time(0));
        resource.setCancelTime(new Time(0));
        resource.setDayEnd(endTime);
        resource.setDayStart(startTime);
        
        resource.setResourceStandard(RESOURCE_STANDARD);
        resource.setResourceName("Test Resource");
        // resource.setDocImage("null");
        resource.setCostUnit(0);
        resource.setPreBlock(0);
        
        this.resourceDataSource.save(resource);
        final Resource resource2 = this.resourceDataSource.get(TEST_ID);
        
        Assert.assertEquals(RESOURCE_STANDARD, resource2.getResourceStandard());
    }
    
    /**
     * Test getting unique resources.
     */
    public void testGetUniqueResources() {
        
        try {
            
            final List<Resource> resources =
                    this.resourceDataSource.findAvailableUniqueResources(this.existingReservation,
                        this.existingReservation.getTimePeriod());
            
            Assert.assertNotNull(resources);
            
        } catch (final ReservationException exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Test getting limited resources.
     */
    public void testGetLimitedResources() {
        
        try {
            
            final List<Resource> resources =
                    this.resourceDataSource.findAvailableLimitedResources(this.existingReservation,
                        this.existingReservation.getTimePeriod());
            
            Assert.assertNotNull(resources);
            
        } catch (final ReservationException exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Test getting unlimited catering resources.
     */
    public void testGetCateringResources() {
        try {
            final Date startDate = Utils.getDate(10);
            final Date endDate = Utils.getDate(10);
            
            final Time startTime = new Time(this.timeFormatter.parse(START_TIME).getTime());
            final Time endTime = new Time(this.timeFormatter.parse(END_TIME).getTime());
            
            final TimePeriod timePeriod = new TimePeriod(startDate, endDate, startTime, endTime);
            final RoomArrangement roomArrangement =
                    new RoomArrangement(BL_ID, FL_ID, RM_ID, CONFIG_ID, ARRANGE_TYPE_ID);
            
            final RoomReservation reservation2 = new RoomReservation(timePeriod, roomArrangement);
            
            final List<Resource> resources =
                    this.resourceDataSource.findAvailableUnlimitedResources(reservation2,
                        reservation2.getTimePeriod());
            
            Assert.assertNotNull(resources);
            
            reservation2.setStartTime(new Time(this.timeFormatter.parse("1899-12-30 06:00:00")
                .getTime()));
            
            final List<DataRecord> partialAvailableResources =
                    this.resourceDataSource.findAvailableUnlimitedResourceRecords(reservation2,
                        reservation2.getTimePeriod(), true);
            
            Assert.assertNotNull(partialAvailableResources);
            Assert.assertEquals(resources.size(), partialAvailableResources.size());
        } catch (final ParseException exception) {
            Assert.fail(exception.toString());
        } catch (final ReservationException exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Test getting the number of reserved resources.
     */
    public void testGetNumberOfReservedResources() {
        try {
            final Date startDate = Utils.getDate(DAYS_IN_ADVANCE);
            final Date endDate = Utils.getDate(DAYS_IN_ADVANCE);
            
            Time startTime = new Time(this.timeFormatter.parse(START_TIME).getTime());
            Time endTime = new Time(this.timeFormatter.parse(END_TIME).getTime());
            
            final TimePeriod timePeriod = new TimePeriod(startDate, endDate, startTime, endTime);
            Assert.assertEquals(0, this.resourceDataSource.getNumberOfReservedResources(timePeriod,
                COFFEE_HQ, null, true));
            
            // Create 5 reservations for the resource, partially overlapping.
            
            // First reservation: same time period.
            this.existingReservation.setTimePeriod(timePeriod);
            final ResourceAllocation alloc = new ResourceAllocation();
            alloc.setResourceId(COFFEE_HQ);
            alloc.setQuantity(1);
            alloc.setBlId(BL_ID);
            alloc.setFlId(FL_ID);
            alloc.setRmId(RM_ID);
            this.existingReservation.addResourceAllocation(alloc);
            this.roomReservationDataSource.save(this.existingReservation);
            
            Assert.assertEquals(1, this.resourceDataSource.getNumberOfReservedResources(timePeriod,
                COFFEE_HQ, null, true));
            Assert.assertEquals(0, this.resourceDataSource.getNumberOfReservedResources(timePeriod,
                COFFEE_HQ, this.existingReservation.getReserveId(), true));
            
            // Second reservation: ends before the time period, but overlaps in pre/post-block.
            startTime = new Time(this.timeFormatter.parse("1899-12-30 9:00:00").getTime());
            endTime = new Time(this.timeFormatter.parse("1899-12-30 9:55:00").getTime());
            final TimePeriod otherTimePeriod =
                    new TimePeriod(startDate, endDate, startTime, endTime);
            // Find an available room on the same floor.
            final RoomArrangement roomArrangement =
                    this.roomArrangementDataSource.findAvailableRooms(BL_ID, FL_ID, null,
                        ARRANGE_TYPE_ID, otherTimePeriod, null, null).get(0);
            // Create an other reservation, leaving the existing unmodified.
            final RoomReservation otherReservation =
                    createReservation(otherTimePeriod, roomArrangement);
            final ResourceAllocation otherAlloc = new ResourceAllocation();
            otherAlloc.setResourceId(COFFEE_HQ);
            otherAlloc.setQuantity(2);
            otherAlloc.setBlId(BL_ID);
            otherAlloc.setFlId(FL_ID);
            otherAlloc.setRmId(roomArrangement.getRmId());
            otherReservation.addResourceAllocation(otherAlloc);
            this.roomReservationDataSource.save(otherReservation);
            
            // Total number of reserved resources is the sum of the two allocations.
            final int totalCount = alloc.getQuantity() + otherAlloc.getQuantity();
            
            Assert.assertEquals(totalCount, this.resourceDataSource.getNumberOfReservedResources(
                timePeriod, COFFEE_HQ, null, true));
            
            final List<TimePeriod> otherPeriods = new ArrayList<TimePeriod>();
            
            // Third reservation: starts before, ends after.
            startTime = new Time(this.timeFormatter.parse("1899-12-30 9:30:00").getTime());
            endTime = new Time(this.timeFormatter.parse("1899-12-30 16:00:00").getTime());
            otherPeriods.add(new TimePeriod(startDate, endDate, startTime, endTime));
            
            // Fourth reservation: starts during, ends after.
            startTime = new Time(this.timeFormatter.parse("1899-12-30 10:30:00").getTime());
            endTime = new Time(this.timeFormatter.parse("1899-12-30 15:00:00").getTime());
            otherPeriods.add(new TimePeriod(startDate, endDate, startTime, endTime));
            
            // Fifth reservation: starts and ends during.
            startTime = new Time(this.timeFormatter.parse("1899-12-30 11:30:00").getTime());
            endTime = new Time(this.timeFormatter.parse("1899-12-30 13:00:00").getTime());
            otherPeriods.add(new TimePeriod(startDate, endDate, startTime, endTime));
            
            for (final TimePeriod otherPeriod : otherPeriods) {
                // Modify the original reservation to the new time.
                this.existingReservation =
                        this.roomReservationDataSource.get(this.existingReservation.getReserveId());
                this.existingReservation.setTimePeriod(otherPeriod);
                this.roomReservationDataSource.save(this.existingReservation);
                
                Assert.assertEquals(totalCount, this.resourceDataSource
                    .getNumberOfReservedResources(timePeriod, COFFEE_HQ, null, true));
            }
        } catch (final ParseException exception) {
            Assert.fail(exception.toString());
        }
    }
}
