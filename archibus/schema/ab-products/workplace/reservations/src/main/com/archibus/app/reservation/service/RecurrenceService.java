package com.archibus.app.reservation.service;

import java.util.*;

import com.archibus.app.common.recurring.RecurringScheduleService;
import com.archibus.app.reservation.dao.datasource.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.DataSource;
import com.archibus.datasource.data.DataRecord;


/**
 * The Class RecurrenceService.
 */
public class RecurrenceService {
    
    /**
     * Constant: Default Maximum Number of Occurrences for recurring reservations if not defined in
     * the activity parameter.
     */
    private static final int DEFAULT_MAX_OCCURRENCES = 500;

    /** The Constant RESERVE_DATE_START. */
    private static final String RESERVE_DATE_START = "reserve.date_start";

    /** The Constant RESERVE_DATE_END. */
    private static final String RESERVE_DATE_END = "reserve.date_end";
    
    /** The Constant RESERVE_RES_PARENT. */
    private static final String RESERVE_RES_PARENT = "reserve.res_parent";
    
    /** The Constant RESERVE_COMMENTS. */
    private static final String RESERVE_COMMENTS = "reserve.comments";

    /** The room reservation data source. */
    private RoomReservationDataSource roomReservationDataSource;

    /**
     * Get the start and end date of the recurrent reservation and the number of occurrences.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param recurringRule the recurring rule
     * @param parentId the parent id if this is for an existing recurrence series
     * @return the first and last date of the recurrent reservation and the number of occurrences
     */
    public DataRecord getFirstAndLastDate(final Date startDate, final Date endDate,
            final String recurringRule, final Integer parentId) {

        Date firstDate = null;
        Date lastDate = null;
        Integer totalOccurrences = null;

        final RecurringScheduleService recurringScheduleService = newRecurringScheduleService();
        if (parentId == null || parentId == 0) {
            final List<Date> dateList =
                    cropDateList(recurringScheduleService.getDatesList(startDate, endDate,
                        recurringRule));

            if (dateList.isEmpty()) { 
                // @translatable
                throw new ReservationException(
                    "The recurrence pattern does not yield any occurrences for your current selection. Please review the pattern.",
                    RecurrenceService.class);
            }
            firstDate = dateList.get(0);
            totalOccurrences = dateList.size();
            lastDate = dateList.get(totalOccurrences - 1);
        } else {
            firstDate = startDate;
            this.roomReservationDataSource.addSort(
                    this.roomReservationDataSource.getMainTableName(), Constants.DATE_START_FIELD_NAME,
                    DataSource.SORT_DESC);
            final List<RoomReservation> existingReservations =
                    this.roomReservationDataSource.getByParentId(parentId, null, startDate);
            if (existingReservations.isEmpty()) {
                // @translatable
                throw new ReservationException("No reservations found with parent id {0}.",
                    RecurrenceService.class, parentId);
            }
            totalOccurrences = existingReservations.size();
            lastDate = existingReservations.get(0).getStartDate();
            
            recurringScheduleService
                .setRecurringSchedulePattern(firstDate, lastDate, recurringRule);
        }

        final DataRecord record = this.roomReservationDataSource.createNewRecord();
        record.setValue(RESERVE_DATE_START, firstDate);
        record.setValue(RESERVE_DATE_END, lastDate);
        record.setValue(RESERVE_RES_PARENT, totalOccurrences);
        
        // Create a modified recurring rule for getting a description with an accurate number of
        // occurrences, even when editing part of the recurrence series.
        final String modifiedRecurringRule =
                RecurringScheduleService.getRecurrenceXMLPattern(
                    recurringScheduleService.getRecurringType(),
                    recurringScheduleService.getInterval(), totalOccurrences,
                    recurringScheduleService.getDaysOfWeek(),
                    recurringScheduleService.getDayOfMonth(),
                    recurringScheduleService.getWeekOfMonth(),
                    recurringScheduleService.getMonthOfYear());

        record.setValue(RESERVE_COMMENTS,
            recurringScheduleService.getRecurringPatternDescription(modifiedRecurringRule));

        return record;
    }
    
    /**
     * For a single occurrence in a recurring reservation, get the minimum and maximum date allowed
     * while not skipping over another occurrence.
     * 
     * @param reservationId id of the reservation to get the min and max date for
     * @param parentId parent reservation id
     * @return data record containing the min and max date as reserve.date_start and
     *         reserve.date_end
     */
    public DataRecord getMinAndMaxDate(final Integer reservationId, final Integer parentId) {
        Date minDate = null;
        Date maxDate = null;
        
        final List<RoomReservation> reservations =
                this.roomReservationDataSource.getByParentId(parentId, null, null);
        int index = 0;
        // find the previous reservation
        while (index < reservations.size()
                && !reservations.get(index).getReserveId().equals(reservationId)) {
            minDate = reservations.get(index).getStartDate();
            ++index;
        }
        // move past the current reservation
        ++index;
        // the next reservation determines the max date
        if (index < reservations.size()) {
            maxDate = reservations.get(index).getStartDate();
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(maxDate);
            calendar.add(Calendar.DATE, -1);
            maxDate = calendar.getTime();
        }
        if (minDate != null) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(minDate);
            calendar.add(Calendar.DATE, 1);
            minDate = calendar.getTime();
        }
        
        final DataRecord record = this.roomReservationDataSource.createNewRecord();
        record.setValue(RESERVE_DATE_START, minDate);
        record.setValue(RESERVE_DATE_END, maxDate);
        
        return record;
    }

    /**
     * Gets the date list.
     *
     * @param reservation the reservation
     * @param recurrenceRule the recurrence rule
     * @return the date list
     */
    public static List<Date> getDateList(final DataRecord reservation,
            final String recurrenceRule) {
        return getDateList(reservation.getDate(RESERVE_DATE_START),
            reservation.getDate(RESERVE_DATE_END), recurrenceRule);
    }
    
    /**
     * Gets the date list.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param recurrenceRule the recurrence rule
     * @return the date list
     */
    public static List<Date> getDateList(final Date startDate, final Date endDate,
            final String recurrenceRule) {
        final RecurringScheduleService recurringScheduleService = newRecurringScheduleService();

        final List<Date> dateList =
                recurringScheduleService.getDatesList(startDate, endDate, recurrenceRule);
        return cropDateList(dateList);
    }
    
    /**
     * Crop the date list for the maximum number of occurrences in reservations.
     * 
     * @param dateList the list of dates
     * @return the cropped list of dates
     */
    private static List<Date> cropDateList(final List<Date> dateList) {
        final int maxOccurrences = getMaxOccurrences();
        List<Date> result;
        if (dateList.size() > maxOccurrences) {
            result = dateList.subList(0, maxOccurrences);
        } else {
            result = dateList;
        }
        return result;
    }
    
    /**
     * Initialize the common recurring schedule service without year-based limits.
     * 
     * @return recurring schedule service instance
     */
    public static RecurringScheduleService newRecurringScheduleService() {
        final RecurringScheduleService recurringScheduleService = new RecurringScheduleService();
        recurringScheduleService.setSchedulingLimits(-1, -1, -1, -1);
        return recurringScheduleService;
    }
    
    /**
     * Get the maximum number of occurrences for a recurring reservation.
     * 
     * @return the maximum number of occurrences.
     */
    public static int getMaxOccurrences() {
        return com.archibus.service.Configuration.getActivityParameterInt(
            "AbWorkplaceReservations", "MaxRecurrencesToCreate", DEFAULT_MAX_OCCURRENCES);
    }

    /**
     * Sets the room reservation data source .
     *
     * @param roomReservationDataSource the new room reservation data source
     */
    public void setRoomReservationDataSource(
            final RoomReservationDataSource roomReservationDataSource) {
        this.roomReservationDataSource = roomReservationDataSource;
    }

}
