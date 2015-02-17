package com.archibus.app.reservation.domain;

import java.text.MessageFormat;

import com.archibus.context.ContextStore;
import com.archibus.eventhandler.EventHandlerBase;
import com.archibus.utility.ExceptionBase;

/**
 * Base class for Reservation Exception.
 * 
 * @author Bart Vanderschoot
 * 
 */
public class ReservationException extends ExceptionBase {
    
    /** serializable. */
    private static final long serialVersionUID = 1L;
    
    /**
     * Create a reservation exception with localization based on the provided class.
     * 
     * @param message the message (to translate)
     * @param clazz the class where the message was defined
     */
    public ReservationException(final String message, final Class<?> clazz) {
        super(EventHandlerBase.localizeString(ContextStore.get()
            .getEventHandlerContext(), message, clazz.getName()));
    }
    
    /**
     * Create a reservation exception with localization based on the provided class. The additional
     * arguments are used for formatting the translated string.
     * 
     * @param message the message (to translate)
     * @param clazz the class where the message was defined
     * @param args additional arguments used for formatting the localized message
     */
    public ReservationException(final String message, final Class<?> clazz, final Object... args) {
        super(MessageFormat.format(EventHandlerBase.localizeString(ContextStore.get()
            .getEventHandlerContext(), message, clazz.getName()), args));
    }
}
