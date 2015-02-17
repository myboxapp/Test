package com.archibus.app.reservation.service;

import java.sql.Time;
import java.util.*;

import org.apache.log4j.Logger;
import org.json.*;

import com.archibus.app.reservation.dao.datasource.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.*;
import com.archibus.app.reservation.service.helpers.TimelineServiceHelper;
import com.archibus.app.reservation.util.*;
import com.archibus.utility.*;

/**
 * The Class TimelineService.
 */
public class TimelineService {

    /** Error message when room reservation is not found. */
    // @translatable
    private static final String RESERVATION_NOT_FOUND = "Room reservation not found";

    /** The Constant RESOURCES. */
    private static final String RESOURCES = "resources";

    /** The Constant EVENTS. */
    private static final String EVENTS = "events";
    
    /** The Constant MESSAGE. */
    private static final String MESSAGE = "message";
    
    /** The default max. number of occurrences to check attendee availability for. */
    private static final int DEFAULT_MAX_OCCURRENCES = 10;

    /** The logger. */
    private final Logger logger = Logger.getLogger(this.getClass());

    /** The timeline service helper. */
    private TimelineServiceHelper timelineServiceHelper;

    /** The room reservation data source. */
    private RoomReservationDataSource roomReservationDataSource;

    /** The reservations service. */
    private IReservationService reservationService;

    /**
     * Load the room arrangement timeline.
     * <p>
     * The room arrangements are filtered by location parameters and arrange type. Additional room
     * attributes (e.g. fixed resources) can also be specified.
     * </p>
     * <p>
     * For recurrent reservation the end date and the recurrence pattern are provided. The
     * recurrence pattern uses the default ARCHIBUS pattern and can have daily, weekly or monthly
     * pattern. *
     * </p>
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param startTime the start time
     * @param endTime the end time
     * @param searchFilter the search filter
     * @param fixedResourceStandards list of fixed resource standard ids
     * @param reservationId the reserve id (when editing a reservation)
     * @return timeline object
     */
    public JSONObject loadRoomArrangementTimeLine(final Date startDate, final Date endDate,
            final Time startTime, final Time endTime, final Map<String, String> searchFilter,
            final List<String> fixedResourceStandards, final Integer reservationId) { 

        JSONObject timeline = null;
      
        // create the room reservation object for the start date, using the search parameters
        final RoomReservation roomReservation =
                timelineServiceHelper.createRoomReservation(reservationId, startDate, startTime, endTime, searchFilter);

        final String numberAttendees = searchFilter.get("number_attendees");
        Integer numberOfAttendees = null;
        if (StringUtil.notNullOrEmpty(numberAttendees)) {
            numberOfAttendees = Integer.valueOf(numberAttendees);
        }

        final String externalAllowed = searchFilter.get("external_allowed");
        boolean externalsMustBeAllowed = true;
        if (StringUtil.notNullOrEmpty(externalAllowed)) {
            externalsMustBeAllowed = Integer.parseInt(externalAllowed) > 0;
        }

        final String recurrenceRule = searchFilter.get("recurrence_rule");

        // when editing a recurrent reservation remain the dates of all occurrences
        // when editing a single occurrence the recurrence rule should be empty
        if (StringUtil.notNullOrEmpty(recurrenceRule) && reservationId != null && reservationId > 0) {
            timeline = loadRoomArrangementTimeLineEditRecurrence(roomReservation, numberOfAttendees, 
                    externalsMustBeAllowed, fixedResourceStandards);
        }  else {
            timeline = loadRoomArrangementTimeline(roomReservation, startDate, endDate, recurrenceRule, 
                    fixedResourceStandards, numberOfAttendees, externalsMustBeAllowed);
        }

        return timeline;
    }

    /**
     * Load the attendees timeline.
     * 
     * Load all attendees and retrieve the free/busy in the calendar service (Exchange)
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param recurrenceRule the recurrence pattern
     * @param locationFilter the location filter used in the form (for time zone information based
     * on building id)
     * @param emails the email addresses of the attendees
     * @param uniqueId the unique reference to the active reservation
     * @param reservationId the reservation id
     * @return json timeline object
     */
    public JSONObject loadAttendeeTimeline(final Date startDate, final Date endDate,
            final String recurrenceRule, final Map<String, String> locationFilter,
            final List<String> emails, final String uniqueId, final Integer reservationId) { 

        final String buildingId =
                locationFilter
                    .get(com.archibus.app.reservation.dao.datasource.Constants.BL_ID_FIELD_NAME);
        final TimeZone timeZone = TimeZone.getTimeZone(TimeZoneConverter.getTimeZoneIdForBuilding(buildingId));

        final JSONObject timeline = TimelineHelper.createTimeline(); 

        if (!emails.isEmpty()) {
            final JSONArray events = new JSONArray();
            final JSONArray resources = new JSONArray();
            final JSONArray failures = new JSONArray();

            timeline.put(EVENTS, events);
            timeline.put(RESOURCES, resources);
            timeline.put(MESSAGE, failures);
            
            // when editing use the existing dates
            if (StringUtil.notNullOrEmpty(recurrenceRule) && reservationId != null
                    && reservationId > 0) {
                loadAttendeeTimelineEditRecurrence(timeline, emails, reservationId, timeZone);
            } else {
                loadAttendeeTimeline(timeline, startDate, endDate, recurrenceRule, emails,
                    uniqueId, timeZone);
            }
        }
        
        return timeline;
    }

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
     * Sets the reservation service.
     *
     * @param reservationService the new reservation service
     */
    public void setReservationService(final IReservationService reservationService) {
        this.reservationService = reservationService;
    }


    /**
     * Sets the timeline service helper.
     *
     * @param timelineServiceHelper the new timeline service helper
     */
    public void setTimelineServiceHelper(final TimelineServiceHelper timelineServiceHelper) {
        this.timelineServiceHelper = timelineServiceHelper;
    } 


    /**
     * Load attendee timeline.
     *
     * @param timeline the time line JSON object
     * @param startDate the start date
     * @param endDate the end date
     * @param recurrenceRule the recurrence rule
     * @param emails the emails
     * @param uniqueId the unique id
     * @param timeZone the time zone
     */
    private void loadAttendeeTimeline(final JSONObject timeline, final Date startDate,
            final Date endDate, final String recurrenceRule, final List<String> emails,
            final String uniqueId, final TimeZone timeZone) {

        final JSONArray resources = timeline.getJSONArray(RESOURCES);
        final JSONArray failures = timeline.getJSONArray(MESSAGE);
        
        Recurrence recurrence = null;
        if (StringUtil.notNullOrEmpty(recurrenceRule)) {
            recurrence = Recurrence.parseRecurrence(startDate, endDate, recurrenceRule);
        }
        
        int rowIndex = 0;
        final int maxRecurrencesToCheckFreeBusy = getMaxRecurrencesToCheckFreeBusy();
        
        // loop through all attendees
        for (final String email : emails) {
            if (StringUtil.isNullOrEmpty(email)) {
                continue;
            }

            final JSONObject resource =
                    TimelineHelper.createAttendeeResource(timeline, email, rowIndex);
            resources.put(resource);

            try {              
                // create events for the start date
                timelineServiceHelper.createAttendeeEvents(startDate, startDate, uniqueId, timeline, timeZone,
                        email, rowIndex);

                // make a final variable
                final int currentIndex = rowIndex;
                final TimeZone localTimeZone = timeZone;
                // final container for counting occurrences
                final int[] counter = new int[] { 1 };

                if (recurrence instanceof AbstractIntervalPattern
                        && counter[0] < maxRecurrencesToCheckFreeBusy) {
                    final AbstractIntervalPattern pattern =
                            (AbstractIntervalPattern) recurrence;
                    pattern.loopThroughRepeats(new AbstractIntervalPattern.OccurrenceAction() {
                        // handle all occurrence events
                        public boolean handleOccurrence(final Date date)
                                throws ReservationException {
                            // create events for this date
                            timelineServiceHelper.createAttendeeEvents(date, date, uniqueId, timeline,
                                    localTimeZone, email, currentIndex);

                            return ++counter[0] < maxRecurrencesToCheckFreeBusy;
                        }

                    });
                }
            } catch (final ExceptionBase exception) {
                handleAttendeeFailure(email, exception, failures);
            }
            // next row
            rowIndex++;
        }
    }

    /**
     * Load room arrangement timeline.
     *
     * @param roomReservation the room reservation
     * @param startDate the start date
     * @param endDate the end date
     * @param recurrenceRule the recurrence rule
     * @param fixedResourceStandards the fixed resource standards
     * @param numberOfAttendees the number of attendees
     * @param externalsMustBeAllowed the externals must be allowed
     * @return the timeline object
     */
    private JSONObject loadRoomArrangementTimeline(final RoomReservation roomReservation, final Date startDate,
            final Date endDate, final String recurrenceRule, final List<String> fixedResourceStandards, 
            final Integer numberOfAttendees, final boolean externalsMustBeAllowed) {

        final JSONObject timeline = TimelineHelper.createTimeline(); 

        List<RoomArrangement> roomArrangements = null;   

        // this can be a new recurrence reservation
        Recurrence recurrence = null;

        if (StringUtil.notNullOrEmpty(recurrenceRule)) {
            recurrence = Recurrence.parseRecurrence(startDate, endDate, recurrenceRule);
            // search for available room arrangements for recurrence
            roomArrangements =
                    this.reservationService.findAvailableRoomsRecurrence(roomReservation,
                            numberOfAttendees, externalsMustBeAllowed, fixedResourceStandards, false,
                            recurrence, null);

        } else {
            // search for available room arrangements
            roomArrangements =
                    this.reservationService.findAvailableRooms(roomReservation, numberOfAttendees,
                            externalsMustBeAllowed, fixedResourceStandards, false, null);
        }

        final JSONArray resources = new JSONArray();
        final JSONArray events = new JSONArray();
        timeline.put(EVENTS, events);
        timeline.put(RESOURCES, resources);

        int rowIndex = 0; 

        final Integer reservationId = roomReservation.getReserveId();

        // loop through available rooms to find allocations for this day
        for (final RoomArrangement roomArrangement : roomArrangements) {
            final JSONObject resource =
                    TimelineHelper.createRoomArrangementResource(timeline, roomArrangement,
                            rowIndex);
            resources.put(resource);

            // create for the first occurrence
            timelineServiceHelper.createRoomAllocationEvents(startDate, reservationId, timeline, events, rowIndex,
                    roomArrangement);

            // make a final variable
            final int currentIndex = rowIndex;

            if (recurrence instanceof AbstractIntervalPattern) {
                final AbstractIntervalPattern pattern = (AbstractIntervalPattern) recurrence;
                pattern.loopThroughRepeats(new AbstractIntervalPattern.OccurrenceAction() {
                    // handle all occurrence events
                    public boolean handleOccurrence(final Date date) throws ReservationException {    
                        // this is a new recurrent reservation
                        timelineServiceHelper.createRoomAllocationEvents(date, reservationId, timeline, events,
                                currentIndex, roomArrangement);

                        return true;
                    }
                });
            }
            // next row
            rowIndex++;
        }

        return  timeline;
    } 


    /**
     * Load the room arrangement timeline when editing a recurrent reservation.
     *
     * @param roomReservation the room reservation
     * @param numberOfAttendees the number of attendees
     * @param externalsMustBeAllowed the externals must be allowed
     * @param fixedResourceStandards the fixed resource standards
     * @return the timeline object.
     */
    private JSONObject loadRoomArrangementTimeLineEditRecurrence(
            final RoomReservation roomReservation, final Integer numberOfAttendees, 
            final boolean externalsMustBeAllowed, final List<String> fixedResourceStandards) {

        final JSONObject timeline = TimelineHelper.createTimeline(); 

        if (roomReservation == null) {
            throw new ReservationException(RESERVATION_NOT_FOUND,
                TimelineService.class);
        }

        final Integer parentId = roomReservation.getParentId();                  
        final List<RoomReservation> existingOccurrences = this.roomReservationDataSource.getByParentId(
                parentId, null, roomReservation.getStartDate());            

        final List<RoomArrangement> roomArrangements = this.reservationService.findAvailableRooms(roomReservation,
                existingOccurrences, numberOfAttendees, externalsMustBeAllowed, fixedResourceStandards, false, null);

        final JSONArray resources = new JSONArray();
        final JSONArray events = new JSONArray();
        timeline.put(EVENTS, events);
        timeline.put(RESOURCES, resources);

        int rowIndex = 0; 

        for (final RoomArrangement roomArrangement : roomArrangements) {
            final JSONObject resource =
                    TimelineHelper.createRoomArrangementResource(timeline, roomArrangement,
                            rowIndex);
            resources.put(resource);

            for (RoomReservation existingReservation : existingOccurrences) {
                // for existing reservations, add the reservation id to ignore
                timelineServiceHelper.createRoomAllocationEvents(existingReservation.getStartDate(), 
                        existingReservation.getReserveId(), timeline, events, rowIndex, roomArrangement);
            } 

            // next row
            rowIndex++;
        }

        return timeline;
    }


    /**
     * Load the attendee time line when editing recurrent reservation.
     * 
     * @param timeline the time line JSON object
     * @param emails the emails of attendees
     * @param reservationId the reservation id
     * @param timeZone the time zone
     */
    private void loadAttendeeTimelineEditRecurrence(final JSONObject timeline,
            final List<String> emails, final Integer reservationId, final TimeZone timeZone) {

        final JSONArray resources = timeline.getJSONArray(RESOURCES);
        final JSONArray failures = timeline.getJSONArray(MESSAGE);

        final RoomReservation roomReservation = this.roomReservationDataSource.get(reservationId);
        if (roomReservation == null) {
            throw new ReservationException(RESERVATION_NOT_FOUND, TimelineService.class);
        } 

        final Integer parentId = roomReservation.getParentId();
        // Do not specify time zone here, just like the first reservation we only need to get
        // the time zone independent date and the unique id of each occurrence
        final List<RoomReservation> existingOccurrences =
                this.roomReservationDataSource.getByParentId(parentId, null,
                    roomReservation.getStartDate());
        
        int rowIndex = 0;
        final int maxRecurrencesToCheckFreeBusy = getMaxRecurrencesToCheckFreeBusy();
        
        // loop through all attendees
        for (final String email : emails) {
            // check for valid e-mail
            if (StringUtil.isNullOrEmpty(email)) {
                continue;
            }

            // create the resource
            final JSONObject resource =
                    TimelineHelper.createAttendeeResource(timeline, email, rowIndex);
            resources.put(resource);

            try {      
                // loop through all occurrences
                int occurrenceIndex = 0;
                for (RoomReservation existingReservation : existingOccurrences) { 
                    final String uniqueId = existingReservation.getUniqueId() == null
                            ? existingReservation.getReserveId().toString() : existingReservation.getUniqueId();

                    timelineServiceHelper.createAttendeeEvents(
                        existingReservation.getStartDate(), existingReservation.getStartDate(),
                        uniqueId, timeline, timeZone, email, rowIndex);
                    
                    if (++occurrenceIndex >= maxRecurrencesToCheckFreeBusy) {
                        break;
                    }
                }
            } catch (final ExceptionBase exception) {
                handleAttendeeFailure(email, exception, failures);
            }
            // next row
            rowIndex++;
        }
    }

    /**
     * Log a failure for retrieving availability of an attendee. 
     * @param email the email of the attendee that failed now
     * @param exception the error that occurred
     * @param failures the JSON object to store attendees for which retrieval failed
     */
    private void handleAttendeeFailure(final String email, final ExceptionBase exception,
            final JSONArray failures) {
        logger.debug("Could not retrieve availability info of " + email, exception);
        // skip this attendee, he could be external
        failures.put(email);
    }

    /**
     * Get the maximum number of occurrences to check when loading attendee availability.
     * 
     * @return maximum number of occurrences to check
     */
    private static int getMaxRecurrencesToCheckFreeBusy() {
        return com.archibus.service.Configuration.getActivityParameterInt(
            "AbWorkplaceReservations", "MaxRecurrencesToCheckFreeBusy", DEFAULT_MAX_OCCURRENCES);
    }

}
