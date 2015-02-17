package com.archibus.app.reservation.util;

import java.sql.Time;
import java.text.*;
import java.util.*;

import org.json.*;

import com.archibus.app.reservation.domain.*;
import com.archibus.context.ContextStore;
import com.archibus.datasource.data.DataRecord;
import com.archibus.eventhandler.EventHandlerBase;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.utility.StringUtil;

/**
 * The Class TimelineHelper.
 */
public final class TimelineHelper {
    
    /** Qualified field name of the post_block field in the resources table. */
    private static final String RESOURCES_POST_BLOCK = "resources.post_block";
    
    /** Qualified field name of the pre_block field in the resources table. */
    private static final String RESOURCES_PRE_BLOCK = "resources.pre_block";
    
    /** JSON property name for the column index marking the end of availability of a resource. */
    private static final String JSON_COLUMN_AVAILABLE_TO = "columnAvailableTo";
    
    /** JSON property name for the column index marking the start of availability of a resource. */
    private static final String JSON_COLUMN_AVAILABLE_FROM = "columnAvailableFrom";
    
    /** JSON property name for the name of a resource. */
    private static final String JSON_NAME = "name";
    
    /** JSON property name for the id of a resource. */
    private static final String JSON_RESOURCE_ID = "resourceId";
    
    /** JSON property name for the row index of a resource. */
    private static final String JSON_ROW = "row";
    
    /** JSON property name for the status of an event. */
    private static final String JSON_STATUS = "status";
    
    /** JSON property name for the ending column of an event. */
    private static final String JSON_COLUMN_END = "columnEnd";
    
    /** JSON property name for the starting column of an event. */
    private static final String JSON_COLUMN_START = "columnStart";
    
    /** JSON property name for the row index of an event. */
    private static final String JSON_RESOURCE_ROW = "resourceRow";
    
    /** JSON property name for the id of an event. */
    private static final String JSON_EVENT_ID = "eventId";
    
    /** JSON property name for the type of timemark. */
    private static final String JSON_TIMEMARK_TYPE = "type";
    
    /** JSON property name for the datetime label of a timemark. */
    private static final String JSON_DATE_TIME_LABEL = "dateTimeLabel";
    
    /** JSON property name for the start date/time of a timemark. */
    private static final String JSON_DATE_TIME_START = "dateTimeStart";
    
    /** JSON property name for the column index of a timemark. */
    private static final String JSON_COLUMN = "column";
    
    /** Java type name for time values. */
    private static final String TIME_TYPENAME = "java.sql.Time";
    
    /** Fieldname used to format time values for the time line. */
    private static final String TIMELINE_TIME_FIELDNAME = "aTime";
    
    /** A dash. */
    private static final String DASH = "-";
    
    /** JSON property name for number of minor segments per major segment. */
    private static final String JSON_MINOR_TO_MAJOR_RATIO = "minorToMajorRatio";
    
    /** JSON property name for post block size. */
    private static final String JSON_POST_BLOCK_TIMESLOTS = "postBlockTimeslots";
    
    /** JSON property name for pre block size. */
    private static final String JSON_PRE_BLOCK_TIMESLOTS = "preBlockTimeslots";
    
    /** JSON property name for start hour of the time line. */
    private static final String JSON_TIMELINE_START_HOUR = "timelineStartHour";
    
    /** JSON property name for timemarks. */
    private static final String JSON_TIMEMARKS = "timemarks";
    
    /** Maximum interval in minutes for a minor segment on the time line. */
    private static final int MAX_MINOR_SEGMENT_MINUTES = 30;
    
    /** Default number of minor segments per hour on the time line. */
    private static final int DEFAULT_SEGMENTS_PER_HOUR = 6;
    
    /** 60 minutes in an hour. */
    private static final int MINUTES_IN_HOUR = 60;
    
    /** Default starting hour for the time line. */
    private static final int DEFAULT_START_HOUR = 8;
    
    /** 24 hours in a day. */
    private static final int HOURS_IN_DAY = 24;
    
    /** Default ending hour for the time line. */
    private static final int DEFAULT_END_HOUR = 20;
    
    /** The Constant ACTIVITY_ID. */
    private static final String ACTIVITY_ID = "AbWorkplaceReservations";
    
    /** The Constant DATE_FORMAT. */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    /** The Constant TIME_FORMAT. */
    private static final String TIME_FORMAT = "1899-12-30 HH:mm";
    
    /** The Constant ATTENDEE_START_BLOCK. */
    private static final int ATTENDEE_START_BLOCK = 0;
    
    /** The Constant ATTENDEE_POST_BLOCK. */
    private static final int ATTENDEE_POST_BLOCK = 0;
    
    /** The Constant ATTENDEE_PRE_BLOCK. */
    private static final int ATTENDEE_PRE_BLOCK = 0;
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private TimelineHelper() {
    }
    
    /**
     * Creates the timeline.
     * 
     * @return the jSON object
     */
    public static JSONObject createTimeline() {
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        
        final JSONObject timeline = new JSONObject();
        
        // BEGIN: init timeline, created timeline mark
        int timelineStartHour = DEFAULT_START_HOUR;
        int timelineEndHour = DEFAULT_END_HOUR;
        int minorSegments = DEFAULT_SEGMENTS_PER_HOUR;
        
        // Get time start and end values
        // Supported values are 0-24 integer as hour, or a formatted time
        // value that we can pull the hour out of
        final Integer startHour = getTimelineHourParam(context, ACTIVITY_ID, "TimelineStartTime");
        if (startHour != null) {
            timelineStartHour = startHour.intValue();
        }
        final Integer endHour = getTimelineHourParam(context, ACTIVITY_ID, "TimelineEndTime");
        if (endHour != null) {
            timelineEndHour = endHour.intValue();
        }
        
        // Error checking on start and end time parameters
        timelineStartHour = Math.max(0, timelineStartHour);
        timelineEndHour = Math.min(HOURS_IN_DAY, timelineEndHour);
        timelineStartHour = Math.min(timelineStartHour, timelineEndHour);
        timelineEndHour = Math.max(timelineStartHour, timelineEndHour);
        
        // Number of segments each hour is broken into - these will be
        // separated by minor timemarks
        final Integer minutesTimeUnit =
                EventHandlerBase.getActivityParameterInt(context, ACTIVITY_ID, "MinutesTimeUnit");
        if (minutesTimeUnit != null) {
            // find out how many minor timemarks to generate per hour
            final int interval = minutesTimeUnit.intValue();
            // Valid intervals are between 1 and 30 - don't generate minor
            // marks outside that range
            if (interval > 0 && interval <= MAX_MINOR_SEGMENT_MINUTES) {
                // Number of minor marks is closest integer
                minorSegments = MINUTES_IN_HOUR / interval;
            }
        }
        timeline.put(JSON_MINOR_TO_MAJOR_RATIO, minorSegments);
        timeline.put(JSON_TIMELINE_START_HOUR, timelineStartHour);
        timeline.put("timelineEndHour", timelineEndHour);
        
        retrieveTimemarks(context, timeline, timelineStartHour, timelineEndHour, minorSegments);
        
        return timeline;
    }
    
    /**
     * Creates the room reservation event.
     * 
     * @param timeline the timeline
     * @param roomArrangement the room arrangement
     * @param roomAllocation the room allocation
     * @param rowIndex the row index
     * @return the jSON object
     */
    public static JSONObject createRoomReservationEvent(final JSONObject timeline,
            final RoomArrangement roomArrangement, final RoomAllocation roomAllocation,
            final int rowIndex) {
        final int maxTimemarksColumn = ((JSONArray) timeline.get(JSON_TIMEMARKS)).length();
        final int timelineStartHour = timeline.getInt(JSON_TIMELINE_START_HOUR);
        final int minorSegments = timeline.getInt(JSON_MINOR_TO_MAJOR_RATIO);
        
        final JSONObject event = new JSONObject();
        event.put(JSON_EVENT_ID, roomAllocation.getId());
        event.put(JSON_RESOURCE_ROW, rowIndex);
        event.put(
            JSON_COLUMN_START,
            getTimeColumn(timelineStartHour, minorSegments, roomAllocation.getStartTime(),
                maxTimemarksColumn));
        event.put(
            JSON_COLUMN_END,
            getTimeColumn(timelineStartHour, minorSegments, roomAllocation.getEndTime(),
                maxTimemarksColumn) - 1);
        // Search for the preblock and postblock timeslots
        event.put(JSON_PRE_BLOCK_TIMESLOTS,
            getTimeSlots(roomArrangement.getPreBlock(), minorSegments));
        event.put(JSON_POST_BLOCK_TIMESLOTS,
            getTimeSlots(roomArrangement.getPostBlock(), minorSegments));
        
        event.put(JSON_STATUS, 0);
        
        return event;
    }
    
    /**
     * Creates the attendee calendar event.
     * 
     * @param timeline the timeline
     * @param calendarEvent the calendar event
     * @param rowIndex the row index
     * @return the jSON object
     */
    public static JSONObject createAttendeeCalendarEvent(final JSONObject timeline,
            final ICalendarEvent calendarEvent, final int rowIndex) {
        
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        
        final int maxTimemarksColumn = ((JSONArray) timeline.get(JSON_TIMEMARKS)).length();
        final int timelineStartHour = timeline.getInt(JSON_TIMELINE_START_HOUR);
        final int minorSegments = timeline.getInt(JSON_MINOR_TO_MAJOR_RATIO);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH);
        
        final JSONObject event = new JSONObject();
        
        event.put("startDate", dateFormat.format(calendarEvent.getStartTime()));
        event.put("endDate", dateFormat.format(calendarEvent.getEndTime()));
        event.put("startTime", timeFormat.format(calendarEvent.getStartTime()));
        event.put("endTime", timeFormat.format(calendarEvent.getEndTime()));
        
        event.put(JSON_EVENT_ID, calendarEvent.getEventId());
        event.put(JSON_RESOURCE_ROW, rowIndex);
        event.put(JSON_COLUMN_START, TimelineHelper.getTimeColumn(timelineStartHour, minorSegments,
            EventHandlerBase.getTimeValue(context, calendarEvent.getStartTime()),
            maxTimemarksColumn));
        event.put(JSON_COLUMN_END,
            TimelineHelper.getTimeColumn(timelineStartHour, minorSegments,
                EventHandlerBase.getTimeValue(context, calendarEvent.getEndTime()),
                maxTimemarksColumn) - 1);
        // Attendees do not have pre- and postblocks.
        event.put(JSON_PRE_BLOCK_TIMESLOTS, 0);
        event.put(JSON_POST_BLOCK_TIMESLOTS, 0);
        event.put(JSON_STATUS, 0);
        
        return event;
    }
    
    /**
     * Creates the resource reservation event.
     * 
     * @param timeline the timeline
     * @param resource the resource
     * @param resourceAllocation the resource allocation
     * @param rowIndex the row index
     * @return the jSON object
     */
    public static JSONObject createResourceReservationEvent(final JSONObject timeline,
            final DataRecord resource, final ResourceAllocation resourceAllocation,
            final int rowIndex) {
        final int maxTimemarksColumn = ((JSONArray) timeline.get(JSON_TIMEMARKS)).length();
        final int timelineStartHour = timeline.getInt(JSON_TIMELINE_START_HOUR);
        final int minorSegments = timeline.getInt(JSON_MINOR_TO_MAJOR_RATIO);
        
        final JSONObject event = new JSONObject();
        event.put(JSON_EVENT_ID, resourceAllocation.getId());
        event.put(JSON_RESOURCE_ROW, rowIndex);
        event.put(
            JSON_COLUMN_START,
            getTimeColumn(timelineStartHour, minorSegments, resourceAllocation.getStartTime(),
                maxTimemarksColumn));
        event.put(
            JSON_COLUMN_END,
            getTimeColumn(timelineStartHour, minorSegments, resourceAllocation.getEndTime(),
                maxTimemarksColumn) - 1);
        // Search for the preblock and postblock timeslots
        event.put(JSON_PRE_BLOCK_TIMESLOTS,
            getTimeSlots(resource.getInt(RESOURCES_PRE_BLOCK), minorSegments));
        event.put(JSON_POST_BLOCK_TIMESLOTS,
            getTimeSlots(resource.getInt(RESOURCES_POST_BLOCK), minorSegments));
        
        event.put(JSON_STATUS, 0);
        
        return event;
    }
    
    /**
     * Creates the room arrangement resource.
     * 
     * @param timeline the timeline
     * @param roomArrangement the room arrangement
     * @param rowIndex the row index
     * @return the jSON object
     */
    public static JSONObject createRoomArrangementResource(final JSONObject timeline,
            final RoomArrangement roomArrangement, final int rowIndex) {
        final int maxTimemarksColumn = ((JSONArray) timeline.get(JSON_TIMEMARKS)).length();
        final int timelineStartHour = timeline.getInt(JSON_TIMELINE_START_HOUR);
        final int minorSegments = timeline.getInt(JSON_MINOR_TO_MAJOR_RATIO);
        
        // this is the unique key
        final String resourceId =
                roomArrangement.getBlId() + DASH + roomArrangement.getFlId() + DASH
                        + roomArrangement.getRmId() + DASH + roomArrangement.getConfigId() + DASH
                        + roomArrangement.getArrangeTypeId();
        // the name will be displayed
        final String resourceName =
                roomArrangement.getBlId() + DASH + roomArrangement.getFlId() + DASH
                        + roomArrangement.getRmId();
        
        final JSONObject resource = new JSONObject();
        resource.put(JSON_ROW, rowIndex);
        resource.put(JSON_RESOURCE_ID, resourceId);
        
        resource.put(JSON_NAME, resourceName);
        
        resource.put("configId", roomArrangement.getConfigId());
        resource.put("arrangeTypeId", roomArrangement.getArrangeTypeId());
        
        resource.put(JSON_PRE_BLOCK_TIMESLOTS,
            getTimeSlots(roomArrangement.getPreBlock(), minorSegments));
        resource.put(JSON_POST_BLOCK_TIMESLOTS,
            getTimeSlots(roomArrangement.getPostBlock(), minorSegments));
        resource.put(
            JSON_COLUMN_AVAILABLE_FROM,
            getTimeColumn(timelineStartHour, minorSegments, roomArrangement.getDayStart(),
                maxTimemarksColumn));
        resource.put(
            JSON_COLUMN_AVAILABLE_TO,
            getTimeColumn(timelineStartHour, minorSegments, roomArrangement.getDayEnd(),
                maxTimemarksColumn));
        
        return resource;
    }
    
    /**
     * Creates the attendee resource.
     * 
     * @param timeline the timeline
     * @param email the email
     * @param rowIndex the row index
     * @return the jSON object
     */
    public static JSONObject createAttendeeResource(final JSONObject timeline, final String email,
            final int rowIndex) {
        
        final int maxTimemarksColumn = ((JSONArray) timeline.get(JSON_TIMEMARKS)).length();
        
        final JSONObject resource = new JSONObject();
        resource.put(JSON_ROW, rowIndex);
        resource.put(JSON_RESOURCE_ID, email);
        resource.put("email", email);
        resource.put(JSON_NAME, "");
        resource.put(JSON_PRE_BLOCK_TIMESLOTS, ATTENDEE_PRE_BLOCK);
        resource.put(JSON_POST_BLOCK_TIMESLOTS, ATTENDEE_POST_BLOCK);
        resource.put(JSON_COLUMN_AVAILABLE_FROM, ATTENDEE_START_BLOCK);
        resource.put(JSON_COLUMN_AVAILABLE_TO, maxTimemarksColumn);
        
        return resource;
    }
    
    /**
     * Creates the reservable resource.
     * 
     * @param timeline the timeline
     * @param reservableResource the reservable resource
     * @param rowIndex the row index
     * @return the jSON object
     */
    public static JSONObject createReservableResource(final JSONObject timeline,
            final DataRecord reservableResource, final int rowIndex) {
        final int maxTimemarksColumn = ((JSONArray) timeline.get(JSON_TIMEMARKS)).length();
        final int timelineStartHour = timeline.getInt(JSON_TIMELINE_START_HOUR);
        final int minorSegments = timeline.getInt(JSON_MINOR_TO_MAJOR_RATIO);
        
        // this is the unique key
        final String resourceId = reservableResource.getString("resources.resource_id");
        // the name will be displayed
        final String resourceName = reservableResource.getString("resources.resource_name");
        final String resourceStd = reservableResource.getString("resources.resource_std");
        
        // use the quantity field to store the required quantity
        final int quantity = reservableResource.getInt("resources.quantity");
        
        final JSONObject resource = new JSONObject();
        resource.put(JSON_ROW, rowIndex);
        resource.put(JSON_RESOURCE_ID, resourceId);
        resource.put("resourceName", resourceName);
        resource.put("resourceStd", resourceStd);
        resource.put(JSON_NAME, resourceName);
        // place holder for quantity
        resource.put("quantity", String.valueOf(quantity));
        
        resource.put(JSON_PRE_BLOCK_TIMESLOTS,
            getTimeSlots(reservableResource.getInt(RESOURCES_PRE_BLOCK), minorSegments));
        resource.put(JSON_POST_BLOCK_TIMESLOTS,
            getTimeSlots(reservableResource.getInt(RESOURCES_POST_BLOCK), minorSegments));
        resource.put(
            JSON_COLUMN_AVAILABLE_FROM,
            getTimeColumn(timelineStartHour, minorSegments,
                new Time(reservableResource.getDate("resources.day_start").getTime()),
                maxTimemarksColumn));
        resource.put(
            JSON_COLUMN_AVAILABLE_TO,
            getTimeColumn(timelineStartHour, minorSegments,
                new Time(reservableResource.getDate("resources.day_end").getTime()),
                maxTimemarksColumn));
        
        return resource;
    }
    
    /**
     * Gets the time column.
     * 
     * @param timelineStartHour the timeline start hour
     * @param minorSegments the minor segments
     * @param timeOfDay the time of day
     * @param maxTimemarksColumn the max timemarks column
     * @return the time column
     */
    public static int getTimeColumn(final int timelineStartHour, final int minorSegments,
            final Time timeOfDay, final int maxTimemarksColumn) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(timeOfDay);
        final int resStartHour = calendar.get(Calendar.HOUR_OF_DAY);
        final int resStartMin = calendar.get(Calendar.MINUTE);
        
        // Calculate column to nearest hour
        int columnAvailableFrom = (resStartHour - timelineStartHour) * minorSegments;
        
        // Add additional segments for minutes
        columnAvailableFrom +=
                (int) Math.ceil(resStartMin * minorSegments / (double) MINUTES_IN_HOUR);
        
        // if the resource is availabe after the timeline end time, assume column
        // MaxTimemarksColumn-1
        if (columnAvailableFrom >= maxTimemarksColumn) {
            columnAvailableFrom = maxTimemarksColumn;
        }
        // if the resource is availabe before the timeline start time, assume column 0
        // negative column values are not allowed
        if (columnAvailableFrom < 0) {
            columnAvailableFrom = 0;
        }
        return columnAvailableFrom;
    }
    
    /**
     * Gets the time slots.
     * 
     * @param val the val
     * @param minorSegments the minor segments
     * @return the time slots
     */
    private static int getTimeSlots(final Integer val, final int minorSegments) {
        int slots = 0;
        if (val != null) {
            // KB 3018952, for preBlock less than 1, make it equal to 1. Modified by ZY, 2008-08-05.
            final double temp = val.doubleValue() * minorSegments / MINUTES_IN_HOUR;
            slots = (int) Math.ceil(temp);
        }
        return slots;
    }
    
    /**
     * Retrieves a timeline start or end hour from the afm_activity_params table.
     * 
     * @param context the context
     * @param activityId the activity id
     * @param paramId the param id
     * @return the timeline hour param
     */
    private static Integer getTimelineHourParam(final EventHandlerContext context,
            final String activityId, final String paramId) {
        Integer val = null;
        final String timelineHourParam =
                EventHandlerBase.getActivityParameterString(context, activityId, paramId);
        // activityparameter error message
        final String errMessage =
                EventHandlerBase.localizeMessage(context, ACTIVITY_ID, "LOADTIMELINE_WFR",
                    "INVALIDPARAMETERERROR", null);
        if (StringUtil.notNullOrEmpty(timelineHourParam)) {
            // First see if it's an integer
            try {
                val = Integer.valueOf(timelineHourParam);
            } catch (final NumberFormatException ne) {
                // Not an int, see if it's a valid Time value
                try {
                    final SimpleDateFormat format =
                            new SimpleDateFormat("HH:mm.ss.SSS", Locale.ENGLISH);
                    final java.util.Date dateValue = format.parse(timelineHourParam);
                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dateValue);
                    val = Integer.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
                } catch (final ParseException e) {
                    // Invalid format - log error
                    // Classlog.error(errMessage+paramId);
                    context.addResponseParameter("message", errMessage + paramId);
                }
            }
        }
        return val;
    }
    
    /**
     * Retrieve timemarks.
     * 
     * @param context the context
     * @param timeline the timeline
     * @param timelineStartHour the timeline start hour
     * @param timelineEndHour the timeline end hour
     * @param minorSegments the minor segments
     */
    private static void retrieveTimemarks(final EventHandlerContext context,
            final JSONObject timeline, final int timelineStartHour, final int timelineEndHour,
            final int minorSegments) {
        
        // generate major and minor timemarks and timeslots
        final JSONArray timemarks = new JSONArray();
        final Calendar calendar = Calendar.getInstance();
        
        int column = 0;
        for (int hour = timelineStartHour; hour < timelineEndHour; hour++) {
            calendar.clear();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            final Time time = new Time(calendar.getTimeInMillis());
            final String dateTimeStart = time.toString();
            final String dateTimeLabel =
                    EventHandlerBase.formatFieldValue(context, time, TIME_TYPENAME,
                        TIMELINE_TIME_FIELDNAME, true);
            
            final JSONObject timemark = new JSONObject();
            timemark.put(JSON_COLUMN, column++);
            timemark.put(JSON_DATE_TIME_START, dateTimeStart);
            timemark.put(JSON_DATE_TIME_LABEL, dateTimeLabel);
            timemark.put(JSON_TIMEMARK_TYPE, "major");
            timemarks.put(timemark);
            
            // Create minor timemarks for the intervals for all but the last hour
            if (hour < timelineEndHour) {
                for (int segment = 1; segment < minorSegments; segment++) {
                    final int minutes = segment * (MINUTES_IN_HOUR / minorSegments);
                    calendar.set(Calendar.MINUTE, minutes);
                    final Time tMinor = new Time(calendar.getTimeInMillis());
                    final String minorTimeLabel =
                            EventHandlerBase.formatFieldValue(context, tMinor, TIME_TYPENAME,
                                TIMELINE_TIME_FIELDNAME, true);
                    
                    final JSONObject minorTimemark = new JSONObject();
                    minorTimemark.put(JSON_COLUMN, column++);
                    minorTimemark.put(JSON_DATE_TIME_START, tMinor.toString());
                    minorTimemark.put(JSON_DATE_TIME_LABEL, minorTimeLabel);
                    minorTimemark.put(JSON_TIMEMARK_TYPE, "minor");
                    timemarks.put(minorTimemark);
                }
            }
        }
        timeline.put(JSON_TIMEMARKS, timemarks);
        calendar.clear();
        calendar.set(Calendar.HOUR_OF_DAY, timelineEndHour);
        timeline.put("dateTimeEnd", new Time(calendar.getTimeInMillis()).toString());
    }
}
