package com.archibus.app.reservation.domain;

import java.sql.Time;
import java.util.*;

import javax.xml.bind.annotation.*;

import com.archibus.app.reservation.util.*;
import com.archibus.utility.*;

/**
 * Domain class for Resource (only) Reservation.
 * 
 * @author Bart Vanderschoot
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ResourceReservation")
public class ResourceReservation extends AbstractReservation {

    /** Contains all occurrences when a recurring reservation is created via Web Central. */
    protected List<ResourceReservation> createdReservations;
    
    /** Building id to use when no resource allocations are in the reservation. */
    private String buildingId;

    /**
     * Default constructor.
     */
    public ResourceReservation() {
        super();
    }
    
    /**
     * Constructor for a time period.
     * 
     * @param timePeriod the time period
     */
    public ResourceReservation(final TimePeriod timePeriod) {
        super();
        setTimePeriod(timePeriod);
    }
    
    /**
     * Constructor using primary key.
     * 
     * @param reserveId id
     */
    public ResourceReservation(final Integer reserveId) {
        super(reserveId);
    }
    
    /**
     * Calculate total cost for the resource reservation.
     * 
     * @return total cost
     */
    public double calculateTotalCost() {
        // calculate resource costs.
        double totalCost = 0.0;
        
        for (final ResourceAllocation resourceAllocation : this.getResourceAllocations()) {
            totalCost += resourceAllocation.getCost();
        }

        // Round the result to two decimals.
        totalCost = DataSourceUtils.round2(totalCost);
        this.setCost(totalCost);
        
        return totalCost;
    }
    
    /**
     * {@inheritDoc}
     */
    @XmlTransient
    public TimePeriod getTimePeriodInTimeZone(final String timeZoneId) {
        Date startDateTime = null;
        Date endDateTime = null;
        final String building = this.determineBuildingId();
        if (building == null) {
            // No time zone can be found based on a resource allocation.
            // Assume DB is in UTC and convert to the requested time zone.
            startDateTime =
                    TimeZoneConverter.calculateRequestorDateTime(this.getStartDate(),
                        this.getStartTime(), timeZoneId, true);
            endDateTime =
                    TimeZoneConverter.calculateRequestorDateTime(this.getEndDate(),
                        this.getEndTime(), timeZoneId, true);
        } else {
            // Base the time zone conversion on the building time zone.
            startDateTime =
                    TimeZoneConverter.calculateDateTimeForBuilding(building, this.getStartDate(),
                        this.getStartTime(), timeZoneId, false);
            endDateTime =
                    TimeZoneConverter.calculateDateTimeForBuilding(building, this.getEndDate(),
                        this.getEndTime(), timeZoneId, false);
        }

        return new TimePeriod(startDateTime, endDateTime, timeZoneId);
    }
    
    /**
     * {@inheritDoc}
     */
    public Date determineCurrentLocalDate() {
        Date currentDate = null;
        
        final String building = this.determineBuildingId();
        if (StringUtil.isNullOrEmpty(building)) {
            currentDate = TimePeriod.clearTime(Utility.currentDate());
        } else {
            currentDate =
                    TimePeriod
                        .clearTime(LocalDateTimeUtil.currentLocalDate(null, null, null, building));
        }
        return currentDate;
    }
    
    /**
     * {@inheritDoc}
     */
    public Time determineCurrentLocalTime() {
        Time currentTime = null;
        
        final String building = this.determineBuildingId();
        if (StringUtil.isNullOrEmpty(building)) {
            currentTime = Utility.currentTime();
        } else {
            currentTime = LocalDateTimeUtil.currentLocalTime(null, null, null, building);
        }
        return currentTime;
    }
    
    /**
     * Set the created reservations.
     * 
     * @param createdReservations the created reservations to set
     */
    public void setCreatedReservations(final List<ResourceReservation> createdReservations) {
        this.createdReservations = createdReservations;
    }
    
    /**
     * Get the created reservations.
     * 
     * @return the list of created reservations (only when the recurring reservation was just
     *         created)
     */
    @XmlTransient
    public List<ResourceReservation> getCreatedReservations() {
        return this.createdReservations;
    }
    
    /**
     * Set the building id to be used when no resource allocations are linked.
     * 
     * @param buildingId the building id to set
     */
    public void setBuildingId(final String buildingId) {
        this.buildingId = buildingId;
    }

    /**
     * Get the building id for this resource-only reservation.
     * 
     * @return the building id, or null if it can't be determined
     */
    private String determineBuildingId() {
        List<ResourceAllocation> allocations = this.getActiveResourceAllocations();
        if (allocations.isEmpty()) {
            // there are no active allocations, so check for inactive ones
            allocations = this.getResourceAllocations();
        }
        String building = null;
        if (allocations.isEmpty()) {
            building = this.buildingId;
        } else {
            building = allocations.get(0).getBlId();
        }
        return building;
    }
    
}
