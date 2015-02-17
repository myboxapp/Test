package com.archibus.app.reservation.exchange.service;

import java.util.*;

import microsoft.exchange.webservices.data.*;

import org.apache.log4j.Logger;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.AttendeeResponseStatus.ResponseStatus;
import com.archibus.app.reservation.exchange.util.*;
import com.archibus.app.reservation.service.ICalendarService;
import com.archibus.utility.*;

/**
 * Provides Calendar information from a Exchange Service. Represents services to create and update
 * appointments and find free/busy.
 * <p>
 * 
 * Managed by Spring
 * 
 * @author Bart Vanderschoot
 * @since 21.2
 * 
 */
public class ExchangeCalendarService implements ICalendarService {
    
    /** Error message indicating an appointment occurrence could not be updated. */
    // @translatable
    private static final String ERROR_UPDATING_OCCURRENCE =
            "Error updating appointment occurrence. Please refer to archibus.log for details";
    
    /** The Constant HOURS_24. */
    private static final int HOURS_24 = 24;
    
    /** The logger. */
    private final Logger logger = Logger.getLogger(this.getClass());
    
    /** The helper for handling Exchange Appointment. */
    private AppointmentHelper appointmentHelper;
    
    /** Service that creates and sends translated reservation messages. */
    private ExchangeMessagesService exchangeMessagesService;
    
    /** Exchange service for reusing a connection. */
    private ExchangeService cachedExchangeService;
    
    /** Email address for whom the cached connection was created. */
    private String cachedOrganizerEmail;

    /** {@inheritDoc} */
    public void checkServiceAvailable() throws ExceptionBase {
        // try to connect to the Exchange server using the organizer account
        appointmentHelper.getServiceHelper().initializeService(
            appointmentHelper.getServiceHelper().getOrganizerAccount());
    }
    
    /** {@inheritDoc} */
    public List<ICalendarEvent> findAttendeeAvailability(final Date startDate, final Date endDate,
            final TimeZone requestedTimeZone, final String email) throws ExceptionBase {
        
        // Create a time window from 0:00 on the startDate until 0:00 on the day after the endDate.
        // Adjust for the requested time zone to get a UTC time window.
        final Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.MILLISECOND, requestedTimeZone.getOffset(startDate.getTime()));
        final Date windowStart = cal.getTime();
        cal.setTime(endDate);
        cal.add(Calendar.HOUR, HOURS_24);
        cal.add(Calendar.MILLISECOND, requestedTimeZone.getOffset(cal.getTimeInMillis()));
        final Date windowEnd = cal.getTime();
        
        try {
            final List<Appointment> appointments =
                    this.appointmentHelper.findAppointments(email, windowStart, windowEnd);
            return ExchangeObjectHelper.convertAvailability(appointments, requestedTimeZone,
                windowStart, windowEnd);
        } catch (final ServiceLocalException exception) {
            throw new CalendarException("Could not retrieve availability of {0}",
                exception, ExchangeCalendarService.class, email);
        }
    }
    
    /** {@inheritDoc} */
    public String createAppointment(final IReservation reservation) throws ExceptionBase {
        if (StringUtil.notNullOrEmpty(reservation.getUniqueId())) {
            throw new ReservationException("Reservation already has an appointment id.",
                ExchangeCalendarService.class);
        }
        
        try {
            final Appointment appointment = this.appointmentHelper.createAppointment(reservation);
            this.appointmentHelper.updateAppointment(reservation, appointment);
            appointment.save(SendInvitationsMode.SendOnlyToAll);
            appointment.load(PropertySet.FirstClassProperties);
            reservation.setUniqueId(appointment.getICalUid());
            
            return appointment.getICalUid();
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error creating appointment. Please refer to archibus.log for details",
                exception, ExchangeCalendarService.class);
        }
    }
    
    /** {@inheritDoc} */
    public void updateAppointment(final IReservation reservation) throws ExceptionBase {
        try {
            final Appointment appointment = this.appointmentHelper.createAppointment(reservation);
            if (appointment.isNew()) {
                // The appointment with the given id no longer exists, so create
                // a new one and update the ID.
                this.appointmentHelper.updateAppointment(reservation, appointment);
                appointment.save(SendInvitationsMode.SendOnlyToAll);
                appointment.load(PropertySet.FirstClassProperties);
                reservation.setUniqueId(appointment.getICalUid());
            } else if (AppointmentEquivalenceChecker.isEquivalent(reservation, appointment, false)) {
                logger.debug("Not updating linked appointment because it's still equivalent.");
            } else {
                this.appointmentHelper.updateAppointment(reservation, appointment);
                appointment.update(ConflictResolutionMode.AlwaysOverwrite,
                    SendInvitationsOrCancellationsMode.SendOnlyToAll);
            }
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error updating appointment. Please refer to archibus.log for details",
                exception, ExchangeCalendarService.class);
        }
    }
    
    /** {@inheritDoc} */
    public void updateAppointmentOccurrence(final IReservation reservation,
            final IReservation originalReservation) {
        try {
            final ExchangeService initializedService = getInitializedService(reservation);
            final Appointment appointment =
                    this.appointmentHelper.bindToOccurrence(initializedService, reservation
                        .getUniqueId(), ((RoomReservation) originalReservation)
                        .getTimePeriodInTimeZone(Constants.TIMEZONE_UTC).getStartDateTime());
            if (appointment == null) {
                // @translatable
                throw new CalendarException(
                    "Appointment occurrence linked to reservation {0} not found for updating.",
                    ExchangeCalendarService.class, reservation.getReserveId());
            } else if (AppointmentEquivalenceChecker.isEquivalent(reservation, appointment, false)) {
                logger.debug("Not updating meeting occurrence because it's still equivalent.");
            } else {
                this.appointmentHelper.updateAppointment(reservation, appointment);
                appointment.update(ConflictResolutionMode.AlwaysOverwrite,
                    SendInvitationsOrCancellationsMode.SendOnlyToAll);
            }
            updateCachedExchangeService(initializedService, reservation);
        } catch (final ExceptionBase exception) {
            throw exception;
        } catch (final ServiceResponseException exception) {
            if (ServiceError.ErrorOccurrenceCrossingBoundary.equals(exception
                .getErrorCode())) {
                // @translatable
                throw new ReservationException("Occurrence cannot skip over another occurrence.",
                    ExchangeCalendarService.class);
            } else {
                throw new CalendarException(ERROR_UPDATING_OCCURRENCE, exception,
                    ExchangeCalendarService.class);
            }
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification:
            // third-party API method throws
            // a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException(ERROR_UPDATING_OCCURRENCE, exception,
                ExchangeCalendarService.class);
        }
    }
    
    /** {@inheritDoc} */
    public void cancelAppointment(final IReservation reservation, final String message)
            throws ExceptionBase {
        
        final String uniqueId = reservation.getUniqueId();
        if (StringUtil.isNullOrEmpty(uniqueId)) {
            // not linked to an appointment, so return
            return;
        }
        
        final Appointment appointment =
                this.appointmentHelper.bindToAppointment(reservation.getEmail(), uniqueId);
        if (appointment == null) {
            // @translatable
            throw new CalendarException(
                "Appointment linked to reservation {0} not found for cancelling.",
                ExchangeCalendarService.class, reservation.getReserveId());
        } else {
            this.cancelAppointmentImpl(appointment, reservation, message);
        }
    }
    
    /** {@inheritDoc} */
    public void cancelAppointmentOccurrence(final IReservation reservation, final String message)
            throws ExceptionBase {
        final String uniqueId = reservation.getUniqueId();
        if (StringUtil.isNullOrEmpty(uniqueId)) {
            // not linked to an appointment, so return
            return;
        }
        
        final ExchangeService initializedService = getInitializedService(reservation);

        // Get the startDateTime in UTC.
        final Date startDateTime =
                ((RoomReservation) reservation).getTimePeriodInTimeZone(Constants.TIMEZONE_UTC)
                    .getStartDateTime();
        final Appointment appointment =
                this.appointmentHelper
                    .bindToOccurrence(initializedService, uniqueId, startDateTime);
        if (appointment == null) {
            // @translatable
            throw new CalendarException(
                "Appointment occurrence linked to reservation {0} not found for cancelling.",
                ExchangeCalendarService.class, reservation.getReserveId());
        } else {
            this.cancelAppointmentImpl(appointment, reservation, message);
        }
        
        updateCachedExchangeService(initializedService, reservation);
    }
    
    /**
     * Sets the appointment helper.
     * 
     * @param appointmentHelper the new appointment helper
     */
    public void setAppointmentHelper(final AppointmentHelper appointmentHelper) {
        this.appointmentHelper = appointmentHelper;
    }
    
    /**
     * Set the Exchange messages service.
     * 
     * @param exchangeMessagesService the new Exchange messages service
     */
    public void setExchangeMessagesService(final ExchangeMessagesService exchangeMessagesService) {
        this.exchangeMessagesService = exchangeMessagesService;
    }
    
    /**
     * {@inheritDoc}
     */
    public List<AttendeeResponseStatus> getAttendeesResponseStatus(final IReservation reservation)
            throws ExceptionBase {
        final String uniqueId = reservation.getUniqueId();
        final List<AttendeeResponseStatus> responses = new ArrayList<AttendeeResponseStatus>();
        // bind to the appointment, go over all required and optional attendees
        try {
            final Appointment appointment =
                    this.appointmentHelper.bindToAppointment(reservation.getEmail(), uniqueId);
            if (appointment == null) {
                this.logger.debug("Error retrieving response status. Appointment not found.");
            } else {
                for (final Attendee attendee : appointment.getRequiredAttendees()) {
                    responses.add(createResponseStatus(attendee));
                }
                for (final Attendee attendee : appointment.getOptionalAttendees()) {
                    responses.add(createResponseStatus(attendee));
                }
            }
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException("Error retrieving attendee response status",
                exception, ExchangeCalendarService.class);
        }
        return responses;
    }
    
    /**
     * Create attendee response status from the given Exchange attendee.
     * 
     * @param attendee the Exchange attendee to convert.
     * @return the attendee response status
     */
    private AttendeeResponseStatus createResponseStatus(final Attendee attendee) {
        final AttendeeResponseStatus response = new AttendeeResponseStatus();
        response.setName(attendee.getName());
        response.setEmail(attendee.getAddress());
        switch (attendee.getResponseType()) {
            case Accept:
                response.setResponseStatus(ResponseStatus.Accepted);
                break;
            case Decline:
                response.setResponseStatus(ResponseStatus.Declined);
                break;
            case Tentative:
                response.setResponseStatus(ResponseStatus.Tentative);
                break;
            case NoResponseReceived:
            case Unknown:
            default:
                response.setResponseStatus(ResponseStatus.Unknown);
                break;
        }
        return response;
    }

    /**
     * Cancel the given appointment and notify the organizer.
     * 
     * @param appointment the appointment to cancel
     * @param reservation the reservation linked to the appointment
     * @param message the message to include in the cancellation
     * @throws CalendarException when an error occurs
     */
    private void cancelAppointmentImpl(final Appointment appointment,
            final IReservation reservation, final String message) throws CalendarException {
        try {
            /*
             * KB 3041208: use delete when invitations have not been sent. Exchange server will
             * report an error if we use 'cancelMeeting' for an appointment without attendees.
             */
            if (appointment.getMeetingRequestWasSent()) {
                if (StringUtil.isNullOrEmpty(message)) {
                    appointment.cancelMeeting();
                } else {
                    appointment.cancelMeeting(message);
                }
            } else {
                appointment.delete(DeleteMode.MoveToDeletedItems, SendCancellationsMode.SendToNone);
            }
            this.exchangeMessagesService.sendCancelNotification(reservation, message, appointment,
                this.appointmentHelper.getServiceHelper());
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error cancelling appointment. Please refer to archibus.log for details",
                exception, ExchangeCalendarService.class);
        }
    }
    
    /**
     * Initialize an Exchange Service for connecting to Exchange. Reuse the cached service if
     * possible.
     * 
     * @param reservation the reservation
     * @return the initialized Exchange service
     */
    private ExchangeService getInitializedService(final IReservation reservation) {
        // reuse the cached exchange service if possible
        ExchangeService initializedService = null;
        if (this.cachedExchangeService == null || this.cachedOrganizerEmail == null
                || !this.cachedOrganizerEmail.equals(reservation.getEmail())) {
            initializedService =
                    appointmentHelper.getServiceHelper().initializeService(reservation.getEmail());
        } else {
            initializedService = this.cachedExchangeService;
        }
        return initializedService;
    }
    
    /**
     * Update the cached Exchange service to the given service. A next call to getInitializedService
     * will reuse the cached Exchange service if possible.
     * 
     * @param exchangeService the Exchange service to cache
     * @param reservation the reservation for which this service was created
     */
    private void updateCachedExchangeService(final ExchangeService exchangeService,
            final IReservation reservation) {
        this.cachedExchangeService = exchangeService;
        this.cachedOrganizerEmail = reservation.getEmail();
    }

}
