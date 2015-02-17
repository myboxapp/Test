package com.archibus.app.reservation.exchange.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.CalendarEvent;
import com.archibus.app.reservation.domain.ICalendarEvent;
import com.archibus.app.reservation.util.TimeZoneConverter;

/**
 * Utility class. Provides methods to convert availability information from Exchange format to
 * WebCentral format and to convert other data types.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public final class ExchangeObjectHelper {
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private ExchangeObjectHelper() {
        super();
    }
    
    /**
     * Convert a list of appointments to availability information.
     * 
     * @param appointments the list of appointments to convert
     * @param requestedTimeZone the time zone for the event list to return
     * @param start start of the time period
     * @param end end of the time period
     * @return list of calendar events for availability information
     * @throws ServiceLocalException when an error occurs reading the appointment properties
     */
    public static List<ICalendarEvent> convertAvailability(final List<Appointment> appointments,
            final TimeZone requestedTimeZone, final Date start, final Date end)
            throws ServiceLocalException {
        final List<ICalendarEvent> events = new ArrayList<ICalendarEvent>();
        for (final Appointment appointment : appointments) {
            final LegacyFreeBusyStatus status = appointment.getLegacyFreeBusyStatus();
            if (LegacyFreeBusyStatus.Free.equals(status)) {
                continue;
            }
            
            // dates are received in UTC and should be converted
            final Date startDateTime =
                    TimeZoneConverter.calculateDateTime(appointment.getStart(),
                        Constants.TIMEZONE_UTC, requestedTimeZone.getID(), true);
            final Date endDateTime =
                    TimeZoneConverter.calculateDateTime(appointment.getEnd(),
                        Constants.TIMEZONE_UTC, requestedTimeZone.getID(), true);
            
            // Ignore events that occur outside the requested range
            // in the requested time zone.
            if (startDateTime.after(end) || endDateTime.before(start)) {
                continue;
            }
            
            // create an ARCHIBUS calendar event
            final ICalendarEvent event = new CalendarEvent();
            
            event.setEventId(appointment.getICalUid());
            event.setSubject(appointment.getSubject());
            event.setLocation(appointment.getLocation());
            
            // KB 3041357 this property is not always available on Exchange 2007.
            // event.setRecurrent(appointment.getIsRecurring());
            event.setStatus(status.toString());
            
            event.setStartDate(startDateTime);
            event.setEndDate(endDateTime);
            event.setStartTime(new java.sql.Time(startDateTime.getTime()));
            event.setEndTime(new java.sql.Time(endDateTime.getTime()));
            event.setTimeZone(requestedTimeZone.getID());
            
            events.add(event);
        }
        return events;
    }

    /**
     * Create a new plain text message body.
     * 
     * @param body the message body text
     * @return the message body
     */
    public static MessageBody newMessageBody(final String body) {
        return new MessageBody(BodyType.Text, body);
    }
}
