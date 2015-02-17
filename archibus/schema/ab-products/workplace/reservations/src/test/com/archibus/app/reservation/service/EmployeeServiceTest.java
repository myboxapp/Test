package com.archibus.app.reservation.service;

import junit.framework.Assert;

import com.archibus.app.common.organization.domain.Employee;
import com.archibus.app.reservation.domain.*;

/**
 * The Class EmployeeServiceTest.
 */
public class EmployeeServiceTest extends ReservationServiceTestBase {
    
    /**
     * Test find employee.
     */
    public final void testFindEmployee() {
        final Employee employee = this.employeeService.findEmployee(AFM_EMAIL);
        Assert.assertNotNull(employee);
    }
    
    /**
     * Test find employee location.
     * 
     * @throws ReservationException the reservation exception
     */
    public final void testFindEmployeeLocation() throws ReservationException {
        final UserLocation userLocation = this.employeeService.getUserLocation();
        
        Assert.assertNotNull(userLocation);
        Assert.assertNotNull(userLocation.getCountryId());
        Assert.assertNotNull(userLocation.getStateId());
        Assert.assertNotNull(userLocation.getCityId());
        Assert.assertNotNull(userLocation.getSiteId());
        Assert.assertNotNull(userLocation.getBuildingId());
        Assert.assertNotNull(userLocation.getFloorId());
        Assert.assertNotNull(userLocation.getRoomId());
    }
    
    /**
     * test employee.
     */
    public final void testIsEmployee() {
        Assert.assertTrue(this.employeeService.isEmployeeEmail(AFM_EMAIL));
    }
    
}
