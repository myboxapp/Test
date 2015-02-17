package com.archibus.app.reservation.exchange.service;

import com.archibus.app.reservation.service.IReservationService;

/**
 * Base class for handling Meeting Items from Exchange.
 *<p>
 * Used by Exchange Listener to handle meeting items.
 * Managed by Spring, has prototype scope.
 *
 * @author Yorik Gerlo
 * @since 21.2
 */
public class MeetingItemHandler {
    
    /** The appointment helper can bind to appointments in Exchange. */
    protected AppointmentHelper appointmentHelper;
    /** The reservation service for accessing the Web Central reservations. */
    protected IReservationService reservationService;
    /** The messages service that builds and sends messages to report actions taken by the listener. */
    protected ExchangeMessagesService messagesService;

    /**
     * Set the new appointment helper.
     * 
     * @param appointmentHelper the appointmentHelper to set
     */
    public void setAppointmentHelper(final AppointmentHelper appointmentHelper) {
        this.appointmentHelper = appointmentHelper;
    }

    /**
     * Set the new Reservation service.
     * 
     * @param reservationService the reservationService to set
     */
    public void setReservationService(final IReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Set the new messages service.
     * 
     * @param messagesService the messages service to set
     */
    public void setMessagesService(final ExchangeMessagesService messagesService) {
        this.messagesService = messagesService;
    }
    
}