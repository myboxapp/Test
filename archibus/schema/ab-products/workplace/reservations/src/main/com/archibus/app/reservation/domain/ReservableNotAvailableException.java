package com.archibus.app.reservation.domain;

/**
 * Reservation exception that indicates an item is not available for reservation.
 * 
 * @author Yorik Gerlo
 */
public class ReservableNotAvailableException extends ReservationException {
    
    /** Generated serial version ID. */
    private static final long serialVersionUID = -6483262706971726091L;
    
    /** The reservable that is not available. */
    private final IReservable reservable;
    
    /**
     * Create a reservation exception with localization based on the provided class. The additional
     * arguments are used for formatting the translated string.
     * 
     * @param reservable the reservable that is not available
     * @param message the message (to translate)
     * @param clazz the class where the message was defined
     * @param args additional arguments used for formatting the localized message
     */
    public ReservableNotAvailableException(final IReservable reservable, final String message,
            final Class<?> clazz, final Object... args) {
        super(message, clazz, args);
        this.reservable = reservable;
    }
    
    /**
     * Create a reservation exception with localization based on the provided class.
     * 
     * @param reservable the reservable that is not available
     * @param message the message (to translate)
     * @param clazz the class where the message was defined
     */
    public ReservableNotAvailableException(final IReservable reservable, final String message,
            final Class<?> clazz) {
        super(message, clazz);
        this.reservable = reservable;
    }
    
    /**
     * Get the reservable that was the reason for the exception.
     * 
     * @return the reservable that was the reason for the exception
     */
    public IReservable getReservable() {
        return this.reservable;
    }
    
}
