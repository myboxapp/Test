package com.archibus.app.reservation.exchange.service;

import java.util.List;

import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.exchange.util.AppointmentEquivalenceChecker;
import com.archibus.utility.ExceptionBase;

/**
 * Can handle meeting requests from the Reservations Mailbox on Exchange, to process meeting changes
 * made via Exchange. Managed by Spring.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class MeetingRequestHandler extends MeetingItemHandler {
    
    /**
     * Handle a meeting request.
     * 
     * @param request the meeting request
     */
    public void handleMeetingRequest(final MeetingRequest request) {
        try {
            request.load(new PropertySet(MeetingRequestSchema.Start, MeetingRequestSchema.ICalUid,
                MeetingRequestSchema.IsOutOfDate, MeetingRequestSchema.AppointmentType,
                MeetingRequestSchema.Organizer, MeetingRequestSchema.AssociatedAppointmentId));
            // Ignore this message if it is already out of date or already processed.
            final boolean outOfDate =
                    (Boolean) request
                        .getObjectFromPropertyDefinition(MeetingMessageSchema.IsOutOfDate);
            if (outOfDate) {
                // this.logger.debug("Ignoring message that is out of date.");
                request.delete(DeleteMode.HardDelete);
            } else {
                // Check whether the appointment is linked to a reservation id.
                final String iCalUid =
                        (String) request
                            .getObjectFromPropertyDefinition(MeetingMessageSchema.ICalUid);
                final String organizerEmail = request.getOrganizer().getAddress();
                ItemHandlerImpl.setUserFromEmail(organizerEmail, request);
                final ItemId appointmentId =
                        (ItemId) request
                            .getObjectFromPropertyDefinition(MeetingRequestSchema.AssociatedAppointmentId);
                Appointment appointment = null;
                AppointmentType appointmentType = request.getAppointmentType();
                if (appointmentId != null) {
                    appointment = Appointment.bind(request.getService(), appointmentId);
                }
                if (appointment != null) {
                    // We already have the corresponding appointment on the calendar, get the
                    // appointment type from there.
                    // This is necessary to detect occurrence updates.
                    appointmentType = appointment.getAppointmentType();
                }
                switch (appointmentType) {
                    case Single:
                        /*
                         * 1. Regular: bind to the original item, check reservation equivalence and
                         * update if required. Reject the request if the update failed, otherwise
                         * accept.
                         */
                        checkSingleMeeting(request,
                            this.appointmentHelper.bindToAppointment(organizerEmail, iCalUid));
                        break;
                    case RecurringMaster:
                        /*
                         * 2. Master: bind to the original master, check equivalence for all
                         * occurrences (mind modified and cancelled ones, although none of those
                         * should exist). If it doesn't match the reservations in the database, just
                         * reject for now. -> only dates/times need to match, other fields can be
                         * updated in the database. Alternative as in Outlook: when dates/times do
                         * not match (i.e. the pattern was changed), take the first active
                         * reservation, cancel all reservations and use the first reservation to
                         * create a new series that matches the new recurrence pattern in the
                         * master.
                         */
                        handleRecurringMeetingRequest(request, organizerEmail, iCalUid);
                        break;
                    default:
                        // cases Exception and Occurrence
                        /*
                         * 3. Occurrence: bind to the original occurrence, if it's an exception
                         * first check there for the reservation id. Otherwise or if no reservation
                         * id is found, bind to the master to get the reservation id for this
                         * occurrence. Verify equivalence between the reservation and occurrence,
                         * update the reservation if required. Reject if the update fails. (probably
                         * best to reject by catching and rethrowing ReservationException)
                         */
                        checkSingleMeeting(request, this.appointmentHelper.bindToOccurrence(
                            organizerEmail, iCalUid, request.getStart()));
                        break;
                
                }
            }
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw ItemHandlerImpl.wrapItemError(exception);
        }
    }
    
    /**
     * Check the equivalence of a single meeting with its corresponding reservation. This can be a
     * single meeting or an occurrence.
     * 
     * @param request the request
     * @param appointment the meeting on the organizer's calendar
     */
    private void checkSingleMeeting(final MeetingRequest request, final Appointment appointment) {
        RoomReservation reservation = null;
        if (appointment != null) {
            final Integer reservationId =
                    this.appointmentHelper.getAppointmentPropertiesHelper().getReservationId(
                        appointment);
            if (reservationId != null) {
                reservation =
                        this.reservationService.getActiveReservation(reservationId,
                            Constants.TIMEZONE_UTC);
            }
        }
        try {
            if (reservation == null) {
                request.decline(true);
            } else if (AppointmentEquivalenceChecker.isEquivalent(reservation, appointment, true)) {
                request.accept(true);
            } else {
                // Try to update the reservation.
                try {
                    this.reservationService.saveReservation(reservation);
                    request.accept(true);
                } catch (final ReservationException exception) {
                    sendUpdateFailure(request, reservation.getReserveId(), exception);
                    
                    // rethrow the reservation exception to ensure nothing is changed in the
                    // database
                    throw exception;
                }
            }
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException("Error accepting or declining meeting request.", exception,
                MeetingRequestHandler.class);
        }
    }
    
    /**
     * Decline the meeting request because of a ReservationException.
     * 
     * @param request the meeting request to decline
     * @param reservationId id of the reservation that could not be updated
     * @param exception indicates the cause of the failure
     */
    private void sendUpdateFailure(final MeetingRequest request, final Integer reservationId,
            final ReservationException exception) {
        // Get the unmodified reservation.
        final RoomReservation reservation =
                this.reservationService.getActiveReservation(reservationId, null);
        
        // Inform the requestor, indicating the cause of the error.
        this.messagesService.sendUpdateFailure(request, reservation, exception);
    }
    
    /**
     * Handle the request for a recurring meeting. It is only accepted if all reservations match
     * their appointment occurrence. Updates of date/time are not supported, other changes are
     * applied.
     * 
     * @param request the request
     * @param organizerEmail email address of the organizer
     * @param iCalUid unique id of the recurring meeting in Exchange
     */
    private void handleRecurringMeetingRequest(final MeetingRequest request,
            final String organizerEmail, final String iCalUid) {
        try {
            final List<RoomReservation> reservations =
                    this.reservationService.getByUniqueId(iCalUid, Constants.TIMEZONE_UTC);
            boolean accept = true;
            Integer reservationId = null;
            final ExchangeService exchangeService =
                    this.appointmentHelper.getServiceHelper().initializeService(organizerEmail);

            for (final RoomReservation reservation : reservations) {
                reservationId = reservation.getReserveId();
                final Appointment occurrence =
                        this.appointmentHelper.bindToOccurrence(exchangeService, iCalUid,
                            reservation.getStartDateTime());
                if (occurrence == null) {
                    accept = false;
                    break;
                } else if (!AppointmentEquivalenceChecker.isEquivalent(reservation, occurrence,
                    true)) {
                    try {
                        this.reservationService.saveReservation(reservation);
                    } catch (final ReservationException exception) {
                        sendUpdateFailure(request, reservation.getReserveId(), exception);
                        
                        // rethrow the reservation exception to ensure nothing is changed in the
                        // database
                        throw exception;
                    }
                }
            }
            if (accept) {
                request.accept(true);
            } else {
                // @translatable
                sendUpdateFailure(
                    request,
                    reservationId,
                    new ReservationException(
                        "This reservation occurrence does not have a corresponding meeting occurrence. Use WebCentral or Outlook Plugin to update the timing of a recurrence series.",
                        MeetingRequestHandler.class));
            }
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException("Error verifying recurring meeting request.", exception,
                MeetingRequestHandler.class);
        }
    }
    
}
