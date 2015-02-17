package com.archibus.app.reservation.service;

import java.sql.Time;
import java.text.ParseException;
import java.util.HashMap;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;

import com.archibus.app.reservation.dao.datasource.ResourceDataSource;
import com.archibus.app.reservation.domain.ResourceAllocation;
import com.archibus.app.reservation.domain.RoomAllocation;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.data.DataSetList;


/**
 * The Class ResourceReservationServiceTest.
 */
public class ResourceTimelineServiceTest extends ReservationServiceTestBase {
    
    /** The Constant START_TIME. */
    private static final String START_TIME = "1899-12-30 10:00:00";

    /** The Constant END_TIME. */
    private static final String END_TIME = "1899-12-30 15:00:00"; 

    /** The Constant RESOURCES. */
    private static final String RESOURCES = "resources";

    /** The Constant EVENTS. */
    private static final String EVENTS = "events";

    /** A valid resource ID of a limited or unique resource. */
    private static final String RESOURCE_ID = "LCD-PROJECTOR2"; 
    
    /** The resource timeline service. */
    private ResourceTimelineService resourceTimelineService;
    
    /** The resource data source. */
    private ResourceDataSource resourceDataSource; 
    
    /**
     * Test loading the resource time line with overlapping reservations.
     */
    public void testLoadResourceTimeline() {
        try {
            final Time secondStartTime =
                    new Time(this.timeFormatter.parse(START_TIME).getTime());
            final Time secondEndTime =
                    new Time(this.timeFormatter.parse(END_TIME).getTime());
            testLoadResourceTimeline(secondStartTime, secondEndTime);
        } catch (final ParseException exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Test loading the resource time line with adjacent reservations. I.e. only the pre- and
     * post-blocks overlap.
     */
    public void testLoadResourceTimelineAdjacent() {
        try {
            final Time secondStartTime =
                    new Time(this.timeFormatter.parse("1899-12-30 10:59:59").getTime());
            final Time secondEndTime =
                    new Time(this.timeFormatter.parse(END_TIME).getTime());
            testLoadResourceTimeline(secondStartTime, secondEndTime);
        } catch (final ParseException exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Test loading the resource time line.
     * 
     * @param secondStartTime start time to use for the second reservation
     * @param secondEndTime end time to use for the second reservation
     */
    private void testLoadResourceTimeline(final Time secondStartTime, final Time secondEndTime) {
        final DataSetList resourceList = new DataSetList();
        final DataRecord record =
                this.resourceDataSource.getRecord("resource_id = '" + RESOURCE_ID + "'");
        record.setValue("resources.quantity", 1);
        resourceList.addRecord(record);
        JSONObject timeline =
                this.resourceTimelineService.loadResourceTimeLine(this.startDate, this.endDate,
                    new HashMap<String, String>(), resourceList, null);
        
        Assert.assertNotNull(timeline);
        JSONArray events = timeline.getJSONArray(EVENTS);
        JSONArray resources = timeline.getJSONArray(RESOURCES);
        Assert.assertNotNull(events);
        Assert.assertNotNull(resources);
        Assert.assertEquals(1, resources.length());
        Assert.assertEquals(0, events.length());
        
        // create some reservations for the resource
        final RoomReservation reservation1 = this.createRoomReservation();
        reservation1.addResourceAllocation(new ResourceAllocation(this.resourceDataSource
            .convertRecordToObject(record), reservation1, 1));
        
        this.reservationService.saveReservation(reservation1);
        
        final RoomReservation reservation2 = this.createRoomReservation();
        final RoomAllocation allocation = reservation2.getRoomAllocations().get(0);
        allocation.setRmId("109");
        allocation.setFlId("18");
        allocation.setConfigId("AAA");
        
        reservation2.setStartTime(secondStartTime);
        reservation2.setEndTime(secondEndTime);
        
        reservation2.addResourceAllocation(new ResourceAllocation(this.resourceDataSource
            .convertRecordToObject(record), reservation2, 1));
        this.reservationService.saveReservation(reservation2);
        
        timeline =
                this.resourceTimelineService.loadResourceTimeLine(this.startDate, this.endDate,
                    new HashMap<String, String>(), resourceList, null);
        
        events = timeline.getJSONArray(EVENTS);
        resources = timeline.getJSONArray(RESOURCES);
        Assert.assertNotNull(events);
        Assert.assertNotNull(resources);
        Assert.assertEquals(1, resources.length());
        Assert.assertEquals(1, events.length());
    }
 
    
    
    /**
     * Sets the resource timeline service.
     *
     * @param resourceTimelineService the new resource timeline service
     */
    public void setResourceTimelineService(
            final ResourceTimelineService resourceTimelineService) {
        this.resourceTimelineService = resourceTimelineService;
    }

    /**
     * Set the resource data source.
     * 
     * @param resourceDataSource the new resource data source
     */
    public void setResourceDataSource(final ResourceDataSource resourceDataSource) {
        this.resourceDataSource = resourceDataSource;
    }
    
}
