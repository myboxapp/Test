package com.archibus.app.reservation.service; 

import java.util.List;

import com.archibus.app.reservation.domain.IReservation; 
import com.archibus.context.ContextStore;
import com.archibus.datasource.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.eventhandler.reservations.*;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.utility.Utility;


/**
 * The Class WorkRequestService.
 * 
 * This class will create and update work request required for room reservations.
 * 
 */
public class WorkRequestService { 

    /** The Constant TABLE_WR. */
    private static final String WR_TABLE = "wr";

    /** The Constant TIME_STAT_CHG. */
    private static final String TIME_STAT_CHG = "time_stat_chg";

    /** The Constant DATE_STAT_CHG. */
    private static final String DATE_STAT_CHG = "date_stat_chg";

    /** The Constant STATUS. */
    private static final String STATUS = "status";

    /** The Constant WR_ID. */
    private static final String WR_ID = "wr_id";

    /** The Constant WR_STATUS. */
    private static final String WR_STATUS = "wr.status";

    /** The Constant WR_TIME_STAT_CHG. */
    private static final String WR_TIME_STAT_CHG = "wr.time_stat_chg";

    /** The Constant WR_DATE_STAT_CHG. */
    private static final String WR_DATE_STAT_CHG = "wr.date_stat_chg";

    /** The Constant STRING_0. */
    private static final String STRING_0 = "0";

    /** The Constant RES_PARENT. */
    private static final String RES_PARENT = "res_parent";

    /** The Constant RES_ID. */
    private static final String RES_ID = "res_id";

    /**
     * Creates the work request.
     *
     * @param reservation the room reservation
     * @param editRecurring the edit recurring
     */
    public void createWorkRequest(final IReservation reservation, final boolean editRecurring) { 

        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();

        if (reservation.getParentId() != null && editRecurring) {
            context.addResponseParameter(RES_ID, STRING_0);
            context.addResponseParameter(RES_PARENT, reservation.getParentId().toString());
        } else {
            context.addResponseParameter(RES_ID, reservation.getReserveId().toString());
            context.addResponseParameter(RES_PARENT, STRING_0);
        } 

        final ReservationsRoomHandler reservationsRoomHandler = new ReservationsRoomHandler(); 
        reservationsRoomHandler.createWorkRequest(context);

        if (!reservation.getResourceAllocations().isEmpty()) {
            final ReservationsResourcesHandler resourceHandler =
                    new ReservationsResourcesHandler();

            if (reservation.getParentId() == null) {
                resourceHandler.createResourceWr(context, 
                        STRING_0, reservation.getReserveId().toString());
            } else {
                resourceHandler.createResourceWr(context, 
                        reservation.getParentId().toString(), STRING_0);
            }

        }

    }

    /**
     * Cancel work request.
     *
     * @param reservation the room reservation
     */
    public void cancelWorkRequest(final IReservation reservation) {

        if (reservation.getReserveId() == null) {
            return;
        }

        final String[] fields = {WR_ID, STATUS, DATE_STAT_CHG, TIME_STAT_CHG, RES_ID};

        final DataSource workRequestDataSource = DataSourceFactory.createDataSourceForFields(WR_TABLE, fields);
        workRequestDataSource.addRestriction(Restrictions.eq(WR_TABLE, RES_ID, reservation.getReserveId()));
        workRequestDataSource.addRestriction(Restrictions.in(WR_TABLE, STATUS, "R,Rev,A,AA"));

        List<DataRecord> records = workRequestDataSource.getRecords();

        for (DataRecord record : records) {
            record.setValue(WR_DATE_STAT_CHG, Utility.currentDate());
            record.setValue(WR_TIME_STAT_CHG, Utility.currentTime());
            record.setValue(WR_STATUS, "Can");

            workRequestDataSource.saveRecord(record);
        }

        workRequestDataSource.clearRestrictions();
        workRequestDataSource.addRestriction(Restrictions.eq(WR_TABLE, RES_ID, reservation.getReserveId()));
        workRequestDataSource.addRestriction(Restrictions.in(WR_TABLE, STATUS, "I,HP,HA,HL"));

        records = workRequestDataSource.getRecords();

        for (DataRecord record : records) {
            record.setValue(WR_DATE_STAT_CHG, Utility.currentDate());
            record.setValue(WR_TIME_STAT_CHG, Utility.currentTime());
            record.setValue(WR_STATUS, "S");

            workRequestDataSource.saveRecord(record);
        }
    } 

}
