package com.archibus.app.reservation.service;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.json.JSONObject;

import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.datasource.data.DataRecord;


/**
 * Test for the RoomReservationService class.
 */ 
public class TimelineServiceTest extends RoomReservationServiceTestBase {


    /** The Constant EVENTS. */
    private static final String EVENTS = "events";

    /** The Constant RESOURCES. */
    private static final String RESOURCES = "resources"; 


    /** The Constant RESERVE_DATE_END. */
    private static final String RESERVE_DATE_END = "reserve.date_end";

    /** The Constant RESERVE_DATE_START. */
    private static final String RESERVE_DATE_START = "reserve.date_start"; 

    /** The Constant RESERVE_RES_ID. */
    private static final String RESERVE_RES_ID = "reserve.res_id";


    /** The Constant THREE. */
    private static final int THREE = 3;

    /** The timeline service. */
    protected TimelineService timelineService;

    /** The recurrence service. */
    protected RecurrenceService recurrenceService;

    /**
     * Test load room timeline.
     */
    public void testLoadRoomTimeline() {
        final Map<String, String> filter = new HashMap<String, String>();

        final JSONObject timeline =
                this.timelineService.loadRoomArrangementTimeLine(this.startDate,
                        this.endDate, this.startTime, this.endTime, filter, null, 0);
        Assert.assertNotNull(timeline);

        Assert.assertNotNull(timeline.get(RESOURCES));
        Assert.assertNotNull(timeline.get(EVENTS));
    }

    /**
     * Test load attendee timeline.
     */
    public void testLoadAttendeeTimeline() { 

        final Map<String, String> locationFilter = new HashMap<String, String>();
        locationFilter.put("bl_id", "HQ");

        final List<String> emails =
                Arrays.asList("paloma.callejo@procos1.onmicrosoft.com",
                        "yorik.gerlo@procos1.onmicrosoft.com", "tom.winters@mailinator.com");

        final JSONObject timeline =
                this.timelineService.loadAttendeeTimeline(this.startDate, this.endDate,
                        this.recurrencePattern, locationFilter, emails, "", null);
        Assert.assertNotNull(timeline);

        Assert.assertNotNull(timeline.get(RESOURCES));
        Assert.assertNotNull(timeline.get(EVENTS));

        Assert.assertEquals(THREE, timeline.getJSONArray(RESOURCES).length());
    }

    /**
     * Test get first and last date of a recurrence pattern.
     */
    public void testGetFirstAndLastDate() {
        final DataRecord reservation = this.createAndSaveRoomReservation(true);
        Date startDate = reservation.getDate(RESERVE_DATE_START);
        final String recurringRule = reservation.getString("reserve.recurring_rule");
        DataRecord dates =
                this.recurrenceService.getFirstAndLastDate(startDate, null, recurringRule,
                        null);
        final Date firstDate = dates.getDate(RESERVE_DATE_START);
        final Date lastDate = dates.getDate(RESERVE_DATE_END);

        Assert.assertTrue(lastDate.after(firstDate));

        final Integer parentId = reservation.getInt(RESERVE_RES_ID);
        dates =
                this.recurrenceService.getFirstAndLastDate(startDate, null, recurringRule,
                        parentId);
        final Date firstDate2 = dates.getDate(RESERVE_DATE_START);
        final Date lastDate2 = dates.getDate(RESERVE_DATE_END);
        Assert.assertEquals(firstDate, firstDate2);
        Assert.assertEquals(lastDate, lastDate2);

        // Now test with a different occurrence as start.
        final List<RoomReservation> reservations =
                this.roomReservationDataSource.getByParentId(parentId, null, startDate);
        startDate = reservations.get(THREE).getStartDate();
        Assert.assertTrue(startDate.after(firstDate));
        dates =
                this.recurrenceService.getFirstAndLastDate(startDate, null, recurringRule,
                        parentId);
        final Date firstDate3 = dates.getDate(RESERVE_DATE_START);
        final Date lastDate3 = dates.getDate(RESERVE_DATE_END);
        Assert.assertEquals(lastDate, lastDate3);
        Assert.assertEquals(startDate, firstDate3);
        Assert.assertTrue(lastDate3.after(firstDate3));
    }
 
    
    /**
     * Sets the timeline service.
     *
     * @param timelineService the new timeline service
     */
    public void setTimelineService(final TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    /**
     * Sets the recurrence service.
     *
     * @param recurrenceService the new recurrence service
     */
    public void setRecurrenceService(final RecurrenceService recurrenceService) {
        this.recurrenceService = recurrenceService;
    }
    
    

}
