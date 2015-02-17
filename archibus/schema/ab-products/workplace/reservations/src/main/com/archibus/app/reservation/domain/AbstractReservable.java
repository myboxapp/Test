package com.archibus.app.reservation.domain;

import java.sql.Time;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Domain class for Reservable objects.
 * 
 * Every reservable object (room or resources) should inherit from this base class.
 * 
 * @author Bart Vanderschoot
 *         <p>
 *         Suppressed warning "PMD.TooManyFields" in this class.
 *         <p>
 *         Justification: reservables have a large number of fields in the database
 * 
 *         <p>
 *         Suppressed warning "PMD.TooManyMethods" in this class.
 *         <p>
 *         Justification: many getter and setter methods required
 */
@SuppressWarnings({ "PMD.TooManyFields", "PMD.TooManyMethods" })
public abstract class AbstractReservable implements IReservable {
    
    /** The ac id. */
    protected String acId;
    
    /** The announce days. */
    protected Integer announceDays;
    
    /** The announce time. */
    @XmlJavaTypeAdapter(TimeAdapter.class)
    protected Time announceTime;
    
    /** check if needs approval. */
    protected Integer approvalRequired;
    
    /** The approval days. */
    protected Integer approvalDays;
    
    /** The available for group. */
    protected String availableForGroup;
    
    /** The cancel days. */
    protected Integer cancelDays;
    
    /** The cancel time. */
    @XmlJavaTypeAdapter(TimeAdapter.class)
    protected Time cancelTime;
    
    /** The day end. */
    protected Time dayEnd;
    
    /** The day start. */
    protected Time dayStart;
    
    /** The doc image. */
    protected String docImage;
    
    /** The max days ahead. */
    protected Integer maxDaysAhead;
    
    /** The post block. */
    protected Integer postBlock;
    
    /** The pre block. */
    protected Integer preBlock;
    
    /** The reservable. */
    protected Integer reservable;
    
    /** The security group name. */
    protected String securityGroupName;
    
    /** cost per unit. */
    protected double costPerUnit;
    
    /** cost per unit. */
    protected double costPerUnitExternal;
    
    /**
     * TODO: create enumeration, since it is unlikely this will change
     * cost unit. 0;Reservation;1;Minute;2;Hour;3;Partial Day;4;Day
     */
    protected int costUnit;
    
    /** cost late cancel percentage. */
    protected int costLateCancelPercentage;
    
    /** {@inheritDoc} */
    
    public final String getAcId() {
        return this.acId;
    }
    
    /** {@inheritDoc} */
    
    public final Integer getAnnounceDays() {
        return this.announceDays;
    }
    
    /** {@inheritDoc} */
    
    @XmlTransient
    public final Time getAnnounceTime() {
        return this.announceTime;
    }
    
    /** {@inheritDoc} */
    
    public final Integer getApprovalDays() {
        return this.approvalDays;
    }
    
    /** {@inheritDoc} */
    
    public final String getAvailableForGroup() {
        return this.availableForGroup;
    }
    
    /** {@inheritDoc} */
    
    public final Integer getCancelDays() {
        return this.cancelDays;
    }
    
    /** {@inheritDoc} */
    
    @XmlTransient
    public final Time getCancelTime() {
        return this.cancelTime;
    }
    
    /** {@inheritDoc} */
    
    public final Time getDayEnd() {
        return this.dayEnd;
    }
    
    /** {@inheritDoc} */
    
    public final Time getDayStart() {
        return this.dayStart;
    }
    
    /** {@inheritDoc} */
    
    public final String getDocImage() {
        return this.docImage;
    }
    
    /** {@inheritDoc} */
    
    public final Integer getMaxDaysAhead() {
        return this.maxDaysAhead;
    }
    
    /** {@inheritDoc} */
    
    public final Integer getPostBlock() {
        return this.postBlock;
    }
    
    /** {@inheritDoc} */
    
    public final Integer getPreBlock() {
        return this.preBlock;
    }
    
    /** {@inheritDoc} */
    
    public final Integer getReservable() {
        return this.reservable;
    }
    
    /** {@inheritDoc} */
    
    public final String getSecurityGroupName() {
        return this.securityGroupName;
    }
    
    /** {@inheritDoc} */
    
    public final void setAcId(final String acId) {
        this.acId = acId;
    }
    
    /** {@inheritDoc} */
    
    public final void setAnnounceDays(final Integer announceDays) {
        this.announceDays = announceDays;
    }
    
    /** {@inheritDoc} */
    
    public final void setAnnounceTime(final Time announceTime) {
        this.announceTime = announceTime;
    }
    
    /** {@inheritDoc} */
    
    public final void setApprovalDays(final Integer approvalDays) {
        this.approvalDays = approvalDays;
    }
    
    /** {@inheritDoc} */
    
    public final void setAvailableForGroup(final String availableForGroup) {
        this.availableForGroup = availableForGroup;
    }
    
    /** {@inheritDoc} */
    
    public final void setCancelDays(final Integer cancelDays) {
        this.cancelDays = cancelDays;
    }
    
    /** {@inheritDoc} */
    
    public final void setCancelTime(final Time cancelTime) {
        this.cancelTime = cancelTime;
    }
    
    /** {@inheritDoc} */
    
    public final void setDayEnd(final Time dayEnd) {
        this.dayEnd = dayEnd;
    }
    
    /** {@inheritDoc} */
    
    public final void setDayStart(final Time dayStart) {
        this.dayStart = dayStart;
    }
    
    /** {@inheritDoc} */
    
    public final void setDocImage(final String docImage) {
        this.docImage = docImage;
    }
    
    /** {@inheritDoc} */
    
    public final void setMaxDaysAhead(final Integer maxDaysAhead) {
        this.maxDaysAhead = maxDaysAhead;
    }
    
    /** {@inheritDoc} */
    
    public final void setPostBlock(final Integer postBlock) {
        this.postBlock = postBlock;
    }
    
    /** {@inheritDoc} */
    
    public final void setPreBlock(final Integer preBlock) {
        this.preBlock = preBlock;
    }
    
    /** {@inheritDoc} */
    
    public final void setReservable(final Integer reservable) {
        this.reservable = reservable;
    }
    
    /** {@inheritDoc} */
    
    public final void setSecurityGroupName(final String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }
    
    /**
     * {@inheritDoc}
     */
    public Integer getApprovalRequired() {
        return this.approvalRequired;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setApprovalRequired(final Integer approvalRequired) {
        this.approvalRequired = approvalRequired;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getCostPerUnit() {
        return this.costPerUnit;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setCostPerUnit(final double costPerUnit) {
        this.costPerUnit = costPerUnit;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getCostPerUnitExternal() {
        return this.costPerUnitExternal;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setCostPerUnitExternal(final double costPerUnitExternal) {
        this.costPerUnitExternal = costPerUnitExternal;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getCostUnit() {
        return this.costUnit;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setCostUnit(final int costUnit) {
        this.costUnit = costUnit;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getCostLateCancelPercentage() {
        return this.costLateCancelPercentage;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setCostLateCancelPercentage(final int costLateCancelPercentage) {
        this.costLateCancelPercentage = costLateCancelPercentage;
    }
    
}
