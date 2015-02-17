package com.archibus.app.reservation.exchange.service;

import microsoft.exchange.webservices.data.*;

import org.springframework.security.userdetails.UsernameNotFoundException;

import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.util.ReservationsContextHelper;
import com.archibus.utility.ExceptionBase;

/**
 * Can handle items from the Reservations Mailbox on Exchange, to process meeting changes made via
 * Exchange. Managed by Spring.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class ItemHandlerImpl implements ItemHandler {
    
    /** Error message that indicates something went wrong handling an item. */
    private static final String ITEM_ERROR = "Error handling Exchange item.";
    
    /** The meeting request handler. */
    private MeetingRequestHandler meetingRequestHandler;
    
    /** The meeting cancellation handler. */
    private MeetingCancellationHandler meetingCancellationHandler;
    
    /**
     * {@inheritDoc}
     */
    public void handleItem(final Item item) {
        if (item instanceof MeetingRequest) {
            this.meetingRequestHandler.handleMeetingRequest((MeetingRequest) item);
        } else if (item instanceof MeetingCancellation) {
            this.meetingCancellationHandler.handleMeetingCancellation((MeetingCancellation) item);
        } else {
            // Delete all other items.
            try {
                item.delete(DeleteMode.HardDelete);
                // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
                // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
            } catch (final Exception exception) {
                // CHECKSTYLE:ON
                throw new CalendarException("Unable to delete item.", exception,
                    ItemHandlerImpl.class);
            }
        }
    }
    
    /**
     * Set the new meeting request handler.
     * 
     * @param meetingRequestHandler the meeting request handler to set
     */
    public void setMeetingRequestHandler(final MeetingRequestHandler meetingRequestHandler) {
        this.meetingRequestHandler = meetingRequestHandler;
    }
    
    /**
     * Set the new meeting cancellation handler.
     * 
     * @param meetingCancellationHandler the meeting cancellation handler to set
     */
    public void setMeetingCancellationHandler(
            final MeetingCancellationHandler meetingCancellationHandler) {
        this.meetingCancellationHandler = meetingCancellationHandler;
    }
    
    /**
     * Wrap an exception that occurred when handling an inbox item in an ExceptionBase. If the
     * exception is an ExceptionBase, do not wrap it but rethrow.
     * 
     * @param exception the exception to wrap
     * @return the exception as an ExceptionBase
     */
    protected static ExceptionBase wrapItemError(final Exception exception) {
        ExceptionBase exceptionBase = null;
        if (exception instanceof ExceptionBase) {
            exceptionBase = (ExceptionBase) exception;
        } else {
            exceptionBase = new CalendarException(ITEM_ERROR, exception, ItemHandlerImpl.class);
        }
        return exceptionBase;
    }
    
    /**
     * Set the user based on the given email. If not found, delete the message and throw exception.
     * 
     * @param organizerEmail the email of the user to set
     * @param item the item to delete when the user is not found
     */
    protected static void setUserFromEmail(final String organizerEmail, final Item item) {
        try {
            try {
                ReservationsContextHelper.setUserFromEmail(organizerEmail);
            } catch (final UsernameNotFoundException exception) {
                // If the requestor isn't a WebCentral user, ignore his messages.
                item.delete(DeleteMode.HardDelete);
                throw exception;
            }
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (Exception exception) {
            // CHECKSTYLE:ON
            throw wrapItemError(exception);
        }
    }
    
}
