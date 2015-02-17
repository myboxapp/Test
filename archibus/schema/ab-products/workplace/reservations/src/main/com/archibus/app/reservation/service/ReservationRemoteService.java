package com.archibus.app.reservation.service;

import java.sql.Time;
import java.util.*;

import javax.jws.*;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.archibus.app.common.space.domain.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.*;
import com.archibus.utility.ExceptionBase;

/**
 * The Interface ReservationRemoteService.
 * 
 *         <p>
 *         Suppressed warning "PMD.TooManyMethods" in this class.
 *         <p>
 *         Justification: many methods required for Outlook Plugin
 * 
 * @author Bart Vanderschoot
 */
@SuppressWarnings({ "PMD.TooManyMethods" })
@WebService(name = "reservationService")
@XmlSeeAlso(value = { DailyPattern.class, WeeklyPattern.class, MonthlyPattern.class,
        YearlyPattern.class })
public interface ReservationRemoteService {
    
    /**
     * Cancel room reservation.
     * 
     * @param reservation the reservation
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "cancelRoomReservation")
    void cancelRoomReservation(RoomReservation reservation) throws ExceptionBase;
    
    /**
     * Cancel room reservation by unique id recurrence.
     * 
     * @param uniqueId the unique id
     * @param email the email
     * @param disconnectOnError the disconnect on error
     * @return the list of reservations that could not be cancelled
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "cancelRoomReservationRecurrence")
    List<RoomReservation> cancelRoomReservationByUniqueIdRecurrence(String uniqueId, String email,
            boolean disconnectOnError) throws ExceptionBase;
    
    /**
     * Disconnect room reservation: remove the appointment unique ID.
     * 
     * @param reservation the reservation
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "disconnectRoomReservation")
    void disconnectRoomReservation(RoomReservation reservation) throws ExceptionBase;
    
    /**
     * Find available rooms.
     * 
     * @param reservation the reservation
     * @param capacity the capacity
     * @param allDayEvent true for all day events, false for regular reservations
     * 
     * @return the list
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "findAvailableRooms")
    List<RoomArrangement> findAvailableRooms(RoomReservation reservation, Integer capacity,
            boolean allDayEvent) throws ExceptionBase;
    
    /**
     * Find available rooms with recurrence.
     * 
     * @param reservation the reservation
     * @param capacity the capacity
     * @param allDayEvent true for all day events, false for regular reservations
     * @param recurrence the recurrence pattern
     * 
     * @return the list
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "findAvailableRoomsRecurrence")
    List<RoomArrangement> findAvailableRoomsRecurrence(RoomReservation reservation,
            Integer capacity, boolean allDayEvent, Recurrence recurrence) throws ExceptionBase;
    
    /**
     * Get room reservation by primary key.
     * 
     * @param reserveId reservation id
     * @return room reservation
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "getRoomReservationById")
    RoomReservation getRoomReservationById(final Integer reserveId) throws ExceptionBase;
    
    /**
     * Gets the room reservations by unique id.
     * 
     * @param uniqueId the unique id
     * @return the room reservations by unique id
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "getRoomReservationsByUniqueId")
    List<RoomReservation> getRoomReservationsByUniqueId(String uniqueId) throws ExceptionBase;
    
    /**
     * Gets the sites that have reservable rooms.
     * 
     * @return the sites
     * 
     * @throws ExceptionBase ExceptionBase
     * @deprecated Maintained for compatibility with 21.1 Outlook Plugin.
     */
    @WebMethod(action = "getSites")
    List<Site> getSites() throws ExceptionBase;
    
    /**
     * Get the countries that have reservable rooms.
     * 
     * @param filter contains restrictions for the countries to return
     * @return the countries
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "findCountries")
    List<Country> findCountries(Country filter) throws ExceptionBase;
    
    /**
     * Get the states that have reservable rooms.
     * 
     * @param filter contains restrictions for the states to return
     * @return the states
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "findStates")
    List<State> findStates(State filter) throws ExceptionBase;
    
    /**
     * Get the cities that have reservable rooms.
     * 
     * @param filter contains restrictions for the cities to return
     * @return the cities
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "findCities")
    List<City> findCities(City filter) throws ExceptionBase;
    
    /**
     * Get the sites that have reservable rooms.
     * 
     * @param filter contains restrictions for the sites to return
     * @return the sites
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "findSites")
    List<Site> findSites(Site filter) throws ExceptionBase;
    
    /**
     * Get the user's location and validate the given email address for creating reservations. An
     * employee with the given email address should exist.
     * 
     * @param email user's email address
     * @return user's location, or null if no location is defined
     * 
     * @throws ExceptionBase when the given email is invalid
     */
    @WebMethod(action = "getUserLocation")
    UserLocation getUserLocation(final String email) throws ExceptionBase;
    
    /**
     * Gets the arrange types.
     * 
     * @return the arrange types
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "getArrangeTypes")
    List<ArrangeType> getArrangeTypes() throws ExceptionBase;
    
    /**
     * Gets the buildings.
     * 
     * @param siteId the site id
     * @return the buildings
     * 
     * @throws ExceptionBase ExceptionBase
     * @deprecated Maintained for compatibility with 21.1 Outlook Plugin.
     */
    @WebMethod(action = "getBuildings")
    List<Building> getBuildings(String siteId) throws ExceptionBase;
    
    /**
     * Gets the buildings.
     * 
     * @param filter contains restrictions for the buildings to return
     * @return the buildings
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "findBuildings")
    List<Building> findBuildings(Building filter) throws ExceptionBase;
    
    /**
     * Get building details.
     * 
     * @param blId building id
     * @return the building details
     * 
     * @throws ExceptionBase ExceptionBase
     * @deprecated Maintained for compatibility with 21.1 Outlook Plugin.
     */
    @WebMethod(action = "getBuildingDetails")
    Building getBuildingDetails(final String blId) throws ExceptionBase;
    
    /**
     * Get the floors that have reservable rooms.
     * 
     * @param filter contains restrictions for the floors to return
     * @return the floors
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "getFloors")
    List<Floor> getFloors(Floor filter) throws ExceptionBase;
    
    /**
     * Gets the floors.
     * 
     * @param blId the bl id
     * @param flId the floor id
     * @param rmId the room id
     * 
     * @return the floors
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "getRoomDetails")
    Room getRoomDetails(String blId, String flId, String rmId) throws ExceptionBase;
    
    /**
     * Gets the value of a reservations activity parameter or property.
     * 
     * @param id activity parameter or property identifier
     * @return value of the activity parameter or property
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "getActivityParameter")
    String getActivityParameter(String id) throws ExceptionBase;
    
    /**
     * Heart beat useful for verifying connection status.
     * 
     * @return the string
     */
    @WebMethod(action = "heartBeat")
    String heartBeat();
    
    /**
     * Save recurring room reservation.
     * 
     * @param reservation the reservation
     * @param recurrence the recurrence
     * @return the list
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "saveRecurringRoomReservation")
    List<RoomReservation> saveRecurringRoomReservation(RoomReservation reservation,
            Recurrence recurrence) throws ExceptionBase;
    
    /**
     * Save room reservation.
     * 
     * @param reservation the reservation
     * @return the room reservation
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "saveRoomReservation")
    RoomReservation saveRoomReservation(RoomReservation reservation) throws ExceptionBase;
    
    /**
     * Verify whether all reservations linked to an ID match a given recurrence pattern.
     * 
     * @param uniqueId the unique id of the appointment series
     * @param recurrence the recurrence
     * @param startTime time of day that the appointments start
     * @param endTime time of day that the appointments end
     * @param timeZone the time zone
     * @return true if it matches, false if at least one reservation is different or missing
     * 
     * @throws ExceptionBase ExceptionBase
     */
    @WebMethod(action = "verifyRecurrencePattern")
    boolean verifyRecurrencePattern(String uniqueId, Recurrence recurrence, Time startTime,
            Time endTime, String timeZone) throws ExceptionBase;
}
