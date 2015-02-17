package com.archibus.app.reservation.exchange.service;

import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.service.ReservationMessagesService;
import com.archibus.utility.*;

/**
 * Exchange version of the Reservation Messages service. Sends messages via Exchange connection.
 * 
 * @author Yorik Gerlo
 */
public class ExchangeMessagesService extends ReservationMessagesService {
    
    /** Error message in archibus.log when sending response failed. */
    private static final String ERROR_SENDING_REPLY = "Error sending reply via Exchange.";

    /**
     * Send a cancellation confirmation in reply to the given meeting cancellation message.
     * 
     * @param cancellation the cancellation message
     * @param reservation the room reservation that was cancelled
     */
    public void sendCancelledConfirmation(final MeetingCancellation cancellation,
            final RoomReservation reservation) {
        final ReservationMessage message = this.createCancelledConfirmation(reservation);
        
        sendReply(cancellation, message);
    }
    
    /**
     * Send a cancellation confirmation in reply to the given meeting cancellation message.
     * 
     * @param cancellation the cancellation message
     * @param reservation the room reservation that was cancelled
     */
    public void sendCancelledFailure(final MeetingCancellation cancellation,
            final RoomReservation reservation) {
        final ReservationMessage message = this.createCancelledFailure(reservation);
        
        sendReply(cancellation, message);
    }
    
    /**
     * Notify the organizer that his meeting is cancelled. If the exchange.organizerAccount is
     * acting organizer on Exchange, do not send the notification because then the real organizer is
     * notified via the regular meeting cancellation in Exchange.
     * 
     * @param reservation the reservation being cancelled
     * @param message the message to include
     * @param appointment the appointment linked to the reservation
     * @param serviceHelper the Exchange service helper to connect to Exchange
     */
    public void sendCancelNotification(final IReservation reservation, final String message,
            final Appointment appointment, final ExchangeServiceHelper serviceHelper) {
        try {
            // Only send the notification if the reservation organizer is also the meeting organizer
            // in Exchange. If he isn't, he'll receive the cancel notification like a regular
            // attendee.
            final String organizerAccount = serviceHelper.getOrganizerAccount();
            if (!appointment.getService().getImpersonatedUserId().getId().equals(organizerAccount)
                    && reservation instanceof RoomReservation) {
                final ReservationMessage reservationMessage =
                        this.createCancelledNotification(
                            (RoomReservation) reservation, message);
                final String senderEmail = getSenderEmail(reservation, serviceHelper);
                final EmailMessage emailMessage =
                        new EmailMessage(serviceHelper.initializeService(senderEmail));
                emailMessage.getToRecipients().add(reservation.getEmail());
                emailMessage.setSubject(reservationMessage.getSubject());
                emailMessage.setBody(new MessageBody(BodyType.Text, reservationMessage.getBody()));
                emailMessage.send();
            }
        } catch (ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error notifying organizer that his appointment was cancelled. Please refer to archibus.log for details",
                exception, ExchangeMessagesService.class);
        }
    }
    
    /**
     * Decline the meeting request because of a ReservationException.
     * 
     * @param request the meeting request to decline
     * @param reservation the reservation that could not be updated
     * @param exception indicates the cause of the failure
     */
    public void sendUpdateFailure(final MeetingRequest request, final RoomReservation reservation,
            final ReservationException exception) {
        IReservable conflictingReservable = null;
        if (exception instanceof ReservableNotAvailableException) {
            conflictingReservable = ((ReservableNotAvailableException) exception).getReservable();
        }
        
        // Inform the requestor, indicating the cause of the error.
        this.sendUpdateFailure(request, reservation, conflictingReservable,
            exception.getPattern());
    }
    
    /**
     * Notify the organizer that the reservation could not be updated.
     * 
     * @param request the meeting request for the update
     * @param reservation the reservation that should have been updated (not modified)
     * @param conflictingReservable the reservable item in the reservation that causes the issue
     *            (can be null)
     * @param detailMessage indicates the reason for the failure
     */
    private void sendUpdateFailure(final MeetingRequest request, final RoomReservation reservation,
            final IReservable conflictingReservable, final String detailMessage) {
        final ReservationMessage message =
                this.createUpdateFailure(reservation, conflictingReservable, detailMessage);
        
        try {
            final DeclineMeetingInvitationMessage declineMessage = request.createDeclineMessage();
            // We cannot modify the subject of a decline message. Hence, put the subject in the
            // beginning of the message body.
            declineMessage.setBody(new MessageBody(BodyType.Text, message.getSubject() + "\n\n"
                    + message.getBody()));
            declineMessage.calendarSend();
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException(ERROR_SENDING_REPLY, exception,
                ExchangeMessagesService.class);
        }
    }
    
    /**
     * Send the given reservation message as a reply to the given received message.
     * 
     * @param receivedMessage the received message to reply to
     * @param message the message to send
     */
    private void sendReply(final EmailMessage receivedMessage, final ReservationMessage message) {
        try {
            final ResponseMessage email = receivedMessage.createReply(false);
            email.setSubject(message.getSubject());
            email.setBody(new MessageBody(BodyType.Text, message.getBody()));
            email.send();
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException(ERROR_SENDING_REPLY, exception,
                ExchangeMessagesService.class);
        }
    }
    
    /**
     * Get the email address to use as sender for a message related to the given reservation.
     * 
     * @param reservation the reservation the message is about
     * @param serviceHelper Exchange service helper
     * @return the email address to use as sender
     */
    private String getSenderEmail(final IReservation reservation,
            final ExchangeServiceHelper serviceHelper) {
        String senderAccount;
        if (StringUtil.notNullOrEmpty(serviceHelper.getOrganizerAccount())) {
            senderAccount = serviceHelper.getOrganizerAccount();
        } else {
            senderAccount = reservation.getEmail();
        }
        return senderAccount;
    }

}
