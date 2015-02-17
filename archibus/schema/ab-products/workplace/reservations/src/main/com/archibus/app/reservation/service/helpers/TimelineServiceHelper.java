package com.archibus.app.reservation.service.helpers;

import java.sql.Time;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.dao.datasource.RoomArrangementDataSource;
import com.archibus.app.reservation.domain.ICalendarEvent;
import com.archibus.app.reservation.domain.RoomAllocation;
import com.archibus.app.reservation.domain.RoomArrangement;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.app.reservation.domain.TimePeriod;
import com.archibus.app.reservation.service.RoomReservationServiceBase;
import com.archibus.app.reservation.util.TimelineHelper;
import com.archibus.utility.ExceptionBase;
import com.archibus.utility.StringUtil;

/**
 * The Class TimelineServiceHelper.
 */
public class TimelineServiceHelper extends RoomReservationServiceBase { 

    /** The Constant EVENTS. */
    private static final String EVENTS = "events";

    /** The room arrangement data source. */
    private RoomArrangementDataSource roomArrangementDataSource; 
    
    /**
     * Create Room Allocation events.
     *
     * @param startDate the start date
     * @param reservationId the reservation id
     * @param timeline the timeline
     * @param events the events
     * @param rowIndex the row index
     * @param roomArrangement the room arrangement
     */
    public void createRoomAllocationEvents(final Date startDate, final Integer reservationId, 
            final JSONObject timeline, final JSONArray events, final int rowIndex,
            final RoomArrangement roomArrangement) {

        final List<RoomAllocation> roomAllocations =
                this.roomAllocationDataSource.getAllocatedRooms(startDate, roomArrangement,
                        reservationId);

        for (final RoomAllocation roomAllocation : roomAllocations) {
            final RoomArrangement allocatedArrangement =
                    this.roomArrangementDataSource.get(roomAllocation.getBlId(),
                            roomAllocation.getFlId(), roomAllocation.getRmId(),
                            roomAllocation.getConfigId(), roomAllocation.getArrangeTypeId());
            events.put(TimelineHelper.createRoomReservationEvent(timeline, allocatedArrangement,
                    roomAllocation, rowIndex));
        }
    }
    
    
    /**
     * Check server available.
     *
     * @throws ExceptionBase the exception base
     */
    public void checkServerAvailable() throws ExceptionBase {
        this.calendarService.checkServiceAvailable();
    }

    /**
     * Create attendee events.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param uniqueId the unique id
     * @param timeline the timeline 
     * @param timeZone the time zone
     * @param email the email
     * @param currentIndex the current index
     */
    public void createAttendeeEvents(final Date startDate, final Date endDate,
            final String uniqueId, final JSONObject timeline, final TimeZone timeZone, 
            final String email, final int currentIndex) {
        // get the calendar events for this attendee
        final List<ICalendarEvent> calendarEvents =
                this.calendarService.findAttendeeAvailability(startDate, endDate, timeZone, email);

        for (final ICalendarEvent calendarEvent : calendarEvents) {
            if (!(StringUtil.notNullOrEmpty(uniqueId) && uniqueId.equals(calendarEvent.getEventId()))) {               
                final JSONArray events = timeline.getJSONArray(EVENTS);
                events.put(TimelineHelper.createAttendeeCalendarEvent(timeline, calendarEvent,
                        currentIndex));
            }
        }
    }



    /**
     * Creates the room reservation object for the given parameters.
     *
     * @param reservationId the reservation id
     * @param startDate the start date
     * @param startTime the start time of the reservation
     * @param endTime the end time of the reservation
     * @param locationFilter the location filter
     * @return the room reservation
     */
    public RoomReservation createRoomReservation(final Integer reservationId,
            final Date startDate, final Time startTime, final Time endTime,
            final Map<String, String> locationFilter) {

        final TimePeriod timePeriod = new TimePeriod(startDate, startDate, startTime, endTime);

        final String blId = StringUtil.notNull(locationFilter.get(Constants.BL_ID_FIELD_NAME));
        final String flId = StringUtil.notNull(locationFilter.get(Constants.FL_ID_FIELD_NAME));
        final String rmId = StringUtil.notNull(locationFilter.get(Constants.RM_ID_FIELD_NAME));
        //        final String configId =
        //                StringUtil.notNull(locationFilter.get(Constants.CONFIG_ID_FIELD_NAME));
        final String arrangeTypeId = StringUtil.notNull(locationFilter.get("rm_arrange_type_id"));

        final RoomReservation roomReservation =
                new RoomReservation(timePeriod, blId, flId, rmId, null, arrangeTypeId);

        // when editing a reservation
        roomReservation.setReserveId(reservationId);

        // when editing  a recurrent reservation we need the parent id
        if (reservationId > 0) {
            final RoomReservation existingReservation = this.roomReservationDataSource.get(reservationId);
            roomReservation.setParentId(existingReservation.getParentId());
        }        

        return roomReservation;
    }

    /**
     * Sets the room arrangement data source for getting detailed arrangement info to use on the
     * timeline.
     * 
     * @param roomArrangementDataSource the new room arrangement data source that will provide
     *            detailed room arrangement info
     */
    public void setRoomArrangementDataSource(
            final RoomArrangementDataSource roomArrangementDataSource) {
        this.roomArrangementDataSource = roomArrangementDataSource;
    } 

}