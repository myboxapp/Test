package com.archibus.app.reservation.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
 
import com.archibus.app.reservation.dao.IResourceReservationDataSource;
import com.archibus.app.reservation.dao.IRoomReservationDataSource;
import com.archibus.app.reservation.domain.AbstractReservation;
import com.archibus.app.reservation.domain.IReservation;
import com.archibus.app.reservation.domain.ReservationException;
import com.archibus.app.reservation.domain.ResourceReservation;
import com.archibus.app.reservation.domain.RoomReservation;

/**
 * The Class CancelReservationService.
 */
public class CancelReservationService {
    
    /** Error message when a room reservation is expected. */
    // @translatable
    private static final String NO_VALID_RESERVATION = "The reservation must be a room or resource reservation";
 
    /** The room reservation data source. */
    private IRoomReservationDataSource roomReservationDataSource; 
    
    /** The room reservation data source. */
    private IResourceReservationDataSource resourceReservationDataSource; 
    
    /** The Work Request service. */
    private WorkRequestService workRequestService; 

    /**
     * Cancel recurring reservation.
     * 
     * @param uniqueId the unique id
     * @param email the email
     * @param disconnectOnError the disconnect on error
     * @return the list of room reservations
     * @throws ReservationException the reservation exception
     */
    public final List<? extends IReservation> cancelRecurringReservation(final String uniqueId,
            final String email, final boolean disconnectOnError) throws ReservationException {

        final List<RoomReservation> reservations =
                this.roomReservationDataSource.getByUniqueId(uniqueId, null);

        List<IReservation> failureList = null;
        if (reservations == null || reservations.isEmpty()) {
            // no reservations found, so no failures
            failureList = new ArrayList<IReservation>(0);
        } else {
            // reservations found: try to cancel them and return the list of failures
            failureList = cancelReservations(reservations, disconnectOnError);
        }

        return failureList;
    }

    /**
     * Cancel recurring reservation.
     *
     * @param reservation the reservation
     * @return the list of room reservations
     * @throws ReservationException the reservation exception
     */
    public final List<List<IReservation>> cancelRecurringReservation(final IReservation reservation)
                    throws ReservationException {

        // return the list of cancelled reservations and list of failures
        final List<IReservation> cancelledReservations = new ArrayList<IReservation>();
        final List<IReservation> failures = new ArrayList<IReservation>();
        final List<List<IReservation>> result = new ArrayList<List<IReservation>>(2);
        result.add(cancelledReservations);
        result.add(failures);

        if (reservation instanceof RoomReservation) {
            // cancel all occurrences starting from this one
            cancelRecurringRoomReservation(reservation, cancelledReservations, failures);                    
        } else  if (reservation instanceof ResourceReservation) {    
            // cancel all occurrences starting from this one
            cancelRecurringResourceReservation(reservation, cancelledReservations, failures);          
        } else {
            throw new ReservationException(NO_VALID_RESERVATION, CancelReservationService.class);
        }
        return result;
    } 
    

    /**
     * {@inheritDoc}
     */
    public final void cancelReservation(final IReservation reservation) throws ReservationException {
        if (reservation instanceof RoomReservation) { 
            this.roomReservationDataSource.cancel((RoomReservation) reservation);  
            
        } else if (reservation instanceof ResourceReservation) { 
            this.resourceReservationDataSource.cancel((ResourceReservation) reservation);  

        } else {
            throw new ReservationException(NO_VALID_RESERVATION, CancelReservationService.class);
        }
        
        // cancel the work request
        this.workRequestService.cancelWorkRequest(reservation);  
    }

    /**
     * {@inheritDoc}
     */
    public final void disconnectReservation(final RoomReservation reservation)
            throws ReservationException {
        this.roomReservationDataSource.clearUniqueId(reservation);
    }  
    
    /**
     * Sets the room reservation data source.
     *
     * @param roomReservationDataSource the new room reservation data source
     */
    public void setRoomReservationDataSource(
            final IRoomReservationDataSource roomReservationDataSource) {
        this.roomReservationDataSource = roomReservationDataSource;
    } 

    /**
     * Sets the resource reservation data source.
     *
     * @param resourceReservationDataSource the new resource reservation data source
     */
    public void setResourceReservationDataSource(
            final IResourceReservationDataSource resourceReservationDataSource) {
        this.resourceReservationDataSource = resourceReservationDataSource;
    }

    /**
     * Sets the work request service for cancelling related work requests.
     *
     * @param workRequestService the new work request service for cancelling
     */
    public void setWorkRequestService(final WorkRequestService workRequestService) {
        this.workRequestService = workRequestService;
    } 

    /**
     * Cancel recurring room reservation.
     *
     * @param reservation the reservation
     * @param cancelledReservations the cancelled reservations
     * @param failures the failures
     */
    private void cancelRecurringRoomReservation(final IReservation reservation,
            final List<IReservation> cancelledReservations,
            final List<IReservation> failures) {
        final Date startDate = reservation.getStartDate();
        final Integer parentId = reservation.getParentId() == null 
                ? reservation.getReserveId() : reservation.getParentId();

        // Get all active reservations starting from (and including) the specified one.
        final List<RoomReservation> reservations =
                this.roomReservationDataSource.getByParentId(parentId, null, startDate);
   
        for (final RoomReservation roomReservation : reservations) {
            try {
                this.roomReservationDataSource.canBeCancelledByCurrentUser(roomReservation);
            } catch (ReservationException exception) {
                // this one can't be cancelled, so skip and report
                failures.add(roomReservation);
                continue;
            }
            this.cancelReservation(roomReservation);
            cancelledReservations.add(roomReservation);
        }
    }
    
    
    /**
     * Cancel recurring resource reservation.
     *
     * @param reservation the reservation
     * @param cancelledReservations the cancelled reservations
     * @param failures the failures
     */
    private void cancelRecurringResourceReservation(final IReservation reservation,
            final List<IReservation> cancelledReservations,
            final List<IReservation> failures) {
        final Date startDate = reservation.getStartDate();
        final Integer parentId = reservation.getParentId() == null 
                ? reservation.getReserveId() : reservation.getParentId();

        // Get all active reservations starting from (and including) the specified one.
        final List<ResourceReservation> reservations =
                this.resourceReservationDataSource.getByParentId(parentId, null, startDate);
   
        for (final ResourceReservation abstractReservation : reservations) {
            try {
                this.resourceReservationDataSource.canBeCancelledByCurrentUser(
                        (ResourceReservation) abstractReservation);
            } catch (ReservationException exception) {
                // this one can't be cancelled, so skip and report
                failures.add(abstractReservation);
                continue;
            }
            this.cancelReservation(abstractReservation);
            cancelledReservations.add(abstractReservation);
        }
    } 

    /**
     * Cancels a list of reservations. If disconnectOnError is false, first checks that all
     * reservations can be cancelled. If not all reservations can be cancelled, no reservations are
     * cancelled and the list of failures is returned.
     * 
     * @param reservations the reservations to cancel
     * @param disconnectOnError true if reservations that cannot be cancelled must be disconnected
     * @return list of reservations that cannot be cancelled
     * @throws ReservationException when an error occurs
     */
    private List<IReservation> cancelReservations(final List<? extends AbstractReservation> reservations,
            final boolean disconnectOnError) throws ReservationException {
        final List<IReservation> failureList = new ArrayList<IReservation>();
        final List<IReservation> successList = new ArrayList<IReservation>();
        // check that all instances can be cancelled
        for (final IReservation reservation : reservations) {
            
            if (reservation instanceof RoomReservation) {            
                cancelRoomReservations(disconnectOnError, failureList,
                        successList, reservation);
            }  else if (reservation instanceof ResourceReservation) {        
                cancelResourceReservations(failureList,
                        successList, reservation);                
            }  
        } 
        

        // If there were no failures, proceed with canceling the other reservations.
        if (failureList.isEmpty()) {
            for (final IReservation reservation : successList) {
                if (reservation instanceof RoomReservation) {            
                    this.roomReservationDataSource.cancel((RoomReservation) reservation);      
                } else if (reservation instanceof ResourceReservation) {     
                    this.resourceReservationDataSource.cancel((ResourceReservation) reservation); 
                }
                // cancel the work request, one by one
                this.workRequestService.cancelWorkRequest(reservation);              
            }
        }

        return failureList;
    }

    /**
     * Cancel resource reservations.
     *
     * @param failureList the failure list
     * @param successList the success list
     * @param reservation the reservation
     */
    private void cancelResourceReservations(final List<IReservation> failureList,
            final List<IReservation> successList, final IReservation reservation) {
        try {
            this.resourceReservationDataSource.canBeCancelledByCurrentUser((ResourceReservation) reservation);
            // this reservation can be cancelled, so add it to the success list
            successList.add(reservation);
        } catch (final ReservationException exception) {
            // this reservation cannot be cancelled, so add it to the failure list
            failureList.add(reservation);
        }
    }

    /**
     * Cancel room reservations.
     *
     * @param disconnectOnError the disconnect on error
     * @param failureList the failure list
     * @param successList the success list
     * @param reservation the reservation
     */
    private void cancelRoomReservations(final boolean disconnectOnError,
            final List<IReservation> failureList,
            final List<IReservation> successList, final IReservation reservation) {
        try {
            this.roomReservationDataSource.canBeCancelledByCurrentUser((RoomReservation) reservation);
            // this reservation can be cancelled, so add it to the success list
            successList.add(reservation);
        } catch (final ReservationException exception) {
            // this reservation cannot be cancelled, so add it to the failure list
            // or disconnect it if the user requested it
            if (disconnectOnError) {
                this.roomReservationDataSource.clearUniqueId((RoomReservation) reservation);
            } else {
                failureList.add(reservation);
            }
        }
    } 
    
}
