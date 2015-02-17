package com.archibus.app.reservation.domain;

import java.util.*;

import javax.xml.bind.annotation.*;

import com.archibus.app.common.organization.domain.Employee;
import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.recurrence.Recurrence;
import com.archibus.utility.StringUtil;

/**
 * Abstract reservation class.
 * 
 * Implements the Reservation interface.
 * 
 * @author Bart Vanderschoot
 * 
 *         <p>
 *         Suppressed warning "PMD.TooManyFields" in this class.
 *         <p>
 *         Justification: reservations have a large number of fields in the database
 * 
 *         <p>
 *         Suppressed warning "PMD.TooManyMethods" in this class.
 *         <p>
 *         Justification: many getter and methods required
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "AbstractReservation")
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.TooManyFields" })
public abstract class AbstractReservation extends AbstractReservationBase implements IReservation {
    
    /** The account id. */
    private String accountId;
    
    /** The attendees. */
    private String attendees;
    
    /** The contact. */
    private String contact;
    
    /** The department id. */
    private String departmentId;
    
    /** The division id. */
    private String divisionId;
    
    /** The email. */
    private String email;
    
    /** The requested by. */
    private String requestedBy;
    
    /** The requested for. */
    private String requestedFor;
    
    /**
     * parent ID when there is a recurrent reservation.
     */
    private Integer parentId;
    
    /** The phone. */
    private String phone;
    
    /**
     * The recurring rule is an XML format that describes the recurring type and settings.
     */
    private String recurringRule;
    
    /** The reservation name. */
    private String reservationName;
    
    /**
     * The reservation type might be 'regular' or 'recurring'.
     */
    private String reservationType;
    
    /** The resource allocations. */
    private List<ResourceAllocation> resourceAllocations;
    
    /**
     * Unique ID for reservation used in MS Exchange.
     */
    private String uniqueId;
    
    /** The recurrence pattern for creating a recurring reservation. */
    private Recurrence recurrence;
        
    /** The recurring date modified. */
    private int recurringDateModified;
    
    /**
     * Instantiates a new abstract reservation.
     */
    public AbstractReservation() {
        super();
    }
    
    /**
     * Instantiates a new abstract reservation.
     * 
     * @param reserveId the reserve id
     */
    public AbstractReservation(final Integer reserveId) {
        super(reserveId);
    }
    
    /**
     * Adds the resource allocation.
     * 
     * @param resourceAllocation the resource allocation
     */
    public final void addResourceAllocation(final ResourceAllocation resourceAllocation) {
        if (this.resourceAllocations == null) {
            this.resourceAllocations = new ArrayList<ResourceAllocation>();
        }
        // setReservation makes sure the date of time values are set to 1899
        this.resourceAllocations.add(resourceAllocation);
        resourceAllocation.setReservation(this);
    }
    
    /**
     * Remove a resource allocation.
     * 
     * @param resourceAllocation the resource allocation
     */
    public void removeResourceAllocation(final ResourceAllocation resourceAllocation) {
        this.resourceAllocations.remove(resourceAllocation);
    }
    
    /**
     * Get the active resource allocations in the reservation, i.e. those that don't have rejected
     * or cancelled status.
     * 
     * @return active resource allocations in the reservation
     */
    public List<ResourceAllocation> getActiveResourceAllocations() {
        final List<ResourceAllocation> activeAllocations = new ArrayList<ResourceAllocation>();
        for (final ResourceAllocation allocation : getResourceAllocations()) {
            if (!(Constants.STATUS_CANCELLED.equals(allocation.getStatus()) || Constants.STATUS_REJECTED
                .equals(allocation.getStatus()))) {
                activeAllocations.add(allocation);
            }
        }
        return activeAllocations;
    }
    
    /**
     * Copy to. Only change attributes that are allowed when editing
     *
     * @param reservation the reservation
     * @param allowDateChange the allow date change
     * @return the abstract reservation
     */
    public final AbstractReservation copyTo(final AbstractReservation reservation,
            final boolean allowDateChange) {
        
        if (StringUtil.isNullOrEmpty(reservation.getCreatedBy())) {
            reservation.setCreatedBy(this.getCreatedBy());
        }
        if (reservation.getCreationDate() == null) {
            reservation.setCreationDate(new Date());
        }
        if (allowDateChange) {
            reservation.setStartDate(this.getStartDate());
            reservation.setEndDate(this.getEndDate());
        }
        reservation.setEndTime(this.getEndTime());
        reservation.setStartTime(this.getStartTime());
        reservation.setRequestedBy(this.getRequestedBy());
        reservation.setRequestedFor(this.getRequestedFor());
        reservation.setReservationName(this.reservationName);

        /*
         * Don't change the target reservation status. If required, the datasource will update the
         * status when saving changes.
         */
        if (StringUtil.isNullOrEmpty(reservation.getStatus())) {
            reservation.setStatus(this.getStatus());
        }
        reservation.setContact(this.contact);
        reservation.setComments(this.getComments());
        reservation.setCost(this.getCost());
        reservation.setAccountId(this.accountId);
        reservation.setDepartmentId(this.departmentId);
        reservation.setDivisionId(this.divisionId);
        reservation.setEmail(this.email);
        reservation.setPhone(this.phone);

        reservation.setUniqueId(this.uniqueId);
        reservation.setParentId(this.getParentId());
        reservation.setRecurringRule(this.getRecurringRule());
        reservation.setReservationType(this.getReservationType());

        reservation.setTimeZone(this.getTimeZone());
        reservation.setAttendees(this.getAttendees());
        
        return reservation;
    }
    
    /**
     * Gets the account id.
     * 
     * @return the account id
     */
    public final String getAccountId() {
        return this.accountId;
    }
    
    /**
     * 
     * Gets the attendees.
     * 
     * @return the attendees
     * 
     * @see com.archibus.reservation.domain.IReservation#getAttendees()
     */
    public final String getAttendees() {
        return this.attendees;
    }
    
    /**
     * Gets the contact.
     * 
     * @return the contact
     */
    public final String getContact() {
        return this.contact;
    }
    
    /**
     * Gets the department id.
     * 
     * @return the department id
     */
    public final String getDepartmentId() {
        return this.departmentId;
    }
    
    /**
     * Gets the division id.
     * 
     * @return the division id
     */
    public final String getDivisionId() {
        return this.divisionId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getEmail() {
        return this.email;
    }
    
    /**
     * {@inheritDoc}
     */
    public final Integer getParentId() {
        return this.parentId;
    }
    
    /**
     * Gets the phone.
     * 
     * @return the phone
     */
    public final String getPhone() {
        return this.phone;
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getRecurringRule() {
        return this.recurringRule;
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getReservationName() {
        return this.reservationName;
    }
    
    /**
     * Gets the reservation type.
     * 
     * @return the reservation type
     */
    public final String getReservationType() {
        return this.reservationType;
    }
    
    /**
     * Gets the resource allocations.
     * 
     * @return list of resource allocations
     * 
     * @see com.archibus.reservation.domain.IReservation#getResourceAllocations()
     */
    public final List<ResourceAllocation> getResourceAllocations() {
        if (this.resourceAllocations == null) {
            this.resourceAllocations = new ArrayList<ResourceAllocation>();
        }
        return this.resourceAllocations;
    }
    
    /**
     * {@inheritDoc}
     */
    @XmlTransient
    public final TimePeriod getTimePeriod() {
        return this.period;
    }
    
    /**
     * Gets the unique id.
     * 
     * @return unique id
     * 
     * @see com.archibus.reservation.domain.IReservation#getUniqueId()
     */
    public final String getUniqueId() {
        return this.uniqueId;
    }
    
    /**
     * Sets the account id.
     * 
     * @param accountId the new account id
     */
    public final void setAccountId(final String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setAttendees(final String attendees) {
        this.attendees = attendees;
    }
    
    /**
     * Convert a reservation and its room allocations to the given time zone.
     * 
     * @param timeZoneId the target time zone
     */
    public void convertToTimeZone(final String timeZoneId) {
        this.setTimePeriod(getTimePeriodInTimeZone(timeZoneId));
        final Date startDateTime = this.getStartDateTime();
        final Date endDateTime = this.getEndDateTime();
        
        for (final ResourceAllocation resourceAllocation : this.getResourceAllocations()) {
            resourceAllocation.setStartDateTime(startDateTime);
            resourceAllocation.setEndDateTime(endDateTime);
            resourceAllocation.setTimeZone(timeZoneId);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void setRecurrence(final Recurrence recurrence) {
        this.recurrence = recurrence;
    }
    
    /**
     * {@inheritDoc}
     */
    public Recurrence getRecurrence() {
        return this.recurrence;
    }
    
    /**
     * Sets the contact.
     * 
     * @param contact the new contact
     */
    public final void setContact(final String contact) {
        this.contact = contact;
    }
    
    /**
     * Sets the department id.
     * 
     * @param departmentId the new department id
     */
    public final void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }
    
    /**
     * Sets the division id.
     * 
     * @param divisionId the new division id
     */
    public final void setDivisionId(final String divisionId) {
        this.divisionId = divisionId;
    }
    
    /**
     * Sets the email of the organizer.
     * 
     * @param email the new email
     */
    public final void setEmail(final String email) {
        this.email = email;
    }
    
    /**
     * Sets the parent id.
     * 
     * @param parentId the new parent id
     */
    public final void setParentId(final Integer parentId) {
        this.parentId = parentId;
    }
    
    /**
     * Sets the phone.
     * 
     * @param phone the new phone
     */
    public final void setPhone(final String phone) {
        this.phone = phone;
    }
    
    /**
     * Sets the recurring rule.
     * 
     * @param recurringRule the new recurring rule
     */
    public final void setRecurringRule(final String recurringRule) {
        this.recurringRule = recurringRule;
        if (StringUtil.notNullOrEmpty(recurringRule)) {
            this.setReservationType(Constants.TYPE_RECURRING);
        } else {
            this.setReservationType(Constants.TYPE_REGULAR);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setReservationName(final String reservationName) {
        this.reservationName = reservationName;
    }
    
    /**
     * Sets the reservation type.
     * 
     * @param reservationType the new reservation type
     */
    public final void setReservationType(final String reservationType) {
        this.reservationType = reservationType;
    }
    
    /**
     * Sets the resource allocations.
     * 
     * @param resourceAllocations the new resource allocations
     */
    public final void setResourceAllocations(final List<ResourceAllocation> resourceAllocations) {
        this.resourceAllocations = resourceAllocations;
    }
    
    /**
     * Set the time period.
     * 
     * @param timePeriod time period
     */
    public final void setTimePeriod(final TimePeriod timePeriod) {
        this.setStartDate(timePeriod.getStartDate());
        this.setStartTime(timePeriod.getStartTime());
        this.setEndDate(timePeriod.getEndDate());
        this.setEndTime(timePeriod.getEndTime());
        this.setTimeZone(timePeriod.getTimeZone());
    }
    
    /**
     * Sets the unique id.
     * 
     * @param uniqueId the new unique id
     */
    public final void setUniqueId(final String uniqueId) {
        this.uniqueId = uniqueId;
    }
    
    /**
     * Gets the time zone.
     * 
     * @return the time zone
     * 
     * @see com.archibus.app.reservation.domain.IReservation#getTimeZone()
     */
    public final String getTimeZone() {
        return this.period.getTimeZone();
    }
    
    /**
     * Sets the time zone.
     * 
     * The time zone is not saved in the database.
     * 
     * @param timeZone the new time zone
     */
    public final void setTimeZone(final String timeZone) {
        this.period.setTimeZone(timeZone);
    }
    
    /**
     * Gets the requested by.
     * 
     * @return requested by
     * 
     * @see com.archibus.reservation.domain.IReservation#getRequestedBy()
     */
    public final String getRequestedBy() {
        return this.requestedBy;
    }
    
    /**
     * Gets the requested for.
     * 
     * @return requested for
     * 
     * @see com.archibus.reservation.domain.IReservation#getRequestedFor()
     */
    public final String getRequestedFor() {
        return this.requestedFor;
    }
    
    /**
     * Sets the requested by.
     * 
     * @param requestedBy the new requested by
     */
    public final void setRequestedBy(final String requestedBy) {
        this.requestedBy = requestedBy;
    }
    
    /**
     * Sets the requested for.
     * 
     * @param requestedFor the new requested for
     */
    public final void setRequestedFor(final String requestedFor) {
        this.requestedFor = requestedFor;
    }

    /**
     * Set the creator of this reservation to be the given employee.
     * 
     * @param creator the employee to set as creator of the reservation.
     */
    public void setCreator(final Employee creator) {
        this.setRequestedBy(creator.getId());
        this.setRequestedFor(creator.getId());
        this.setDepartmentId(creator.getDepartmentId());
        this.setDivisionId(creator.getDivisionId());
        this.setCreatedBy(creator.getId());
        this.setCreationDate(new Date());
    }
        

    /**
     * Gets the recurring date modified.
     *
     * @return the recurring date modified
     */
    @XmlTransient
    public int getRecurringDateModified() {
        return recurringDateModified;
    }

    /**
     * Sets the recurring date modified.
     *
     * @param recurringDateModified the new recurring date modified
     */
    public void setRecurringDateModified(final int recurringDateModified) {
        this.recurringDateModified = recurringDateModified;
    }
    
}
