package com.archibus.app.reservation.domain;

import java.sql.Time;

import javax.xml.bind.annotation.*;

/**
 * Domain class for Resource Allocation.
 * 
 * Resources can be allocation to room reservations or resources can be reserved without a room.
 * 
 * @author Bart Vanderschoot
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ResourceAllocation")
public class ResourceAllocation extends AbstractAllocation {
    
    /** The id. */
    private Integer id;
    
    /** The quantity. */
    private Integer quantity;
    
    /** The resource. */
    private Resource resource;
    
    /** The resource id. */
    private String resourceId;
    
    /**
     * Instantiates a new resource allocation.
     */
    public ResourceAllocation() {
        super();
    }
    
    /**
     * Instantiates a new resource allocation.
     * 
     * @param reservation the reservation
     */
    public ResourceAllocation(final IReservation reservation) {
        super();
        setReservation(reservation);
    }
    
    /**
     * Instantiates a new resource allocation.
     * 
     * @param resource the resource
     * @param reservation the reservation
     * @param quantity the quantity
     */
    public ResourceAllocation(final Resource resource, final IReservation reservation,
            final Integer quantity) {
        super();
        setReservation(reservation);
        setResource(resource);
        this.quantity = quantity;
    }
    
    /**
     * Copy this object's properties to the resource allocation provided as a parameter.
     * 
     * @param allocation the resource allocation to modify according to this object's properties
     */
    @Override
    public void copyTo(final AbstractAllocation allocation) {
        if (allocation instanceof ResourceAllocation) {
            super.copyTo(allocation);
            final ResourceAllocation resourceAllocation = (ResourceAllocation) allocation;
            resourceAllocation.setResourceId(this.resourceId);
            resourceAllocation.setQuantity(this.quantity);
        } else {
            throw new IllegalArgumentException("Target object is not a resource allocation.");
        }
    }
    
    /**
     * Get the resource allocation identifier.
     * 
     * @return the resource allocation identifier
     */
    @Override
    public final Integer getId() {
        return this.id;
    }
    
    /**
     * Gets the quantity of the resource allocation.
     * 
     * @return the quantity of the resource allocation
     */
    public final Integer getQuantity() {
        return this.quantity;
    }
    
    /**
     * Gets the resource.
     * 
     * @return the resource
     */
    @XmlTransient
    public final Resource getResource() {
        return this.resource;
    }
    
    /**
     * Gets the resource id of this resource allocation.
     * 
     * @return the resource id
     */
    public final String getResourceId() {
        return this.resourceId;
    }
    
    /**
     * Sets the id of the resource allocation.
     * 
     * @param id the new id
     */
    public final void setId(final Integer id) {
        this.id = id;
    }
    
    /**
     * Sets the allocated quantity of the resource.
     * 
     * @param quantity the new quantity
     */
    public final void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }
    
    /**
     * Sets the resource.
     * 
     * @param resource the new resource
     */
    public final void setResource(final Resource resource) {
        this.resource = resource;
        this.resourceId = resource.getResourceId();
    }
    
    /**
     * Sets the resource id of the allocation.
     * 
     * @param resourceId the resource id for this allocation
     */
    public final void setResourceId(final String resourceId) {
        this.resourceId = resourceId;
    }
    
    /**
     * {@inheritDoc} Only update the resource allocation time period if the current time period is
     * outside the main reservation time period.
     */
    @Override
    public final void setReservation(final IReservation reservation) {
        // set the id
        setReserveId(reservation.getReserveId());
        // copy from reservation
        setStartDate(reservation.getStartDate());
        setEndDate(reservation.getEndDate());
        
        // make sure the date of time values are set to 1899
        if (this.getStartTime() != null) {
            setStartTime(new Time(TimePeriod.clearDate(getStartTime()).getTime()));
        }
        if (this.getEndTime() != null) {
            setEndTime(new Time(TimePeriod.clearDate(getEndTime()).getTime()));
        }
        
        final Time reservationStartTime =
                new Time(TimePeriod.clearDate(reservation.getStartTime()).getTime());
        final Time reservationEndTime =
                new Time(TimePeriod.clearDate(reservation.getEndTime()).getTime());

        // Modify the allocation time so it is within the boundaries of the reservation time.
        if (this.getStartTime() == null || this.getStartTime().before(reservationStartTime)
                || this.getStartTime().compareTo(reservationEndTime) >= 0) {
            setStartTime(reservationStartTime);
        }
        if (this.getEndTime() == null || this.getEndTime().after(reservationEndTime)
                || this.getEndTime().compareTo(reservationStartTime) <= 0) {
            setEndTime(reservationEndTime);
        }
        setTimeZone(reservation.getTimeZone());
    }
    
}
