package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.util.*;

import com.archibus.datasource.DataSource;
import com.archibus.datasource.restriction.Restrictions;

/**
 * Utility class. Provides methods to add restrictions to a data source.
 * <p>
 * Used by RoomArrangmentDataSource to add specific restrictions.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 * 
 */
public final class RoomArrangementDataSourceRestrictionsHelper {
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private RoomArrangementDataSourceRestrictionsHelper() {
    }
    
    /**
     * Adds the fixed resources restriction.
     * 
     * @param fixedResourceStandards the fixed resource standards
     * @param dataSource the data source
     * 
     *            <p>
     *            Suppress PMD warning "AvoidUsingSql" in this method.
     *            <p>
     *            Justification: Case #1: Statement with SELECT WHERE EXISTS ... pattern.
     * 
     */
    @SuppressWarnings("PMD.AvoidUsingSql")
    public static void addFixedResourcesRestriction(final List<String> fixedResourceStandards,
            final DataSource dataSource) {
        if (fixedResourceStandards != null && !fixedResourceStandards.isEmpty()) {
            int parameterIndex = 0;
            for (final String resourceStd : fixedResourceStandards) {
                // Use a different parameter name for each restriction.
                final String parameterName =
                        Constants.RESOURCE_STD_PARAMETER_NAME + ++parameterIndex;
                dataSource.addParameter(parameterName, resourceStd, DataSource.DATA_TYPE_TEXT);
                final String restriction =
                        " EXISTS (select resource_std from rm_resource_std where rm_resource_std.bl_id = rm_arrange.bl_id and rm_resource_std.fl_id = rm_arrange.fl_id and rm_resource_std.rm_id = rm_arrange.rm_id  "
                                + " and rm_resource_std.config_id = rm_arrange.config_id and rm_resource_std.rm_arrange_type_id = rm_arrange.rm_arrange_type_id and rm_resource_std.resource_std = ${parameters['"
                                + parameterName + "']}) ";
                
                dataSource.addRestriction(Restrictions.sql(restriction));
            }
        }
    }
    
    /**
     * Adds the time restriction.
     * 
     * @param startDate the start date
     * @param startTime the start time
     * @param endTime the end time
     * @param reserveId the reserve id
     * @param dataSource the ds
     * 
     *            <p>
     *            Suppress PMD warning "AvoidUsingSql" in this method.
     *            <p>
     *            Justification: Case #1: Statement with SELECT WHERE EXISTS ... pattern.
     */
    @SuppressWarnings("PMD.AvoidUsingSql")
    public static void addTimeRestriction(final Date startDate, final Time startTime,
            final Time endTime, final Integer reserveId, final DataSource dataSource) {
        // if (startDate == null || startTime == null || endTime == null) return;
        
        if (startDate != null && startTime != null && endTime != null) {
            
            String editRestriction = "";
            if (reserveId != null) {
                dataSource.addParameter("reserveId", reserveId, DataSource.DATA_TYPE_INTEGER);
                editRestriction = " reserve_rm.res_id <> ${parameters['reserveId']} and ";
            }
            
            dataSource.addParameter("startDate", startDate, DataSource.DATA_TYPE_DATE);
            
            String reservationRestriction =
                    " NOT EXISTS (select res_id from reserve_rm left outer join rm_arrange ra "
                            + " on reserve_rm.bl_id = ra.bl_id and reserve_rm.fl_id = ra.fl_id and reserve_rm.rm_id = ra.rm_id and reserve_rm.config_id = ra.config_id and reserve_rm.rm_arrange_type_id = ra.rm_arrange_type_id "
                            + " , rm_config rc "
                            + "  where "
                            + editRestriction
                            + " reserve_rm.bl_id = rm_arrange.bl_id "
                            + " and reserve_rm.fl_id = rm_arrange.fl_id and reserve_rm.rm_id = rm_arrange.rm_id "
                            + " and rc.bl_id=rm_arrange.bl_id AND rc.fl_id=rm_arrange.fl_id AND rc.rm_id=rm_arrange.rm_id "
                            + " and rc.config_id = reserve_rm.config_id "
                            + " and (rc.config_id = rm_arrange.config_id OR rc.excluded_config like '%'''${sql.concat}rm_arrange.config_id${sql.concat}'''%' ) "
                            // don't check on arrange type
                            + " and reserve_rm.date_start = ${parameters['startDate']} "
                            + " and (reserve_rm.status = 'Awaiting App.' or reserve_rm.status = 'Confirmed') ";
            
            dataSource.addParameter("startTime", startTime, DataSource.DATA_TYPE_TIME);
            dataSource.addParameter("endTime", endTime, DataSource.DATA_TYPE_TIME);
            
            // check if the reservation overlaps other reservations.
            // Check that no other room reservation exists with other.endTime + preblock + postblock
            // > new.startTime and other.startTime - preblock - postblock < new.endTime
            if (dataSource.isOracle()) {
                reservationRestriction +=
                        " and ( reserve_rm.time_start - (ra.pre_block + rm_arrange.post_block) / (24*60) < ${parameters['endTime']} ) "
                                + " and ( reserve_rm.time_end + (rm_arrange.pre_block + ra.post_block) / (24*60) > ${parameters['startTime']} ) ";
                
            } else if (dataSource.isSqlServer()) {
                reservationRestriction +=
                        " and ( DATEADD(mi, -ra.pre_block - rm_arrange.post_block, reserve_rm.time_start) < ${parameters['endTime']}) "
                                + " and ( DATEADD(mi, rm_arrange.pre_block + ra.post_block, reserve_rm.time_end) > ${parameters['startTime']}) ";
                
            } else {
                reservationRestriction +=
                        " and ( Convert(char(10), DATEADD(mi, -ra.pre_block - rm_arrange.post_block, reserve_rm.time_start), 108) < Convert(char(10), ${parameters['endTime']}, 108) ) "
                                + " and ( Convert(char(10), DATEADD(mi, rm_arrange.pre_block + ra.post_block, reserve_rm.time_end), 108) > Convert(char(10), ${parameters['startTime']}, 108) ) ";
            }
            
            // end EXISTS
            reservationRestriction += Constants.RIGHT_PAR;
            
            dataSource.addRestriction(Restrictions.sql(reservationRestriction));
            
        }
    }
    
}
