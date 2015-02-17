package com.archibus.app.reservation.service;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.data.DataSet;
import com.archibus.datasource.restriction.Restrictions;

/**
 * The Class ResourceReservationServiceTest.
 */
public class ResourceFinderServiceTest extends RoomReservationServiceTestBase {
    
    /** Building ID of the HQ building. */
    private static final String BL_ID_HQ = "HQ";
    
    /** Reservation id in the reserve table. */
    private static final String RESERVE_RES_ID = "reserve.res_id";
    
    /** The resource reservation service. */
    private ResourceFinderService resourceFinderService;
    
    /**
     * Test find available reservable resources.
     */
    public void testFindAvailableReservableResources() {
        
        final Map<String, String> locationFilter = new HashMap<String, String>();
        locationFilter.put(Constants.BL_ID_FIELD_NAME, BL_ID_HQ);
        
        final DataRecord reservation = this.createAndSaveRoomReservation(true);
        
        this.roomAllocationDataSource.clearRestrictions();
        this.roomAllocationDataSource.addRestriction(Restrictions.eq(
            Constants.RESERVE_RM_TABLE_NAME, Constants.RES_ID, reservation.getInt(RESERVE_RES_ID)));
        final DataRecord roomAllocation = this.roomAllocationDataSource.getRecord();
        
        final DataSet availableResources =
                resourceFinderService.findAvailableReservableResourcesForRoom(locationFilter,
                    reservation, roomAllocation);
        
        Assert.assertNotNull(availableResources);
        
    }
    
    /**
     * Test find available catering resources.
     */
    public void testFindAvailableCateringResources() {
        
        final Map<String, String> locationFilter = new HashMap<String, String>();
        locationFilter.put("bl_id", BL_ID_HQ);
        
        final DataRecord reservation = this.createAndSaveRoomReservation(true);
        
        this.roomAllocationDataSource.clearRestrictions();
        this.roomAllocationDataSource.addRestriction(Restrictions.eq(
            Constants.RESERVE_RM_TABLE_NAME, Constants.RES_ID, reservation.getInt(RESERVE_RES_ID)));
        final DataRecord roomAllocation = this.roomAllocationDataSource.getRecord();
        
        final DataSet availableResources =
                resourceFinderService.findAvailableCateringResourcesForRoom(locationFilter,
                    reservation, roomAllocation);
        
        Assert.assertNotNull(availableResources);
        
    }
    
    /**
     * Set the resource finder service.
     * 
     * @param resourceFinderService the resource finder service
     */
    public void setResourceFinderService(final ResourceFinderService resourceFinderService) {
        this.resourceFinderService = resourceFinderService;
    }
    
}
