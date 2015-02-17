package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.util.*;

import com.archibus.app.reservation.dao.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.util.DataSourceUtils;
import com.archibus.datasource.DataSource;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.utility.*;

/**
 * The Class ResourceAllocationDataSource.
 * 
 * @author Bart Vanderschoot
 */
public class ResourceAllocationDataSource extends AbstractAllocationDataSource<ResourceAllocation>
implements IResourceAllocationDataSource {

    /** resourceDataSource resourceDataSource. */
    private IResourceDataSource resourceDataSource;

    /**
     * Default constructor.
     */
    public ResourceAllocationDataSource() {
        this("resourceAllocation", "reserve_rs");
    }

    /**
     * Constructor using parameters.
     * 
     * @param beanName the bean name
     * @param tableName the table name
     */
    protected ResourceAllocationDataSource(final String beanName, final String tableName) {
        super(beanName, tableName);
    }

    /**
     * {@inheritDoc}
     */
    public final List<ResourceAllocation> getResourceAllocations(final IReservation reservation) {
        return this.find(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public final List<ResourceAllocation> getResourceAllocations(final Date startDate,
            final Date endDate, final String blId, final String flId, final String rmId) {
        final List<DataRecord> records = getAllocationRecords(startDate, endDate, blId, flId, rmId);

        return convertRecordsToObjects(records);
    }

    /**
     * {@inheritDoc}
     */
    public final List<ResourceAllocation> getResourceAllocations(final Date startDate,
            final String blId, final String flId, final String rmId) {
        return getResourceAllocations(startDate, null, blId, flId, rmId);
    }

    /**
     * {@inheritDoc}
     */
    public void cancelOther(final IReservation reservation) { 
        // create a list of the new reservation
        final List<Integer> ids = new ArrayList<Integer>();

        for (ResourceAllocation resourceAllocation : reservation.getResourceAllocations()) {
            ids.add(resourceAllocation.getId());
        }

        // loop through the resource allocations in the database
        for (ResourceAllocation allocation : this.getResourceAllocations(reservation)) {
            // when the resource allocation is removed, 
            if (!ids.contains(allocation.getId())) {
                this.cancel(allocation);
            }
        } 
    }

    /**
     * Check canceling.
     * 
     * @param allocation the allocation
     * @throws ReservationException the reservation exception
     */
    public void checkCancelling(final ResourceAllocation allocation) throws ReservationException {
        final Resource resource = this.resourceDataSource.get(allocation.getResourceId());

        final Date localCurrentDate = getLocalCurrentDateForResource(resource);
        final Time localCurrentTime = getLocalCurrentTimeForResource(resource);

        if (!checkIfResourceExists(allocation, localCurrentDate)) {
            // @translatable
            throw new ReservableNotAvailableException(resource,
                "The reservation cannot be cancelled, resource allocation not found",
                ResourceAllocationDataSource.class);
        }

        final Integer cancelDays = resource.getCancelDays();
        final Time cancelTime = resource.getCancelTime();

        final long daysDifference = DataSourceUtils.getDaysDifference(allocation, localCurrentDate);

        // make sure the days before canceling is respected
        if (cancelDays != null && daysDifference < cancelDays) {
            // @translatable
            throw new ReservableNotAvailableException(resource,
                "The reservation cannot be cancelled, resource cancel days expired",
                ResourceAllocationDataSource.class);
        }

        // make sure the cancel time is respected
        if (cancelDays != null && cancelTime != null && daysDifference == cancelDays.longValue()
                && localCurrentTime.after(cancelTime)) {
            // @translatable
            throw new ReservableNotAvailableException(resource,
                "The reservation cannot be cancelled, resource cancel time expired",
                ResourceAllocationDataSource.class);
        }
    }

    /** {@inheritDoc} */
    public void calculateCancellationCost(final ResourceAllocation allocation) {
        calculateCancellationCost(allocation,
                this.resourceDataSource.get(allocation.getResourceId()));
    }

    /**
     * Check if resource allocation exists in the database.
     * 
     * @param allocation the resource allocation
     * @param localCurrentDate local current date for the resource
     * @return true is resource allocation found in the database
     */
    private Boolean checkIfResourceExists(final ResourceAllocation allocation,
            final Date localCurrentDate) {
        final DataSource dataSource = this.createCopy();

        dataSource.addRestriction(Restrictions.eq(this.tableName, Constants.RSRES_ID_FIELD_NAME,
                allocation.getId()));
        dataSource.addRestriction(Restrictions
                .sql(Constants.STATUS_AWAITING_APP_OR_STATUS_CONFIRMED));
        // make sure cancel date is before start date
        dataSource.addRestriction(Restrictions.gt(this.tableName, Constants.DATE_START_FIELD_NAME,
                localCurrentDate));

        final DataRecord record = dataSource.getRecord();
        return record != null;
    }

    /**
     * Check editing.
     * 
     * @param allocation the allocation
     * @throws ReservationException the reservation exception
     */
    public void checkEditing(final ResourceAllocation allocation) throws ReservationException {

        final Resource resource = this.resourceDataSource.get(allocation.getResourceId());

        final Date localCurrentDate = getLocalCurrentDateForResource(resource);
        final Time localCurrentTime = getLocalCurrentTimeForResource(resource);

        if (!checkIfResourceExists(allocation, localCurrentDate)) {
            // @translatable
            throw new ReservableNotAvailableException(resource,
                "The reservation cannot be modified, resource allocation not found",
                ResourceAllocationDataSource.class);
        }

        final Integer announceDays = resource.getAnnounceDays();
        final Time announceTime = resource.getAnnounceTime();

        final long daysDifference = DataSourceUtils.getDaysDifference(allocation, localCurrentDate);

        // make sure the days before announce is respected
        if (announceDays != null && daysDifference < announceDays) {
            // @translatable
            throw new ReservableNotAvailableException(resource,
                "The reservation cannot be modified, resource announce days expired",
                ResourceAllocationDataSource.class);
        }

        // make sure the announce time is respected
        if (announceDays != null && announceTime != null
                && daysDifference == announceDays.longValue()
                && localCurrentTime.after(announceTime)) {
            // @translatable
            throw new ReservableNotAvailableException(resource,
                "The reservation cannot be modified, resource announce time expired",
                ResourceAllocationDataSource.class);
        }

    }

    /**
     * Create fields to properties mapping. To be compatible with version 19.
     * 
     * @return mapping
     */
    @Override
    public Map<String, String> createFieldToPropertyMapping() {
        final Map<String, String> mapping = super.createFieldToPropertyMapping();
        mapping.put(this.tableName + ".rsres_id", "id");
        mapping.put(this.tableName + ".resource_id", "resourceId");
        mapping.put(this.tableName + ".quantity", "quantity");
        mapping.put(this.tableName + ".cost_rsres", "cost");

        return mapping;
    }

    /**
     * Calculate the total cost for the allocation.
     * 
     * @param allocation resource allocation
     */
    public void calculateCost(final ResourceAllocation allocation) {
        final IReservable reservable = this.resourceDataSource.get(allocation.getResourceId());

        final double units = getCostUnits(allocation, reservable);

        // check on external ??
        final double costPerUnit = reservable.getCostPerUnit();
        final int quantity = allocation.getQuantity();
        // calculate cost and round to 2 decimals
        final double cost = DataSourceUtils.round2(costPerUnit * units * quantity);

        allocation.setCost(cost);
    }

    /**
     * Setter for ResourceDataSource.
     * 
     * @param resourceDataSource resourceDataSource to set
     */
    public void setResourceDataSource(final IResourceDataSource resourceDataSource) {
        this.resourceDataSource = resourceDataSource;
    }

    /**
     * Get Local Current Date For Resource.
     * 
     * @param resource the resource
     * @return local current date
     */
    private Date getLocalCurrentDateForResource(final Resource resource) {
        Date localCurrentDate = null;
        if (resource.getSiteId() == null && resource.getBlId() == null) {
            localCurrentDate = TimePeriod.clearTime(Utility.currentDate());
        } else {
            localCurrentDate =
                    TimePeriod.clearTime(LocalDateTimeUtil.currentLocalDate(null, null,
                            resource.getSiteId(), resource.getBlId()));
        }
        return localCurrentDate;
    }

    /**
     * Get local current time for resource.
     * 
     * @param resource the resource
     * @return local current time
     */
    private Time getLocalCurrentTimeForResource(final Resource resource) {
        Time localCurrentTime = null;
        if (resource.getSiteId() == null && resource.getBlId() == null) {
            localCurrentTime = Utility.currentTime();
        } else {
            localCurrentTime =
                    LocalDateTimeUtil.currentLocalTime(null, null, resource.getSiteId(),
                            resource.getBlId());
        }
        return localCurrentTime;
    }



}
