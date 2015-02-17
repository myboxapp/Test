package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.util.*;

import com.archibus.app.reservation.dao.IRoomArrangementDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.util.*;
import com.archibus.context.*;
import com.archibus.datasource.DataSource;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.utility.*;

/**
 * The Class RoomArrangementDataSource.
 * 
 * @author Bart Vanderschoot
 */
public class RoomArrangementDataSource extends AbstractReservableDataSource<RoomArrangement>
        implements IRoomArrangementDataSource {
    
    /**
     * Instantiates a new room arrangement data source.
     */
    public RoomArrangementDataSource() {
        this("roomArrangement", "rm_arrange");
    }
    
    /**
     * Instantiates a new room arrangement data source.
     * 
     * @param beanName the bean name
     * @param tableName the table name
     */
    protected RoomArrangementDataSource(final String beanName, final String tableName) {
        super(beanName, tableName);
        // join with room table
        this.addTable(Constants.ROOM_TABLE, DataSource.ROLE_STANDARD);
        // add the name of the room
        this.addField(Constants.ROOM_TABLE, Constants.NAME_FIELD_NAME);
        // add the reservable property of the room
        this.addField(Constants.ROOM_TABLE, Constants.RESERVABLE_FIELD_NAME);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Suppress warning avoid final local variable.
     * <p>
     * Justification: Turning local variable into field is not useful
     */ 
    public final List<DataRecord> findAvailableRoomRecords(
            final RoomReservation receivedReservation, final Integer numberAttendees,
            final boolean externalAllowed, final List<String> fixedResourceStandards,
            final boolean allDayEvent) throws ReservationException {
        
        RoomReservation reservation = receivedReservation;
        final List<RoomAllocation> rooms = receivedReservation.getRoomAllocations();
        
        if (rooms == null || rooms.isEmpty()) {
            // @translatable
            throw new ReservationException("No rooms in reservation", RoomArrangementDataSource.class);
        }
        
        final RoomAllocation roomAllocation = rooms.get(0);
        
        if (StringUtil.notNullOrEmpty(reservation.getTimeZone()) && !allDayEvent) {
            reservation = createReservation(receivedReservation, reservation, roomAllocation);
        }
        
        List<DataRecord> results = null;
        if (reservation.getEndDate() == null
                || reservation.getStartDate().equals(reservation.getEndDate())
                || (allDayEvent && (reservation.getTimePeriod().getDaysDifference() == 1.0))) {
            results =
                    findAvailableRoomRecordsInLocalTime(reservation, roomAllocation,
                        numberAttendees, externalAllowed, fixedResourceStandards, allDayEvent);
        } else {
            // Don't return any results if the reservation spans multiple days.
            results = new ArrayList<DataRecord>(0);
        }
        return results;
    }

    
    /**
     * Find available rooms for the specified reservation, which is already in the local time zone
     * of the building.
     * 
     * @param reservation the reservation in the time zone of the building
     * @param roomAllocation domain object representing the location restrictions
     * @param numberAttendees number of attendees
     * @param externalAllowed whether to return only rooms that allow external visitors
     * @param fixedResourceStandards fixed resource standards
     * @param allDayEvent true to look for rooms available for all day events
     * @return the list of results
     */
    private List<DataRecord> findAvailableRoomRecordsInLocalTime(final RoomReservation reservation,
            final RoomAllocation roomAllocation, final Integer numberAttendees,
            final boolean externalAllowed, final List<String> fixedResourceStandards,
            final boolean allDayEvent) {
        // since the remote service is a singleton Spring bean and data sources prototypes, we
        // create copy
        final DataSource dataSource = this.createCopy();
        
        Date localCurrentDate = TimePeriod.clearTime(Utility.currentDate());
        Time localCurrentTime = Utility.currentTime();
        
        if (StringUtil.notNullOrEmpty(roomAllocation.getBlId())) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, Constants.BL_ID_FIELD_NAME,
                roomAllocation.getBlId()));
            // get the current date and time of the building location
            localCurrentDate =
                    TimePeriod.clearTime(LocalDateTimeUtil.currentLocalDate(null, null, null,
                        roomAllocation.getBlId()));
            localCurrentTime =
                    new Time(LocalDateTimeUtil.currentLocalTime(null, null, null,
                        roomAllocation.getBlId()).getTime());
            
        }
        if (StringUtil.notNullOrEmpty(roomAllocation.getFlId())) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, Constants.FL_ID_FIELD_NAME,
                roomAllocation.getFlId()));
        }
        if (StringUtil.notNullOrEmpty(roomAllocation.getRmId())) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, Constants.RM_ID_FIELD_NAME,
                roomAllocation.getRmId()));
        }
        
        if (StringUtil.notNullOrEmpty(roomAllocation.getConfigId())) {
            dataSource.addRestriction(Restrictions.eq(this.tableName,
                Constants.CONFIG_ID_FIELD_NAME, roomAllocation.getConfigId()));
        }
        if (StringUtil.notNullOrEmpty(roomAllocation.getArrangeTypeId())) {
            dataSource.addRestriction(Restrictions.eq(this.tableName,
                Constants.RM_ARRANGE_TYPE_ID_FIELD_NAME, roomAllocation.getArrangeTypeId()));
        }
        
        // see if they are reservable (KB#3035993)
        dataSource.addRestriction(Restrictions.eq(this.tableName, Constants.RESERVABLE_FIELD_NAME,
            1));
        // also see if they are reservable in rm (KB#3036598)
        dataSource.addRestriction(Restrictions.eq(Constants.ROOM_TABLE,
            Constants.RESERVABLE_FIELD_NAME, 1));
        
        this.log.debug("Local current date " + localCurrentDate);
        this.log.debug("Local current time " + localCurrentTime);
        
        addRestrictions(dataSource, reservation, localCurrentDate, localCurrentTime, allDayEvent,
            externalAllowed);
        
        // extra
        addNumberOfAttendeesRestriction(numberAttendees, dataSource);
        RoomArrangementDataSourceRestrictionsHelper.addFixedResourcesRestriction(
            fixedResourceStandards, dataSource);
        
        // sort on building, default arrangement and capacity first
        dataSource.addSort(this.tableName, "bl_id", DataSource.SORT_ASC);
        dataSource.addSort(this.tableName, "is_default", DataSource.SORT_DESC);
        dataSource.addSort(this.tableName, Constants.MAX_CAPACITY_FIELD_NAME, DataSource.SORT_ASC);

        // then sort on floor and room
        dataSource.addSort(this.tableName, "fl_id", DataSource.SORT_ASC);
        dataSource.addSort(this.tableName, "rm_id", DataSource.SORT_ASC);
        
        // finally sort on arrangement and configuration for each room
        dataSource.addSort(this.tableName, "rm_arrange_type_id", DataSource.SORT_ASC);
        dataSource.addSort(this.tableName, "config_id", DataSource.SORT_ASC);
        
        return dataSource.getRecords();
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<RoomArrangement> findAvailableRooms(final RoomReservation reservation,
            final Integer numberAttendees, final boolean externalAllowed,
            final List<String> fixedResourceStandards, final boolean allDayEvent,
            final String timeZone) throws ReservationException {
        final List<RoomArrangement> results =
                convertRecordsToObjects(this.findAvailableRoomRecords(reservation, numberAttendees,
                    externalAllowed, fixedResourceStandards, allDayEvent));
        
        // Convert the dayStart and dayEnd properties to the requested time zone.
        if (StringUtil.notNullOrEmpty(timeZone)) {
            final Date now = new Date();
            for (final RoomArrangement arrangement : results) {
                if (arrangement.getDayStart() != null) {
                    arrangement.setDayStart(new Time(TimeZoneConverter
                        .calculateDateTimeForBuilding(arrangement.getBlId(), now,
                            arrangement.getDayStart(), timeZone, false).getTime()));
                }
                if (arrangement.getDayEnd() != null) {
                    arrangement.setDayEnd(new Time(TimeZoneConverter.calculateDateTimeForBuilding(
                        arrangement.getBlId(), now, arrangement.getDayEnd(), timeZone, false)
                        .getTime()));
                }
            }
        }
        
        return results;
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<RoomArrangement> findAvailableRooms(final String blId, final String flId,
            final String rmId, final String arrangeTypeId, final TimePeriod timePeriod,
            final Integer numberAttendees, final List<String> fixedResourceStandards)
            throws ReservationException {
        // Create the corresponding domain objects for the query.
        final RoomArrangement roomArrangement =
                new RoomArrangement(blId, flId, rmId, null, arrangeTypeId);
        final RoomReservation reservation = new RoomReservation(timePeriod, roomArrangement);
        
        return this.findAvailableRooms(reservation, numberAttendees, false, fixedResourceStandards,
            false, null);
    }
    
    /**
     * {@inheritDoc}
     */
    public final RoomArrangement get(final String blId, final String flId, final String rmId,
            final String configId, final String arrangeTypeId) {
        
        final DataSource dataSource = this.createCopy();
        
        dataSource
            .addRestriction(Restrictions.eq(this.tableName, Constants.BL_ID_FIELD_NAME, blId));
        dataSource
            .addRestriction(Restrictions.eq(this.tableName, Constants.FL_ID_FIELD_NAME, flId));
        dataSource
            .addRestriction(Restrictions.eq(this.tableName, Constants.RM_ID_FIELD_NAME, rmId));
        dataSource.addRestriction(Restrictions.eq(this.tableName, Constants.CONFIG_ID_FIELD_NAME,
            configId));
        dataSource.addRestriction(Restrictions.eq(this.tableName,
            Constants.RM_ARRANGE_TYPE_ID_FIELD_NAME, arrangeTypeId));
        
        final DataRecord record = dataSource.getRecord();
        RoomArrangement arrangement = null;
        if (record != null) {
            arrangement = convertRecordToObject(record);
        }
        return arrangement;
    }
    
    /**
     * Adds the number of attendees restriction.
     * 
     * @param numberAttendees the number attendees
     * @param dataSource the ds
     */
    protected final void addNumberOfAttendeesRestriction(final Integer numberAttendees,
            final DataSource dataSource) {
        if (numberAttendees != null) {
            dataSource.addRestriction(Restrictions.gte(this.tableName,
                Constants.MAX_CAPACITY_FIELD_NAME, numberAttendees));
            
            // Do not add the min_required restriction for Reservation Manager and Reservation
            // Service Desk members.
            final User user = ContextStore.get().getUser();
            if (!user.isMemberOfGroup(Constants.RESERVATION_SERVICE_DESK)
                    && !user.isMemberOfGroup(Constants.RESERVATION_MANAGER)) {
                dataSource.addRestriction(Restrictions.lte(this.tableName, "min_required",
                    numberAttendees));
            }
        }
    }
    
    /**
     * Adds the restrictions.
     * 
     * @param dataSource the data source
     * @param reservation the reservation
     * @param localCurrentDate the local current date
     * @param localCurrentTime the local current time
     * @param allDayEvent true to look for rooms available for all day events
     * @param externalAllowed whether to retrict the results to show only rooms that allow external
     *            guests
     * @throws ReservationException the reservation exception
     */
    protected final void addRestrictions(final DataSource dataSource,
            final IReservation reservation, final Date localCurrentDate,
            final Time localCurrentTime, final boolean allDayEvent, final boolean externalAllowed)
            throws ReservationException {
        
        addTimePeriodParameters(dataSource, reservation.getTimePeriod());
        // checks free busy of rooms
        addTimeRestriction(dataSource, reservation);
        // add restriction for announce days
        addAnnounceRestriction(dataSource, reservation.getTimePeriod(), localCurrentDate,
            localCurrentTime);
        // add restriction for maximum days ahead
        addMaxDayAheadRestriction(dataSource, reservation.getTimePeriod(), localCurrentDate);
        // add restriction for security groups
        addSecurityRestriction(dataSource);
        
        if (!allDayEvent) {
            // add pre and post- block start/end date
            addDayStartEndRestriction(dataSource, reservation.getTimePeriod(), false);
        }
        
        this.addExternalAllowedRestriction(dataSource, reservation, externalAllowed);
        
    }
    
    /**
     * Adds the time restriction.
     * 
     * @param dataSource the data source
     * @param reservation the reservation
     */
    protected final void addTimeRestriction(final DataSource dataSource,
            final IReservation reservation) {
        Integer reserveId = Integer.valueOf(0);
        Date startDate = null;
        Time startTime = null;
        Time endTime = null;
        if (reservation != null) {
            reserveId = reservation.getReserveId();
            startDate = reservation.getStartDate();
            startTime = reservation.getStartTime();
            endTime = reservation.getEndTime();
        }
        
        RoomArrangementDataSourceRestrictionsHelper.addTimeRestriction(startDate, startTime,
            endTime, reserveId, dataSource);
    }
    
    /**
     * Add external visitors allowed.
     * 
     * @param dataSource the data source
     * @param reservation the reservation
     * @param externalAllowed external visitors allowed.
     */
    protected final void addExternalAllowedRestriction(final DataSource dataSource,
            final IReservation reservation, final boolean externalAllowed) {
        // select only the room arrangements that are external visitors allowed, when
        // externalAllowed is specified.
        boolean addRestriction = externalAllowed;
        
        // KB#3035994
        // add restriction for rooms allowing external attendees when at least one attendee is
        // external
        if (!addRestriction && StringUtil.notNullOrEmpty(reservation.getAttendees())) {
            final String[] attendees = reservation.getAttendees().split(";");
            for (final String attendeeEmail : attendees) {
                if (!DataSourceUtils.isEmployeeEmail(attendeeEmail)) {
                    addRestriction = true;
                    break;
                }
            }
        }
        
        if (addRestriction) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, "external_allowed", 1));
        }
    }
    
    /**
     * Create fields to properties mapping. To be compatible with version 19.
     * 
     * @return mapping
     */
    @Override
    protected final Map<String, String> createFieldToPropertyMapping() {
        final Map<String, String> mapping = super.createFieldToPropertyMapping();
        
        mapping.put(this.tableName + ".bl_id", "blId");
        mapping.put(this.tableName + ".fl_id", "flId");
        mapping.put(this.tableName + ".rm_id", "rmId");
        
        mapping.put(this.tableName + ".config_id", "configId");
        mapping.put(this.tableName + ".rm_arrange_type_id", "arrangeTypeId");
        
        mapping.put(this.tableName + ".min_required", "minRequired");
        mapping.put(this.tableName + ".max_capacity", "maxCapacity");
        
        mapping.put(this.tableName + ".res_stds_not_allowed", "standardsNotAllowed");
        
        mapping.put(this.tableName + ".external_allowed", "externalAllowed");
        
        // add the name of the room
        mapping.put("rm.name", Constants.NAME_FIELD_NAME);
        
        return mapping;
    }
    
    
    /**
     * Configure reservation.
     *
     * @param receivedReservation the received reservation 
     * @param reservation the reservation
     * @param roomAllocation the room allocation
     * @return the room reservation
     */
    private RoomReservation createReservation(
            final RoomReservation receivedReservation, final RoomReservation reservation,
            final RoomAllocation roomAllocation) {

        // No adjustment for all-day events.

        // For timezone adjustment, building id is required.
        if (StringUtil.isNullOrEmpty(roomAllocation.getBlId())) {
            // @translatable
            throw new ReservationException("No building specified",
                    RoomArrangementDataSource.class);
        }

        // adjust the reservation date/time for the time zone of the requestor
        final Date startDateTime =
                TimeZoneConverter.calculateDateTimeForBuilding(roomAllocation.getBlId(),
                        reservation.getStartDate(), reservation.getStartTime(),
                        reservation.getTimeZone(), true);

        // adjust the reservation date/time for the time zone of the requestor
        final Date endDateTime =
                TimeZoneConverter.calculateDateTimeForBuilding(roomAllocation.getBlId(),
                        reservation.getEndDate(), reservation.getEndTime(),
                        reservation.getTimeZone(), true);

        // create a new reservation object, don't modify the one passed in
        final RoomReservation roomReservation = new RoomReservation(reservation.getReserveId());
        final TimePeriod period = new TimePeriod();
        period.setStartDateTime(startDateTime);
        period.setEndDateTime(endDateTime);
        roomReservation.setTimePeriod(period);
        roomReservation.addRoomAllocation(roomAllocation);
        roomReservation.setAttendees(receivedReservation.getAttendees());

        return roomReservation;
    }

    
}
