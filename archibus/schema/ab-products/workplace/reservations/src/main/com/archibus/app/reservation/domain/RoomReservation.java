package com.archibus.app.reservation.domain;

import java.sql.Time;
import java.util.*;

import javax.xml.bind.annotation.*;

import com.archibus.app.reservation.util.*;
import com.archibus.utility.*;

/**
 * Domain class for Room Reservation.
 * 
 * @author Bart Vanderschoot
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "RoomReservation")
public class RoomReservation extends AbstractReservation {
    
    /**
     * Room allocations for this room. Normally there will be one room allocation for a room
     * reservation.
     */
    protected List<RoomAllocation> roomAllocations;
    
    /** Contains all occurrences when a recurring reservation is created via Web Central. */
    protected List<RoomReservation> createdReservations;
    
    /**
     * Default constructor.
     */
    public RoomReservation() {
        super();
    }
    
    /**
     * Constructor for a time period.
     * 
     * @param timePeriod the time period
     */
    public RoomReservation(final TimePeriod timePeriod) {
        super();
        setTimePeriod(timePeriod);
    }
    
    /**
     * Constructor using parameters.
     * 
     * @param timePeriod the time period
     * @param blId building id
     * @param flId floor id
     * @param rmId room id
     * @param configId configuration id
     * @param arrangeTypeId arrange type id
     */
    public RoomReservation(final TimePeriod timePeriod, final String blId, final String flId,
            final String rmId, final String configId, final String arrangeTypeId) {
        this(timePeriod);
        final RoomAllocation roomAllocation =
                new RoomAllocation(blId, flId, rmId, configId, arrangeTypeId, this);
        addRoomAllocation(roomAllocation);
    }
    
    /**
     * Constructor using primary key.
     * 
     * @param reserveId id
     */
    public RoomReservation(final Integer reserveId) {
        super(reserveId);
    }
    
    /**
     * Constructor using objects.
     * 
     * @param timePeriod time period
     * @param roomArrangement room arrangement
     */
    public RoomReservation(final TimePeriod timePeriod, final RoomArrangement roomArrangement) {
        super();
        setTimePeriod(timePeriod);
        final RoomAllocation roomAllocation = new RoomAllocation(roomArrangement, this);
        addRoomAllocation(roomAllocation);
    }
    
    /**
     * Add a room allocation to the reservation.
     * 
     * @param roomAllocation room allocation
     */
    public final void addRoomAllocation(final RoomAllocation roomAllocation) {
        if (this.roomAllocations == null) {
            this.roomAllocations = new ArrayList<RoomAllocation>();
        }
        this.roomAllocations.add(roomAllocation);
        
        // Copy the time period and id of the reservation to the allocation,
        // so no need to modify the date of the time values here.
        roomAllocation.setReservation(this);
    }
    
    /**
     * Get room allocations.
     * 
     * @return room allocations
     */
    public final List<RoomAllocation> getRoomAllocations() {
        return this.roomAllocations;
    }
    
    /**
     * Set room allocations.
     * 
     * @param roomAllocations room allocations
     */
    public final void setRoomAllocations(final List<RoomAllocation> roomAllocations) {
        this.roomAllocations = roomAllocations;
    }
    
    /**
     * Convert a reservation and its room allocations to the given time zone.
     * 
     * @param timeZoneId the target time zone
     */
    public final void convertToTimeZone(final String timeZoneId) {
        super.convertToTimeZone(timeZoneId);
        final Date startDateTime = this.getStartDateTime();
        final Date endDateTime = this.getEndDateTime();
        
        for (final RoomAllocation roomAllocation : this.getRoomAllocations()) {
            roomAllocation.setStartDateTime(startDateTime);
            roomAllocation.setEndDateTime(endDateTime);
            roomAllocation.setTimeZone(timeZoneId);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @XmlTransient
    public TimePeriod getTimePeriodInTimeZone(final String timeZoneId) {
        Date startDateTime = null;
        Date endDateTime = null;
        if (this.getRoomAllocations().isEmpty()) {
            // No time zone can be found based on a room allocation.
            // Assume DB is in UTC and convert to the requested time zone.
            startDateTime =
                    TimeZoneConverter.calculateRequestorDateTime(this.getStartDate(),
                        this.getStartTime(), timeZoneId, true);
            endDateTime =
                    TimeZoneConverter.calculateRequestorDateTime(this.getEndDate(),
                        this.getEndTime(), timeZoneId, true);
        } else {
            // Base the time zone conversion on the first room's time zone.
            final String blId = this.getRoomAllocations().get(0).getBlId();
            startDateTime =
                    TimeZoneConverter.calculateDateTimeForBuilding(blId, this.getStartDate(),
                        this.getStartTime(), timeZoneId, false);
            endDateTime =
                    TimeZoneConverter.calculateDateTimeForBuilding(blId, this.getEndDate(),
                        this.getEndTime(), timeZoneId, false);
        }
        
        return new TimePeriod(startDateTime, endDateTime, timeZoneId);
    }
    
    /**
     * Calculate total cost for the room reservation.
     * 
     * @return total cost
     */
    public double calculateTotalCost() {
        // calculate resource costs.
        double totalCost = 0.0;
        for (final ResourceAllocation resourceAllocation : this.getResourceAllocations()) {
            totalCost += resourceAllocation.getCost();
        }
        
        // add room cost.
        for (final RoomAllocation roomAllocation : this.roomAllocations) {
            totalCost += roomAllocation.getCost();
        }
        // Round the result to two decimals.
        totalCost = DataSourceUtils.round2(totalCost);
        this.setCost(totalCost);
        
        return totalCost;
    }
    
    /**
     * {@inheritDoc}
     */
    public Date determineCurrentLocalDate() {
        String blId = null;
        if (this.roomAllocations != null && !this.roomAllocations.isEmpty()) {
            blId = this.roomAllocations.get(0).getBlId();
        }
        Date currentDate = null;
        if (StringUtil.isNullOrEmpty(blId)) {
            currentDate = TimePeriod.clearTime(Utility.currentDate());
        } else {
            currentDate =
                    TimePeriod
                        .clearTime(LocalDateTimeUtil.currentLocalDate(null, null, null, blId));
        }
        return currentDate;
    }
    
    /**
     * {@inheritDoc}
     */
    public Time determineCurrentLocalTime() {
        String blId = null;
        if (this.roomAllocations != null && !this.roomAllocations.isEmpty()) {
            blId = this.roomAllocations.get(0).getBlId();
        }
        Time currentTime = null;
        if (StringUtil.isNullOrEmpty(blId)) {
            currentTime = Utility.currentTime();
        } else {
            currentTime = LocalDateTimeUtil.currentLocalTime(null, null, null, blId);
        }
        return currentTime;
    }
    
    /**
     * Set the created reservations.
     * 
     * @param createdReservations the created reservations to set
     */
    public void setCreatedReservations(final List<RoomReservation> createdReservations) {
        this.createdReservations = createdReservations;
    }
    
    /**
     * Get the created reservations.
     * 
     * @return the list of created reservations (only when the recurring reservation was just
     *         created)
     */
    @XmlTransient
    public List<RoomReservation> getCreatedReservations() {
        return this.createdReservations;
    }
    
}
