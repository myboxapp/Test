package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.util.*;

import com.archibus.app.reservation.dao.IAllocationDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.util.DataSourceUtils;
import com.archibus.context.*;
import com.archibus.datasource.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.utility.*;

/**
 * Abstract datasource class for allocations of rooms or resources.
 * 
 * @param <T> the generic type
 * @author Bart Vanderschoot
 */
public abstract class AbstractAllocationDataSource<T extends AbstractAllocation> extends
ObjectDataSourceImpl<T> implements IAllocationDataSource<T> {

    /** Multiplier for a percentage value. */
    private static final Double PERCENTAGE_MULTIPLIER = 0.01;

    /**
     * Constructor.
     * 
     * @param beanName Spring bean name
     * @param tableName table name
     */
    protected AbstractAllocationDataSource(final String beanName, final String tableName) {
        super(beanName, tableName);
    }

    /**
     * Mapping of fields to properties.
     * 
     * to be compatible with version 19.
     * 
     * @return mapping
     */
    @Override
    protected Map<String, String> createFieldToPropertyMapping() {
        final Map<String, String> mapping = new HashMap<String, String>();
        mapping.put(this.tableName + ".res_id", "reserveId");
        mapping.put(this.tableName + ".status", "status");
        mapping.put(this.tableName + ".comments", "comments");

        mapping.put(this.tableName + ".user_last_modified_by", "lastModifiedBy");

        mapping.put(this.tableName + ".date_created", "creationDate");
        mapping.put(this.tableName + ".date_last_modified", "lastModifiedDate");

        mapping.put(this.tableName + ".date_rejected", "rejectedDate");
        mapping.put(this.tableName + ".date_cancelled", "cancelledDate");

        mapping.put(this.tableName + ".date_start", "startDate");
        mapping.put(this.tableName + ".time_start", "startTime");
        mapping.put(this.tableName + ".time_end", "endTime");

        mapping.put(this.tableName + ".bl_id", "blId");
        mapping.put(this.tableName + ".fl_id", "flId");
        mapping.put(this.tableName + ".rm_id", "rmId");

        return mapping;
    }

    /**
     * {@inheritDoc}
     */
    public final List<T> find(final IReservation reservation) {
        final DataSource dataSource = this.createCopy();

        if (reservation != null && reservation.getReserveId() != null) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, "res_id",
                    reservation.getReserveId()));
        }

        final List<DataRecord> records = dataSource.getRecords();
        return convertRecordsToObjects(records);
    }

    /**
     * Find all allocations adhering to the current restrictions of the data source.
     * 
     * @return list of allocations
     */
    public final List<T> findAll() {
        final DataSource dataSource = this.createCopy();
        final List<DataRecord> records = dataSource.getRecords();
        return convertRecordsToObjects(records);
    }

    /**
     * {@inheritDoc}
     */
    public final void checkAndUpdate(final T allocation) throws ReservationException {
        final User user = ContextStore.get().getUser();

        if (!user.isMemberOfGroup(Constants.RESERVATION_SERVICE_DESK)
                && !user.isMemberOfGroup(Constants.RESERVATION_MANAGER)) {
            checkEditing(allocation);
        }
        allocation.setLastModifiedBy(user.getEmployee().getId());
        // TODO timezone??
        allocation.setLastModifiedDate(Utility.currentDate());
        super.update(allocation);
    }

    /**
     * {@inheritDoc}
     */
    public void cancel(final T allocation) throws ReservationException {
        final User user = ContextStore.get().getUser();
        if (!user.isMemberOfGroup(Constants.RESERVATION_SERVICE_DESK)
                && !user.isMemberOfGroup(Constants.RESERVATION_MANAGER)) {
            checkCancelling(allocation);
        }

        allocation.setStatus(Constants.STATUS_CANCELLED);
        allocation.setLastModifiedBy(user.getEmployee().getId());
        calculateCancellationCost(allocation);
        allocation.setCancelledDate(Utility.currentDate());
        allocation.setLastModifiedDate(Utility.currentDate());

        super.update(allocation);
    }

    /**
     * Calculate the cancellation cost.
     * 
     * @param allocation the allocation to calculate the cost for
     * @param reservable the reservable object that is referred to in the allocation
     */
    protected void calculateCancellationCost(final T allocation, final IReservable reservable) {
        final Integer cancelDays = reservable.getCancelDays();
        final Time cancelTime = reservable.getCancelTime();
        final String blId = allocation.getBlId();

        // get local time using the building location
        final Date localCurrentDate =
                TimePeriod.clearTime(LocalDateTimeUtil.currentLocalDate(null, null, null, blId));
        final Time localCurrentTime = LocalDateTimeUtil.currentLocalTime(null, null, null, blId);
        final long daysDifference = DataSourceUtils.getDaysDifference(allocation, localCurrentDate);

        boolean lateCancellation = false;
        if (cancelDays != null) {
            if (daysDifference < cancelDays) {
                lateCancellation = true;
            } else if (cancelTime != null && daysDifference == cancelDays.longValue()
                    && localCurrentTime.after(cancelTime)) {
                lateCancellation = true;
            }
        }
        if (lateCancellation) {
            allocation.setCost(DataSourceUtils.round2(allocation.getCost()
                    * reservable.getCostLateCancelPercentage() * PERCENTAGE_MULTIPLIER));
        } else {
            allocation.setCost(0.0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T save(final T allocation) {
        final User user = ContextStore.get().getUser();
        this.log.debug("Allocation cost : " + allocation.getCost());

        allocation.setCreatedBy(user.getEmployee().getId());
        allocation.setCreationDate(Utility.currentDate());

        this.log
        .debug("Status for allocation " + allocation.getId() + " " + allocation.getStatus());

        return super.save(allocation);
    }

    /**
     * 
     * add Date Restriction.
     * 
     * @param dataSource data source
     * @param startDate start date
     * @param endDate end date
     */
    protected void addDateRestriction(final DataSource dataSource, final Date startDate,
            final Date endDate) {
        if (startDate != null) {
            dataSource.addRestriction(Restrictions.gte(this.tableName,
                    Constants.DATE_START_FIELD_NAME, startDate));
        }
        if (endDate != null) {
            dataSource.addRestriction(Restrictions.lte(this.tableName, "date_end", endDate));
        }
    }

    /**
     * add Room Restriction.
     * 
     * @param dataSource data source
     * @param blId building id
     * @param flId floor id
     * @param rmId room id
     */
    protected void addRoomRestriction(final DataSource dataSource, final String blId,
            final String flId, final String rmId) {
        if (blId != null) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, "bl_id", blId));
        }
        if (flId != null) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, "fl_id", flId));
        }
        if (rmId != null) {
            dataSource.addRestriction(Restrictions.eq(this.tableName, "rm_id", rmId));
        }
    }

    /**
     * 
     * Get allocation records.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param blId building id
     * @param flId floor id
     * @param rmId room id
     * @return list of data records
     */
    protected List<DataRecord> getAllocationRecords(final Date startDate, final Date endDate,
            final String blId, final String flId, final String rmId) {
        final DataSource dataSource = this.createCopy();

        addDateRestriction(dataSource, startDate, endDate);

        addRoomRestriction(dataSource, blId, flId, rmId);

        return dataSource.getRecords();
    }

    /**
     * Get number of units for cost calculation.
     * 
     * @param allocation allocation
     * @param reservable reservable object
     * 
     * @return units to calculate cost
     */
    protected double getCostUnits(final IAllocation allocation, final IReservable reservable) {
        double units = 0.0;
        // type of cost unit
        switch (reservable.getCostUnit()) {
            case Constants.COST_UNIT_RESERVATION:
                units = 1.0;
                break;
            case Constants.COST_UNIT_MINUTE:
                units = allocation.getTimePeriod().getMinutesDifference();
                break;
            case Constants.COST_UNIT_HOUR:
                units = allocation.getTimePeriod().getHoursDifference();
                break;
            case Constants.COST_UNIT_PARTIAL:
                units = allocation.getTimePeriod().getHoursDifference() / Constants.HALF_DAY_HOURS;
                break;
            case Constants.COST_UNIT_DAY:
                // a single reservation doesn't expand several days
                units = 1;
                break;
            default:
                break;
        }

        // Pay for each started unit.
        return Math.ceil(units);
    }

    /**
     * Get field to properties for version 20.
     * 
     * @return array of arrays.
     */
    @Override
    protected final String[][] getFieldsToProperties() {
        return DataSourceUtils.getFieldsToProperties(createFieldToPropertyMapping());
    }

}
