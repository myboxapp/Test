package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.util.*;

import com.archibus.app.reservation.dao.IResourceDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;

/**
 * DataSource for Resources.
 * 
 * @author Bart Vanderschoot
 */
public abstract class AbstractResourceDataSource extends AbstractReservableDataSource<Resource>
implements IResourceDataSource {

    /** Field for resource type. */
    private static final String RESOURCE_TYPE_FIELD = "resource_type";

    /** Field for quantity. */
    private static final String QUANTITY = "quantity";

    /** Resource reservations table name. */
    private static final String RESERVE_RS_TABLE = "reserve_rs";

    /**
     * Default Constructor.
     */
    public AbstractResourceDataSource() {
        this("resourceBean", "resources");
    }

    /**
     * Private constructor.
     * 
     * @param beanName Spring bean name
     * @param tableName table name
     */
    private AbstractResourceDataSource(final String beanName, final String tableName) {
        super(beanName, tableName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Suppress PMD warning "AvoidUsingSql" in this method.
     * <p>
     * Justification: Case #1: Statement with SELECT WHERE EXISTS ... pattern.
     */
    @SuppressWarnings("PMD.AvoidUsingSql")
    public List<DataRecord> findAvailableLimitedResourceRecords(final IReservation reservation,
            final TimePeriod timePeriod) throws ReservationException {

        final DataSource dataSource = this.createCopy();

        dataSource.addRestriction(Restrictions.eq(this.tableName, RESOURCE_TYPE_FIELD,
                ResourceType.LIMITED.toString()));

        if (timePeriod.getStartDate() != null && timePeriod.getStartTime() != null
                && timePeriod.getEndTime() != null) {

            final String editRestriction = defineEditRestriction(reservation, dataSource);
            // no resources have been reserved for this date/time
            String limitedReserved =
                    " NOT EXISTS (SELECT reserve_rs.resource_id FROM reserve_rs LEFT OUTER JOIN resources rs ON reserve_rs.resource_id = rs.resource_id "
                            + " WHERE (reserve_rs.status = 'Awaiting App.' or reserve_rs.status = 'Confirmed') AND "
                            + editRestriction
                            + " reserve_rs.resource_id = resources.resource_id and reserve_rs.date_start = ${parameters['startDate']} AND "
                            + getOverlappingReservationRestriction(dataSource) + " )";

            // or there are still available
            limitedReserved +=
                    " OR EXISTS (SELECT reserve_rs.resource_id FROM reserve_rs WHERE (reserve_rs.status = 'Awaiting App.' or reserve_rs.status = 'Confirmed') AND "
                            + editRestriction + " reserve_rs.resource_id = resources.resource_id"
                            + " AND reserve_rs.date_start = ${parameters['startDate']} AND "
                            + getOverlappingReservationRestriction(dataSource)
                            + " GROUP BY reserve_rs.resource_id"
                            + " HAVING resources.quantity > SUM(reserve_rs.quantity) )";

            dataSource.addRestriction(Restrictions.sql(limitedReserved));
        }

        addRestrictions(dataSource, reservation, timePeriod, false,
                reservation.determineCurrentLocalDate(), reservation.determineCurrentLocalTime());
        return dataSource.getRecords();

    }

    /**
     * {@inheritDoc}
     * <p>
     * Suppress PMD warning "AvoidUsingSql" in this method.
     * <p>
     * Justification: Case #1: Statement with SELECT WHERE EXISTS ... pattern.
     */
    @SuppressWarnings("PMD.AvoidUsingSql")
    public List<DataRecord> findAvailableUniqueResourceRecords(final IReservation reservation,
            final TimePeriod timePeriod) throws ReservationException {
        final DataSource dataSource = this.createCopy();
        dataSource.addRestriction(Restrictions.eq(this.tableName, RESOURCE_TYPE_FIELD,
                ResourceType.UNIQUE.toString()));

        if (reservation.getStartDate() != null && reservation.getStartTime() != null
                && reservation.getEndTime() != null) {

            final String editRestriction = defineEditRestriction(reservation, dataSource);

            // check if the reservation overlaps other reservations.
            final String uniqueNotReserved =
                    " NOT EXISTS (SELECT res_id FROM reserve_rs WHERE "
                            + editRestriction
                            + " reserve_rs.resource_id = resources.resource_id "
                            + " AND reserve_rs.date_start = ${parameters['startDate']} "
                            + " AND (reserve_rs.status = 'Awaiting App.' or reserve_rs.status = 'Confirmed') AND "
                            + getOverlappingReservationRestriction(dataSource)
                            + Constants.RIGHT_PAR;

            dataSource.addRestriction(Restrictions.sql(uniqueNotReserved));
        }

        addRestrictions(dataSource, reservation, timePeriod, false,
                reservation.determineCurrentLocalDate(), reservation.determineCurrentLocalTime());
        return dataSource.getRecords();
    }
 
 

    /**
     * Find available resource records for existing recurring reservation.
     *
     * @param reservation the reservation currently being edited (whose time period to use)
     * @param reservations the recurring reservations
     * @param resourceType the resource type
     * @return the list of data records
     * @throws ReservationException the reservation exception
     */
    public List<DataRecord> findAvailableResourceRecords(final IReservation reservation,
            final Collection<? extends IReservation> reservations,
            final ResourceType resourceType) throws ReservationException {
        // only resource objects can be used with retainAll
        List<Resource> resources = null;
        for (IReservation occurrence : reservations) {
            // get the unique resources available for this occurrence
            List<Resource> results = null;
            occurrence.setStartTime(reservation.getStartTime());
            occurrence.setEndTime(reservation.getEndTime());

            if (resourceType.equals(ResourceType.LIMITED)) {
                results = findAvailableLimitedResources(occurrence, occurrence.getTimePeriod());

            } else if (resourceType.equals(ResourceType.UNIQUE)) {
                results = findAvailableUniqueResources(occurrence, occurrence.getTimePeriod());
            }

            if (resources == null) {
                // initialize
                resources = results; 
            } else {
                resources.retainAll(results);  
            }
        }

        // return as data records
        return this.convertObjectsToRecords(resources);
    }



    /**
     * {@inheritDoc}
     */
    public int getNumberOfReservedResources(final TimePeriod timePeriod, final String resourceId,
            final Integer reserveId, final boolean includePreAndPostBlocks) {
        final DataSourceGroupingImpl dataSource = new DataSourceGroupingImpl();

        // Join the reserve_rs and resources tables.
        dataSource.addTable(RESERVE_RS_TABLE);
        dataSource.addTable(this.tableName, DataSource.ROLE_STANDARD);

        // Define the calculated total number of reserved resources, grouped by resource_id.
        dataSource.addCalculatedField(this.tableName, "total", DataSource.DATA_TYPE_INTEGER,
                DataSourceGroupingImpl.FORMULA_SUM, RESERVE_RS_TABLE + Constants.DOT + QUANTITY);
        dataSource.addGroupByField(this.tableName, Constants.RESOURCE_ID_FIELD,
                DataSource.DATA_TYPE_TEXT);

        // Ignore cancelled and rejected reservations.
        dataSource.addRestriction(Restrictions.notIn(RESERVE_RS_TABLE, Constants.STATUS,
                "Cancelled,Rejected"));

        if (reserveId != null) {
            // Ignore the resources reserved for the given reservation.
            dataSource.addRestriction(Restrictions
                    .ne(RESERVE_RS_TABLE, Constants.RES_ID, reserveId));
        }

        // Get the count for a particular resource and date.
        dataSource.addRestriction(Restrictions.eq(RESERVE_RS_TABLE, Constants.RESOURCE_ID_FIELD,
                resourceId));
        // the start date should always be required.
        dataSource.addRestriction(Restrictions.eq(RESERVE_RS_TABLE, "date_start",
                timePeriod.getStartDate()));

        // Count the existing reservations that overlap the given time period.
        if (timePeriod.getStartTime() != null && timePeriod.getEndTime() != null) {
            if (includePreAndPostBlocks) {
                dataSource.addParameter("startTime", timePeriod.getStartTime(),
                        DataSource.DATA_TYPE_TIME);
                dataSource.addParameter("endTime", timePeriod.getEndTime(),
                        DataSource.DATA_TYPE_TIME);
                dataSource.addRestriction(Restrictions
                        .sql(getOverlappingReservationRestriction(dataSource)));
            } else {
                // only count reservations that really overlap with the given time period
                dataSource.addRestriction(Restrictions.gt(RESERVE_RS_TABLE, "time_end",
                        timePeriod.getStartTime()));
                dataSource.addRestriction(Restrictions.lt(RESERVE_RS_TABLE, "time_start",
                        timePeriod.getEndTime()));
            }
        }

        final DataRecord record = dataSource.getRecord();

        return (record == null) ? 0 : record.getInt(this.tableName + ".total");
    }

    /**
     * Get Overlapping Reservation Restriction.
     * 
     * @param dataSource dataSource
     * @return sql restriction
     */
    private String getOverlappingReservationRestriction(final DataSource dataSource) {
        String sql = null;
        if (dataSource.isOracle()) {
            sql =
                    " ( reserve_rs.time_start - (resources.pre_block + resources.post_block) / (24*60) < ${parameters['endTime']} ) "
                            + " and ( reserve_rs.time_end + (resources.pre_block + resources.post_block) / (24*60) > ${parameters['startTime']} )  ";

        } else if (dataSource.isSqlServer()) {
            sql =
                    " ( DATEADD(mi, -resources.pre_block - resources.post_block, reserve_rs.time_start) < ${parameters['endTime']}) "
                            + " and ( DATEADD(mi, resources.pre_block + resources.post_block, reserve_rs.time_end) > ${parameters['startTime']}) ";

        } else {
            sql =
                    " ( Convert(char(10), DATEADD(mi, -resources.pre_block - resources.post_block, reserve_rs.time_start), 108) < Convert(char(10), ${parameters['endTime']}, 108) ) "
                            + " and ( Convert(char(10), DATEADD(mi, resources.pre_block + resources.post_block, reserve_rs.time_end), 108) > Convert(char(10), ${parameters['startTime']}, 108) ) ";

        }

        return sql;
    }

    /**
     * {@inheritDoc}
     */
    public List<DataRecord> findAvailableUnlimitedResourceRecords(final IReservation reservation,
            final TimePeriod timePeriod, final boolean allowPartialAvailability)
            throws ReservationException {
        final DataSource dataSource = this.createCopy();
        
        dataSource.addRestriction(Restrictions.eq(this.tableName, RESOURCE_TYPE_FIELD,
            ResourceType.UNLIMITED.toString()));
        
        addRestrictions(dataSource, reservation, timePeriod, allowPartialAvailability,
            reservation.determineCurrentLocalDate(), reservation.determineCurrentLocalTime());
        
        return dataSource.getRecords();
    }

    /**
     * Add Restrictions.
     * 
     * @param dataSource data source
     * @param reservation reservation object
     * @param timePeriod the time period to use for the restrictions
     * @param allowPartialAvailability whether to return resources that are only available for part
     *            of the chosen time frame
     * @param localCurrentDate local current date
     * @param localCurrentTime local current time
     * @throws ReservationException the reservation exception
     */
    protected void addRestrictions(final DataSource dataSource, final IReservation reservation,
            final TimePeriod timePeriod, final boolean allowPartialAvailability,
            final Date localCurrentDate, final Time localCurrentTime) throws ReservationException {
        dataSource.addRestriction(Restrictions.eq(this.tableName, "reservable", 1));

        addTimePeriodParameters(dataSource, timePeriod);
        ResourceDataSourceRestrictionsHelper.addLocationRestriction(dataSource, reservation);

        addAnnounceRestriction(dataSource, timePeriod, localCurrentDate, localCurrentTime);
        addMaxDayAheadRestriction(dataSource, timePeriod, localCurrentDate);

        addDayStartEndRestriction(dataSource, timePeriod, allowPartialAvailability);

        addSecurityRestriction(dataSource);
    }

    /**
     * Mapping of fields to properties.
     * 
     * @return mapping
     */
    @Override
    public Map<String, String> createFieldToPropertyMapping() {
        final Map<String, String> mapping = super.createFieldToPropertyMapping();

        mapping.put(this.tableName + ".resource_id", "resourceId");

        mapping.put(this.tableName + ".resource_type", "resourceType");
        mapping.put(this.tableName + ".resource_std", "resourceStandard");
        mapping.put(this.tableName + ".resource_name", "resourceName");

        mapping.put(this.tableName + ".site_id", "siteId");
        mapping.put(this.tableName + ".bl_id", Constants.BL_ID_PARAMETER);

        mapping.put(this.tableName + ".quantity", QUANTITY);

        return mapping;
    }

    /**
     * Define a restriction that excludes resources reserved for the given reservation.
     * 
     * @param reservation the reservation to exclude
     * @param dataSource the data source to define the restriction for
     * @return SQL string containing the restriction: either it's empty or it contains the
     *         restriction and ends with AND.
     */
    private String defineEditRestriction(final IReservation reservation, final DataSource dataSource) {
        String editRestriction = "";
        if (reservation.getReserveId() != null) {
            dataSource.addParameter("reserveId", reservation.getReserveId(),
                    DataSource.DATA_TYPE_INTEGER);
            editRestriction = " reserve_rs.res_id <> ${parameters['reserveId']} and ";
        }
        return editRestriction;
    }

}
