package com.archibus.app.reservation.exchange.service;

import java.util.*;

import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.Recurrence;
import com.archibus.app.reservation.exchange.util.*;
import com.archibus.app.reservation.util.StringTranscoder;
import com.archibus.utility.*;

/**
 * Helper class providing functionality for Exchange appointment handling.
 * 
 * Managed by Spring.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class AppointmentHelper {
    
    /** The time zone mapper. */
    private AppointmentTimeZoneMapper timeZoneMapper;
    
    /** The recurrence pattern converter. */
    private ExchangeRecurrenceConverter recurrenceConverter;
    
    /** The Exchange Service helper provides a connection to Exchange. */
    private ExchangeServiceHelper serviceHelper;
    
    /** The helper that provides access to the user properties used for ARCHIBUS reservations. */
    private AppointmentPropertiesHelper appointmentPropertiesHelper;
    
    /**
     * Bind to an existing appointment on a given user's calendar with the given unique id.
     * 
     * @param email the Exchange user's email address
     * @param iCalUid the unique id of the appointment in the user's Exchange calendar
     * @return the appointment object, or null if not found
     */
    public Appointment bindToAppointment(final String email, final String iCalUid) {
        try {
            final String encodedUid = StringTranscoder.transcodeHexToBase64(iCalUid);
            
            final SearchFilter filter =
                    new SearchFilter.IsEqualTo(
                        this.appointmentPropertiesHelper.getIcalUidPropertyDefinition(), encodedUid);
            
            Appointment result = null;
            
            final FindItemsResults<Item> results =
                    this.serviceHelper.initializeService(email).findItems(WellKnownFolderName.Calendar,
                        filter, new ItemView(1));
            final List<Item> items = results.getItems();
            if (!items.isEmpty() && items.get(0) instanceof Appointment) {
                result = (Appointment) items.get(0);
                result.load(this.appointmentPropertiesHelper.getExtendedPropertySet());
            }
            
            return result;
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error binding to appointment. Please refer to archibus.log for details.",
                exception, AppointmentHelper.class);
        }
    }
    
    /**
     * Bind to an appointment occurrence based on its start date and time in UTC.
     * 
     * @param email the Exchange user's email address
     * @param iCalUid the iCalendar UID of the appointment series
     * @param startDateTime the current start date and time of the appointment occurrence
     * @return the appointment, or null if not found
     */
    public Appointment bindToOccurrence(final String email, final String iCalUid,
            final Date startDateTime) {
        return this.bindToOccurrence(this.serviceHelper.initializeService(email), iCalUid,
            startDateTime);
    }
    
    /**
     * Bind to an appointment occurrence based on its start date and time in UTC.
     * 
     * @param exchangeService the service connected to the Exchange user's mailbox
     * @param iCalUid the iCalendar UID of the appointment series
     * @param startDateTime the current start date and time of the appointment occurrence
     * @return the appointment, or null if not found
     */
    public Appointment bindToOccurrence(final ExchangeService exchangeService,
            final String iCalUid, final Date startDateTime) {
        try {
            final FindItemsResults<Appointment> results =
                    exchangeService.findAppointments(WellKnownFolderName.Calendar,
                        new CalendarView(startDateTime, startDateTime));
            
            Appointment result = null;
            for (final Appointment appointment : results.getItems()) {
                appointment.load(this.appointmentPropertiesHelper.getExtendedPropertySet());
                if (appointment.getICalUid().equals(iCalUid)) {
                    result = appointment;
                    break;
                }
            }
            
            return result;
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error binding to appointment occurrence. Please refer to archibus.log for details",
                exception, AppointmentHelper.class);
        }
    }
    
    /**
     * Find all appointments on the specific user's calendar during the specified time period.
     * 
     * @param email user's email address
     * @param windowStart start of the time period (UTC)
     * @param windowEnd end of the time period (UTC)
     * @return list of appointments that occur during the time period
     * @throws CalendarException when the user doesn't exist on Exchange or any other error occurs
     */
    public List<Appointment> findAppointments(final String email, final Date windowStart,
            final Date windowEnd) throws CalendarException {
        try {
            // Use getService so it doesn't switch to the resource mailbox automatically.
            // This means an Exception will be thrown when the user doesn't exist.
            final FindItemsResults<Appointment> results =
                    this.serviceHelper.getService(email).findAppointments(
                        WellKnownFolderName.Calendar, new CalendarView(windowStart, windowEnd));
            
            for (final Appointment appointment : results.getItems()) {
                appointment.load(this.appointmentPropertiesHelper.getExtendedPropertySet());
            }
            
            return results.getItems();
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException("Error finding appointments for {0}.", exception,
                AppointmentHelper.class, email);
        }
    }

    /**
     * Creates the appointment object.
     * 
     * @param reservation the reservation to create an appointment for
     * @return the appointment
     */
    public Appointment createAppointment(final IReservation reservation) {
        try {
            Appointment appointment = null;
            // First look for an existing appointment with matching unique id.
            if (StringUtil.notNullOrEmpty(reservation.getUniqueId())) {
                appointment =
                        this.bindToAppointment(reservation.getEmail(), reservation.getUniqueId());
            }
            if (appointment == null) {
                // No matching appointment was found, so create a new one locally.
                appointment =
                        new Appointment(this.serviceHelper.initializeService(reservation.getEmail()));
            }
            return appointment;
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error creating appointment. Please refer to archibus.log for details", exception,
                AppointmentHelper.class);
        }
    }
    
    /**
     * Update the appointment object to reflect the properties of the calendar event.
     * 
     * @param reservation the calendar event
     * @param appointment the appointment to update
     */
    public void updateAppointment(final IReservation reservation, final Appointment appointment) {
        try {
            // Get the correct Windows Time Zone ID based on calendar event time zone.
            this.timeZoneMapper.setTimeZone(appointment, reservation.getTimeZone());
            TimePeriod timePeriodUtc = null;
            
            if (reservation instanceof RoomReservation) {
                final RoomReservation roomReservation = (RoomReservation) reservation;
                final List<RoomAllocation> allocations = roomReservation.getRoomAllocations();
                
                appointment.setLocation(allocations.get(0).getLocation());
                timePeriodUtc = roomReservation.getTimePeriodInTimeZone(Constants.TIMEZONE_UTC);
            } else {
                timePeriodUtc = reservation.getTimePeriod();
            }
            
            appointment.setSubject(reservation.getReservationName());
            appointment.setBody(ExchangeObjectHelper.newMessageBody(reservation.getComments()));
            appointment.setStart(timePeriodUtc.getStartDateTime());
            appointment.setEnd(timePeriodUtc.getEndDateTime());
            AttendeesHelper.setAttendees(reservation, appointment,
                this.serviceHelper.getResourceAccount(), this.serviceHelper.getOrganizerAccount());
            
            // Set the reservation properties and recurrence.
            final Recurrence recurrence = reservation.getRecurrence();
            if (recurrence == null) {
                if (reservation.getReserveId() == null) {
                    // @translatable
                    throw new ReservationException(
                        "A non-recurring appointment needs one reservation id",
                        AppointmentHelper.class);
                } else {
                    this.appointmentPropertiesHelper.setReservationId(appointment,
                        reservation.getReserveId());
                }
            } else {
                if (appointment.isNew()) {
                    // Only set the recurrence pattern for new appointments.
                    // We do not support changing the recurrence pattern from Web Central.
                    appointment.setRecurrence(this.recurrenceConverter
                        .convertToExchangeRecurrence(recurrence));
                    
                    this.appointmentPropertiesHelper.setRecurringReservationIds(appointment,
                        (RoomReservation) reservation);
                }
                this.appointmentPropertiesHelper.removeReservationId(appointment);
            }
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error setting appointment properties. Please refer to archibus.log for details",
                exception, AppointmentHelper.class);
        }
    }
    
    /**
     * Sets the time zone mapper.
     * 
     * @param timeZoneMapper the new time zone mapper
     */
    public void setTimeZoneMapper(final AppointmentTimeZoneMapper timeZoneMapper) {
        this.timeZoneMapper = timeZoneMapper;
    }
    
    /**
     * Set the Recurrence converter.
     * 
     * @param exchangeRecurrenceConverter the new recurrence converter
     */
    public void setRecurrenceConverter(final ExchangeRecurrenceConverter exchangeRecurrenceConverter) {
        this.recurrenceConverter = exchangeRecurrenceConverter;
    }
    
    /**
     * Sets the appointment properties helper.
     * 
     * @param appointmentPropertiesHelper the new appointment properties helper
     */
    public void setAppointmentPropertiesHelper(
            final AppointmentPropertiesHelper appointmentPropertiesHelper) {
        this.appointmentPropertiesHelper = appointmentPropertiesHelper;
    }
    
    /**
     * Sets the service helper.
     * 
     * @param serviceHelper the new service helper
     */
    public void setServiceHelper(final ExchangeServiceHelper serviceHelper) {
        this.serviceHelper = serviceHelper;
    }
    
    /**
     * Get the appointment properties helper.
     * 
     * @return the appointment properties helper
     */
    public AppointmentPropertiesHelper getAppointmentPropertiesHelper() {
        return this.appointmentPropertiesHelper;
    }
    
    /**
     * Get the Exchange Service Helper.
     * 
     * @return the service helper
     */
    public ExchangeServiceHelper getServiceHelper() {
        return this.serviceHelper;
    }
    
    /**
     * Get the recurrence converter.
     * 
     * @return the recurrence converter
     */
    public ExchangeRecurrenceConverter getRecurrenceConverter() {
        return this.recurrenceConverter;
    }
    
}
