package com.archibus.app.reservation.service;

import java.sql.Time;
import java.util.*;

import com.archibus.app.common.space.domain.*;
import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.Recurrence;
import com.archibus.app.reservation.util.*;
import com.archibus.utility.ExceptionBase;

/**
 * The Class ReservationRemoteServiceImpl.
 * 
 *         <p>
 *         Suppressed warning "PMD.TooManyMethods" in this class.
 *         <p>
 *         Justification: many methods required for Outlook Plugin
 *         
 * @author Bart Vanderschoot
 */
@SuppressWarnings({ "PMD.TooManyMethods" })
public class ReservationRemoteServiceImpl implements ReservationRemoteService {
    
    /** Reservation activity name. */
    private static final String RESERVATIONS_ACTIVITY = "AbWorkplaceReservations";
    
    /** The reservation service. */
    private IReservationService reservationService;
    
    /** The cancel service. */
    private CancelReservationService cancelReservationService;
    
    /** The space service. */
    private ISpaceService spaceService;
    
    /** The employee service. */
    private IEmployeeService employeeService;
    
    /** The calendar settings. */
    private ICalendarSettings calendarSettings;
    
    /**
     * {@inheritDoc}
     */
    public final void cancelRoomReservation(final RoomReservation reservation) throws ExceptionBase {
        ReservationsContextHelper.checkProjectContext();
        this.cancelReservationService.cancelReservation(reservation);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<RoomReservation> cancelRoomReservationByUniqueIdRecurrence(
            final String uniqueId, final String email, final boolean disconnectOnError)
            throws ExceptionBase {
        ReservationsContextHelper.checkProjectContext();
        return (List<RoomReservation>) this.cancelReservationService.cancelRecurringReservation(
            uniqueId, email, disconnectOnError);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void disconnectRoomReservation(final RoomReservation reservation)
            throws ExceptionBase {
        
        // get the original reservation, so any changes in the object received from the client
        // are not saved
        final RoomReservation roomReservation =
                this.reservationService.getActiveReservation(reservation.getReserveId(),
                    Constants.TIMEZONE_UTC);
        
        this.cancelReservationService.disconnectReservation(roomReservation);
        
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<RoomArrangement> findAvailableRooms(final RoomReservation reservation,
            final Integer capacity, final boolean allDayEvent) throws ExceptionBase {
        return this.reservationService.findAvailableRooms(reservation, capacity, false, null,
            allDayEvent, Constants.TIMEZONE_UTC);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<RoomArrangement> findAvailableRoomsRecurrence(
            final RoomReservation reservation, final Integer capacity, final boolean allDayEvent,
            final Recurrence recurrence) throws ExceptionBase {
        ReservationsContextHelper.checkProjectContext();
        return this.reservationService.findAvailableRoomsRecurrence(reservation, capacity, false,
            null, allDayEvent, recurrence, Constants.TIMEZONE_UTC);
    }

    /**
     * {@inheritDoc}
     */
    public final List<ArrangeType> getArrangeTypes() throws ExceptionBase {
        return this.spaceService.getArrangeTypes();
    }
    
    /**
     * {@inheritDoc}
     */
    public final UserLocation getUserLocation(final String email) throws ExceptionBase {
        this.employeeService.findEmployee(email);
        return this.employeeService.getUserLocation();
    }
    
    /**
     * {@inheritDoc}
     */
    @Deprecated
    public final List<Site> getSites() throws ExceptionBase {
        return this.spaceService.getSites(new Site());
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<Country> findCountries(final Country filter) throws ExceptionBase {
        return this.spaceService.getCountries(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<State> findStates(final State filter) throws ExceptionBase {
        return this.spaceService.getStates(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<City> findCities(final City filter) throws ExceptionBase {
        return this.spaceService.getCities(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<Site> findSites(final Site filter) throws ExceptionBase {
        return this.spaceService.getSites(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<Building> findBuildings(final Building filter) throws ExceptionBase {
        return this.spaceService.getBuildings(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    @Deprecated
    public final List<Building> getBuildings(final String siteId) throws ExceptionBase {
        final Building filter = new Building();
        filter.setSiteId(siteId);
        return this.spaceService.getBuildings(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    @Deprecated
    public final Building getBuildingDetails(final String blId) throws ExceptionBase {
        return this.spaceService.getBuildingDetails(blId);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<Floor> getFloors(final Floor filter) throws ExceptionBase {
        return this.spaceService.getFloors(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public final RoomReservation getRoomReservationById(final Integer reserveId)
            throws ExceptionBase {
        return this.reservationService.getActiveReservation(reserveId, Constants.TIMEZONE_UTC);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<RoomReservation> getRoomReservationsByUniqueId(final String uniqueId)
            throws ExceptionBase {
        return this.reservationService.getByUniqueId(uniqueId, Constants.TIMEZONE_UTC);
    }
    
    /**
     * {@inheritDoc}
     */
    public final String heartBeat() {
        return "ok";
    }
    
    /**
     * {@inheritDoc}
     */
    public final RoomReservation saveRoomReservation(final RoomReservation reservation)
            throws ExceptionBase {
        this.employeeService.setRequestor(reservation);
        ReservationsContextHelper.checkProjectContext();
        this.reservationService.saveReservation(reservation);
        return this.getRoomReservationById(reservation.getReserveId());
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean verifyRecurrencePattern(final String uniqueId,
            final Recurrence recurrence, final Time startTime, final Time endTime,
            final String timeZone) throws ExceptionBase {
        ReservationsContextHelper.checkProjectContext();
        return this.reservationService.verifyRecurrencePattern(uniqueId, recurrence, startTime,
            endTime, timeZone);
        
    }
    
    /**
     * {@inheritDoc}
     */
    public final Room getRoomDetails(final String blId, final String flId, final String rmId)
            throws ExceptionBase {
        return this.spaceService.getRoomDetails(blId, flId, rmId);
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getActivityParameter(final String identifier) throws ExceptionBase {
        String value = null;
        if ("RESOURCE_ACCOUNT".equals(identifier)) {
            // This isn't an activity parameter but a Spring property.
            value = this.calendarSettings.getResourceAccount();
        } else {
            ReservationsContextHelper.checkProjectContext();
            value =
                    com.archibus.service.Configuration.getActivityParameterString(
                        RESERVATIONS_ACTIVITY, identifier);
        }
        return value;
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<RoomReservation> saveRecurringRoomReservation(
            final RoomReservation reservation, final Recurrence recurrence) throws ExceptionBase {
        // Set the requestor and save the reservation series.
        this.employeeService.setRequestor(reservation);
        ReservationsContextHelper.checkProjectContext();
        
        final List<RoomReservation> savedReservations =
                this.reservationService.saveRecurringReservation(reservation, recurrence);
        
        // Return the reservations with UTC time zone.
        for (final RoomReservation savedReservation : savedReservations) {
            savedReservation.convertToTimeZone(Constants.TIMEZONE_UTC);
        }
        return savedReservations;
    }
    
    /**
     * Sets the employee service.
     * 
     * @param employeeService the new employee service
     */
    public final void setEmployeeService(final IEmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    
    /**
     * Sets the reservation service.
     * 
     * @param reservationService the new reservation service
     */
    public final void setReservationService(final IReservationService reservationService) {
        this.reservationService = reservationService;
    }
    
    /**
     * Sets the space service.
     * 
     * @param spaceService the new space service
     */
    public final void setSpaceService(final ISpaceService spaceService) {
        this.spaceService = spaceService;
    }
    
    /**
     * Setter for the calendar settings object.
     * 
     * @param calendarSettings the new calendar settings
     */
    public final void setCalendarSettings(final ICalendarSettings calendarSettings) {
        this.calendarSettings = calendarSettings;
    }
     
    /**
     * Sets the cancel reservation service.
     *
     * @param cancelReservationService the new cancel reservation service
     */
    public final void setCancelReservationService(
            final CancelReservationService cancelReservationService) {
        this.cancelReservationService = cancelReservationService;
    }
    
}
