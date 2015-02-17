package com.archibus.app.reservation.service;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.dao.datasource.*;
import com.archibus.datasource.data.*;

/**
 * The Class ResourceReservationServiceTest.
 */
public class ResourceReservationServiceTest extends AbstractReservationServiceTestBase {   
    
    /** Reservation id in the reserve table. */
    private static final String RESERVE_RES_ID = "reserve.res_id";
    
    /** coffee resource HQ. */
    private static final String COFFEE_HQ = "COFFEE HQ";
    
    /** The resource reservation service. */
    private ResourceReservationService resourceReservationService;
    
    /** The resource reservation data source. */
    private ResourceReservationDataSource resourceReservationDataSource; 
    
    /** The resource allocation data source. */
    private ResourceAllocationDataSource resourceAllocationDataSource;  
    
    /**
     * Test save new resource reservation.
     */
    public void testSaveNewResourceReservation() {
        final DataRecord record = createResourceReservation(false); 

        Assert.assertTrue(record.getInt(RESERVE_RES_ID) > 0); 
    }  

    /**
     * Test save new recurrent resource reservation.
     */
    public void testSaveNewRecurrentResourceReservation() {
        final DataRecord record = createResourceReservation(true); 

        Assert.assertTrue(record.getInt(RESERVE_RES_ID) > 0); 
    } 
  
    /**
     * Sets the resource reservation service.
     * 
     * @param resourceReservationService the new resource reservation service
     */
    public void setResourceReservationService(
            final ResourceReservationService resourceReservationService) {
        this.resourceReservationService = resourceReservationService;
    }
    
    /**
     * Set the resource reservation data source for this test.
     *
     * @param resourceReservationDataSource the data source
     */
    public void setResourceReservationDataSource(
            final ResourceReservationDataSource resourceReservationDataSource) {
        this.resourceReservationDataSource = resourceReservationDataSource;
    }

    /**
     * Sets the resource allocation data source.
     *
     * @param resourceAllocationDataSource the new resource allocation data source
     */
    public void setResourceAllocationDataSource(
            final ResourceAllocationDataSource resourceAllocationDataSource) {
        this.resourceAllocationDataSource = resourceAllocationDataSource;
    }

    /**
     * Create a resource reservation.
     * 
     * @param recurrent true for a recurring reservation, false for a regular one
     * 
     * @return DataRecord containing the new reservation
     */
    private DataRecord createResourceReservation(final boolean recurrent) {
        final DataRecord reservation = this.resourceReservationDataSource.createNewRecord();
        createReservation(reservation, recurrent); 
        
        final List<DataRecord> caterings = new ArrayList<DataRecord>(); 
        caterings.add(createResource(COFFEE_HQ, FIVE)); 
        
        final List<DataRecord> resources = new ArrayList<DataRecord>();
        resources.add(createResource("CHAIRS HQ", FIVE));
        resources.add(createResource("IT-SUPPORT", 1));
        
        final DataSetList cateringList = new DataSetList(caterings);
        final DataSetList resourceList = new DataSetList(resources);
        
        return this.resourceReservationService.saveResourceReservation(reservation, 
                  resourceList, cateringList);
    }
    
    /**
     * Creates the resource.
     *
     * @param resourceId the resource id
     * @param quantity the quantity
     * @return the data record
     */
    private DataRecord createResource(final String resourceId, final int quantity) {
        final DataRecord resource = resourceAllocationDataSource.createNewRecord();
        resource.setValue("reserve_rs.resource_id", resourceId);
        resource.setValue("reserve_rs.quantity", quantity);
        resource.setValue("reserve_rs.comments", "test comments");
        resource.setValue("reserve_rs.bl_id", BL_ID);
        resource.setValue("reserve_rs.fl_id", FL_ID);
        resource.setValue("reserve_rs.rm_id", RM_ID);
        resource.setValue("reserve_rs.date_start", this.startDate); 
        resource.setValue("reserve_rs.time_start", this.startTime);
        resource.setValue("reserve_rs.time_end", this.endTime);        
        return resource;
    }
    
    
}
