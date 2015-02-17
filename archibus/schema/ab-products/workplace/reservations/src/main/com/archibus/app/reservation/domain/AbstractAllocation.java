package com.archibus.app.reservation.domain;

import java.util.Date;

import javax.xml.bind.annotation.*;

/**
 * Abstract class for an allocation in a reservation.
 * 
 * Every reservable object can be allocated in reservation and should be linked to a room.
 * 
 * @author Bart Vanderschoot
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "AbstractAllocation")
public abstract class AbstractAllocation extends AbstractReservationBase implements IAllocation {
    
    /** The rejected date. */
    protected Date rejectedDate;
    
    /** The code of the building where the room is located. */
    protected String blId;
    
    /** The floor code. */
    protected String flId;
    
    /** The room code. */
    protected String rmId;
    
    /**
     * Instantiates a new abstract allocation.
     */
    public AbstractAllocation() {
        super();
    }
    
    /**
     * Constructor for an allocation for a specified room.
     * 
     * @param blId the building identifier
     * @param flId the floor identifier
     * @param rmId the room identifier
     */
    public AbstractAllocation(final String blId, final String flId, final String rmId) {
        super();
        this.blId = blId;
        this.flId = flId;
        this.rmId = rmId;
    }
    
    /**
     * {@inheritDoc}
     */
    public abstract Integer getId();
    
    /**
     * {@inheritDoc}
     */
    @XmlElement(nillable = true, required = false)
    public final Date getRejectedDate() {
        return this.rejectedDate;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setRejectedDate(final Date rejectedDate) {
        this.rejectedDate = rejectedDate;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setReservation(final IReservation reservation) {
        // set the id
        setReserveId(reservation.getReserveId());
        // copy from reservation
        setStartDate(reservation.getStartDate());
        setEndDate(reservation.getEndDate());
        setStartTime(reservation.getStartTime());
        setEndTime(reservation.getEndTime());
        setTimeZone(reservation.getTimeZone());
    }
    
    /**
     * 
     * get TimePeriod.
     * 
     * @return TimePeriod TimePeriod
     */
    @XmlTransient
    public final TimePeriod getTimePeriod() {
        return this.period;
    }
    
    /**
     * Sets the time period.
     * 
     * @param timePeriod the new time period
     */
    public final void setTimePeriod(final TimePeriod timePeriod) {
        this.setStartDate(timePeriod.getStartDate());
        this.setStartTime(timePeriod.getStartTime());
        this.setEndDate(timePeriod.getEndDate());
        this.setEndTime(timePeriod.getEndTime());
        this.setTimeZone(timePeriod.getTimeZone());
    }
    
    /**
     * {@inheritDoc}
     */
    public void copyTo(final AbstractAllocation allocation) {
        allocation.setCreatedBy(this.getCreatedBy());
        allocation.setCreationDate(this.getCreationDate());
        allocation.setLastModifiedBy(this.getLastModifiedBy());
        allocation.setLastModifiedDate(this.getLastModifiedDate());        
        // copy the location
        allocation.setBlId(this.getBlId());
        allocation.setFlId(this.getFlId());
        allocation.setRmId(this.getRmId());
        // copy common attributes
        allocation.setComments(this.getComments());
        allocation.setStatus(this.getStatus());
        allocation.setCost(this.getCost());
        allocation.setTimePeriod(this.getTimePeriod());
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getBlId() {
        return this.blId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getFlId() {
        return this.flId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getRmId() {
        return this.rmId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setBlId(final String blId) {
        this.blId = blId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setFlId(final String flId) {
        this.flId = flId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setRmId(final String rmId) {
        this.rmId = rmId;
    }
    
    /**
     * Gets the time zone.
     * 
     * @return the time zone
     */
    public final String getTimeZone() {
        return this.period.getTimeZone();
    }
    
    /**
     * Sets the time zone.
     * 
     * @param timeZone the new time zone
     */
    public final void setTimeZone(final String timeZone) {
        this.period.setTimeZone(timeZone);
    }
    
}
