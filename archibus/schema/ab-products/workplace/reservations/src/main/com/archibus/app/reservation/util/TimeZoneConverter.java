package com.archibus.app.reservation.util;

import java.sql.Time;
import java.util.*;

import org.apache.log4j.Logger;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.utility.*;

/**
 * Utility class. Provides methods to convert reservations to a given time zone.
 * <p>
 * 
 * Used by ReservationService.
 * 
 * @author Yorik Gerlo
 * @since 20.1
 * 
 */
public final class TimeZoneConverter {
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private TimeZoneConverter() {
    }
    
    /**
     * Gets the time zone. If the building id is null or the building doesn't have a time zone,
     * returns the default time zone of the server.
     * 
     * @param buildingId the building id
     * @return the time zone if of the building, or the default time zone id
     */
    public static String getTimeZoneIdForBuilding(final String buildingId) {
        String timeZoneId = null;
        if (StringUtil.isNullOrEmpty(buildingId)) {
            timeZoneId = TimeZone.getDefault().getID();
            Logger.getLogger(TimeZoneConverter.class).info(
                "No building ID specified, using default timezone.");
        } else {
            timeZoneId =
                    LocalDateTimeUtil.getLocationTimeZone(null, null, null, buildingId);
            if (timeZoneId == null) {
                timeZoneId = TimeZone.getDefault().getID();
                Logger.getLogger(TimeZoneConverter.class).info(
                    "Building '" + buildingId + "' has no time zone, using default timezone.");
            }
        }
        
        return timeZoneId;
    }

    /**
     * Change the time zone of the reservations to the time zone of the requestor. The dates and
     * times in the current reservation objects are modified to reflect the same absolute time as
     * before, but specified in the time zone of the requestor.
     * 
     * @param reservations the reservations to modify
     * @param timeZone the target time zone (time zone of the requestor)
     * @return modified reservations mapped according to (start)date
     */
    public static Map<Date, RoomReservation> toRequestorTimeZone(
            final List<RoomReservation> reservations, final String timeZone) {
        final Map<Date, RoomReservation> reservationMap =
                new HashMap<Date, RoomReservation>(reservations.size());
        for (final RoomReservation reservation : reservations) {
            // convert all to time zone of the requestor
            final String blId = reservation.getRoomAllocations().get(0).getBlId();
            final Date startDateTime =
                    Utility.toDatetime(reservation.getStartDate(), reservation.getStartTime());
            Date dateTime =
                    TimeZoneConverter.calculateDateTimeForBuilding(blId, startDateTime, timeZone,
                            false);

            final Date requestorStartDate = TimeZoneConverter.getDateValue(dateTime);
            final Time requestorStartTime = TimeZoneConverter.getTimeValue(dateTime);

            reservation.setStartDate(requestorStartDate);
            reservation.setStartTime(requestorStartTime);

            final Date endDateTime =
                    Utility.toDatetime(reservation.getEndDate(), reservation.getEndTime());
            dateTime =
                    TimeZoneConverter.calculateDateTimeForBuilding(blId, endDateTime, timeZone,
                            false);

            final Date requestorEndDate = TimeZoneConverter.getDateValue(dateTime);
            final Time requestorEndTime = TimeZoneConverter.getTimeValue(dateTime);

            reservation.setEndDate(requestorEndDate);
            reservation.setEndTime(requestorEndTime);

            reservationMap.put(requestorStartDate, reservation);
        }
        return reservationMap;
    }

    /**
     * Calculate date time for site.
     * 
     * @param siteId the site id
     * @param startDate the start date
     * @param startTime the start time
     * @param requestorTimeZoneId the requestor time zone id
     * @param isComingFrom true for requestor to site, false for site to requestor
     * @return the date
     */
    public static Date calculateDateTimeForSite(final String siteId, final Date startDate,
            final Time startTime, final String requestorTimeZoneId, final boolean isComingFrom) {
        final Date dateTime = Utility.toDatetime(startDate, startTime);
        return calculateDateTimeForSite(siteId, dateTime, requestorTimeZoneId, isComingFrom);
    }

    /**
     * Calculate date time for site.
     * 
     * @param siteId the site id
     * @param startDateTime the start date time
     * @param requestorTimeZoneId the requestor time zone id
     * @param isComingFrom true for requestor to site, false for site to requestor
     * @return the date
     */
    public static Date calculateDateTimeForSite(final String siteId, final Date startDateTime,
            final String requestorTimeZoneId, final boolean isComingFrom) {
        final String timeZone = LocalDateTimeUtil.getLocationTimeZone(null, null, siteId, null);
        return calculateDateTime(startDateTime, requestorTimeZoneId, timeZone, isComingFrom);
    }

    /**
     * Calculate date time for building.
     * 
     * @param blId the bl id
     * @param startDate the start date
     * @param startTime the start time
     * @param requestorTimeZoneId the requestor time zone id
     * @param isComingFrom true for requestor to building, false for building to requestor
     * @return the date
     */
    public static Date calculateDateTimeForBuilding(final String blId, final Date startDate,
            final Time startTime, final String requestorTimeZoneId, final boolean isComingFrom) {
        final Date dateTime = Utility.toDatetime(startDate, startTime);
        return calculateDateTimeForBuilding(blId, dateTime, requestorTimeZoneId, isComingFrom);
    }

    /**
     * Calculate date time for building.
     * 
     * @param blId the bl id
     * @param startDateTime the start date time
     * @param requestorTimeZoneId the requestor time zone id
     * @param isComingFrom true for requestor to building, false for building to requestor
     * @return the date
     */
    public static Date calculateDateTimeForBuilding(final String blId, final Date startDateTime,
            final String requestorTimeZoneId, final boolean isComingFrom) {

        final String timeZone = LocalDateTimeUtil.getLocationTimeZone(null, null, null, blId);
        return calculateDateTime(startDateTime, requestorTimeZoneId, timeZone, isComingFrom);
    }

    /**
     * Calculate requestor date time.
     * 
     * @param startDate the start date
     * @param startTime the start time
     * @param requestorTimeZoneId the requestor time zone id
     * @param isComingFrom true for UTC to requestor, false for requestor to UTC
     * @return the date
     */
    public static Date calculateRequestorDateTime(final Date startDate, final Time startTime,
            final String requestorTimeZoneId, final boolean isComingFrom) {

        final Date startDateTime = Utility.toDatetime(startDate, startTime);
        TimeZone requestorTimeZone = TimeZone.getDefault();

        if (StringUtil.notNullOrEmpty(requestorTimeZoneId)) {
            requestorTimeZone = TimeZone.getTimeZone(requestorTimeZoneId);
        }

        // offset in milliseconds
        final int requestorOffset = requestorTimeZone.getOffset(startDateTime.getTime());

        final Calendar cal = Calendar.getInstance();
        cal.setTime(startDateTime);

        if (isComingFrom) {
            cal.add(Calendar.MILLISECOND, requestorOffset);
        } else {
            cal.add(Calendar.MILLISECOND, -requestorOffset);
        }

        return cal.getTime();
    }

    /**
     * Calculate date time between two time zones.
     * 
     * For web interface reservations will always be in the time zone of the building/site.
     * Therefore the site is always required when making a search. The requestor time zone will be
     * the site time zone.
     * 
     * For reservations coming from Outlook, the requestor time zone will be GMT/UTC.
     * 
     * @param startDateTime the start date time
     * @param requestorTimeZoneId the requestor time zone id
     * @param cityTimeZoneId the city time zone id
     * @param isComingFrom true for requestor to building, false for building to requestor
     * 
     * @return the date
     */
    public static Date calculateDateTime(final Date startDateTime,
            final String requestorTimeZoneId, final String cityTimeZoneId,
            final boolean isComingFrom) {
        TimeZone cityTimeZone = null;

        if (StringUtil.notNullOrEmpty(cityTimeZoneId)) {
            cityTimeZone = TimeZone.getTimeZone(cityTimeZoneId);
        } else {
            // if the time zone is not defined for the building, we assume the time zone of the
            // server.
            cityTimeZone = TimeZone.getDefault();
        }

        // offset in milliseconds
        final int cityOffset = cityTimeZone.getOffset(startDateTime.getTime());

        TimeZone requestorTimeZone = null;

        if (StringUtil.notNullOrEmpty(requestorTimeZoneId)) {
            requestorTimeZone = TimeZone.getTimeZone(requestorTimeZoneId);
        } else {
            requestorTimeZone = TimeZone.getDefault();
        }

        // offset in milliseconds
        final int requestorOffset = requestorTimeZone.getOffset(startDateTime.getTime());

        final Calendar cal = Calendar.getInstance();
        cal.setTime(startDateTime);

        if (isComingFrom) {
            cal.add(Calendar.MILLISECOND, cityOffset);
            cal.add(Calendar.MILLISECOND, -requestorOffset);
        } else {
            cal.add(Calendar.MILLISECOND, -cityOffset);
            cal.add(Calendar.MILLISECOND, requestorOffset);
        }

        return cal.getTime();
    }

    /**
     * Gets the date value.
     * 
     * @param dateTime the date time
     * @return the date value
     */
    public static Date getDateValue(final Date dateTime) {
        Date result = null;
        if (dateTime != null) {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(dateTime);
            // Use the set() method to clear the hour value (see Calendar.clear() documentation).
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.clear(Calendar.MINUTE);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);

            result = cal.getTime(); 
        } 
        return result;
    }

    /**
     * Gets the time value.
     * 
     * @param dateTime the date time
     * @return the time value
     */
    public static Time getTimeValue(final Date dateTime) {
        Time result = null;
        if (dateTime != null) {
            final Calendar calStart = Calendar.getInstance();
            calStart.setTime(dateTime);
            calStart.set(Calendar.YEAR, Constants.INIT_YEAR);
            calStart.set(Calendar.MONTH, Calendar.DECEMBER);
            calStart.set(Calendar.DATE, Constants.INIT_DATE);

            result = new Time(calStart.getTimeInMillis());
        } 
        return result;
    }

}
