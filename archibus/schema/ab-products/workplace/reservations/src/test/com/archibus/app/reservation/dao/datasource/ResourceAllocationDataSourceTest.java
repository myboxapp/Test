package com.archibus.app.reservation.dao.datasource;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.model.view.datasource.ParsedRestrictionDef;

/**
 * Test class for ResourceAllocationDataSource.
 */
public class ResourceAllocationDataSourceTest extends ReservationDataSourceTestBase {
    
    /** ID of the resource to allocate. */
    private static final String RESOURCE_ID = "LCD-PROJECTOR1";
    
    /**
     * Test getting all resource allocations within a time period.
     */
    public void testGetResourceAllocationsByDate() {
        // first save a room allocation
        saveResourceAllocation();
        
        // then look for it
        Date startDate = this.existingReservation.getStartDate();
        
        List<ResourceAllocation> resourceAllocations =
                this.resourceAllocationDataSource.getResourceAllocations(startDate, BL_ID, FL_ID,
                    RM_ID);
        
        Assert.assertNotNull(resourceAllocations);
        Assert.assertFalse(resourceAllocations.isEmpty());
        
        final Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.MONTH, -1);
        startDate = cal.getTime();
        
        cal.add(Calendar.MONTH, 2);
        final Date endDate = cal.getTime();
        
        resourceAllocations =
                this.resourceAllocationDataSource.getResourceAllocations(startDate, endDate, BL_ID,
                    FL_ID, RM_ID);
        
        Assert.assertNotNull(resourceAllocations);
        Assert.assertFalse(resourceAllocations.isEmpty());
    }
    
    /**
     * Test getting resource allocations linked to a reservation.
     */
    public void testGetResourceAllocationsByReservation() {
        // first save an allocation
        saveResourceAllocation();
        
        // get all resource allocations
        final List<DataRecord> roomAllocations = this.resourceAllocationDataSource.getAllRecords();
        
        Assert.assertNotNull(roomAllocations);
        Assert.assertFalse(roomAllocations.isEmpty());
        
        List<ResourceAllocation> resources =
                this.resourceAllocationDataSource.find((ParsedRestrictionDef) null);
        Assert.assertNotNull(resources);
        Assert.assertFalse(resources.isEmpty());
        
        final RoomReservation reservation =
                new RoomReservation(this.existingReservation.getReserveId());
        resources = this.resourceAllocationDataSource.find(reservation);
        Assert.assertNotNull(resources);
        Assert.assertFalse(resources.isEmpty());
        ResourceAllocation alloc = resources.get(0);
        Assert.assertEquals(this.existingReservation.getReserveId(), alloc.getReserveId());
        
        alloc = this.resourceAllocationDataSource.get(alloc.getId());
        Assert.assertEquals(this.existingReservation.getReserveId(), alloc.getReserveId());
    }
    
    /**
     * Test saving and deleting modified resource allocation.
     */
    public void testSaveResourceAllocation() {
        ResourceAllocation resourceAllocation = saveResourceAllocation();
        
        resourceAllocation =
                this.resourceAllocationDataSource.find(this.existingReservation).get(0);
        Assert.assertEquals(ALLOCATION_COMMENTS, resourceAllocation.getComments());
        
        resourceAllocation.setComments(ALLOCATION_COMMENTS_CHANGED);
        this.resourceAllocationDataSource.update(resourceAllocation);
        
        resourceAllocation =
                this.resourceAllocationDataSource.find(this.existingReservation).get(0);
        Assert.assertEquals(ALLOCATION_COMMENTS_CHANGED, resourceAllocation.getComments());
        
        this.resourceAllocationDataSource.delete(resourceAllocation);
        
        assertTrue("After delete, no more resource allocation linked to the reservation.",
            this.resourceAllocationDataSource.find(this.existingReservation).isEmpty());
    }
    
    /**
     * test total cost calculation.
     */
    public void testResourceAllocationCost() {
        
        final Resource resource = this.resourceDataSource.get(RESOURCE_ID);
        
        final ResourceAllocation allocation = new ResourceAllocation(this.existingReservation);
        allocation.setResourceId(RESOURCE_ID);
        allocation.setQuantity(1);
        allocation.setComments(ALLOCATION_COMMENTS);
        allocation.setBlId(BL_ID);
        allocation.setFlId(FL_ID);
        allocation.setRmId(RM_ID);
        
        this.resourceAllocationDataSource.calculateCost(allocation);
        this.resourceAllocationDataSource.save(allocation);
        
        // standard cost by reservation
        Assert.assertEquals(resource.getCostPerUnit(), allocation.getCost());
    }
    
    /**
     * Save a room allocation linked to this.existingReservation.
     * 
     * @return the room allocation saved
     */
    private ResourceAllocation saveResourceAllocation() {
        final ResourceAllocation alloc = new ResourceAllocation(this.existingReservation);
        alloc.setResourceId(RESOURCE_ID);
        alloc.setQuantity(1);
        alloc.setComments(ALLOCATION_COMMENTS);
        alloc.setBlId(BL_ID);
        alloc.setFlId(FL_ID);
        alloc.setRmId(RM_ID);
        
        this.resourceAllocationDataSource.calculateCost(alloc);
        this.resourceAllocationDataSource.save(alloc);
        return alloc;
    }
    
}
