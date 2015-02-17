package com.archibus.app.reservation.service;

import java.util.List;

import junit.framework.Assert;

import com.archibus.app.common.space.domain.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.utility.StringUtil;

/**
 * The Class SpaceServiceTest.
 */
public class SpaceServiceTest extends ReservationServiceTestBase {
    
    /** A dummy id that doesn't exist as a country id. */
    private static final String DUMMY_ID = "does not exist";
    
    /**
     * Test get countries.
     */
    public final void testGetCountries() {
        List<Country> countries = this.spaceService.getCountries(new Country());
        Assert.assertFalse(countries.isEmpty());
        
        final Country filter = new Country();
        filter.setCountryId(countries.get(countries.size() / 2).getCountryId());
        countries = this.spaceService.getCountries(filter);
        Assert.assertEquals(1, countries.size());
        Assert.assertEquals(filter.getCountryId(), countries.get(0).getCountryId());
        Assert.assertTrue(StringUtil.notNullOrEmpty(countries.get(0).getName()));
        
        filter.setCountryId(DUMMY_ID);
        countries = this.spaceService.getCountries(filter);
        Assert.assertTrue(countries.isEmpty());
    }
    
    /**
     * Test get states.
     */
    public final void testGetStates() {
        List<State> states = this.spaceService.getStates(new State());
        Assert.assertFalse(states.isEmpty());
        
        final State filter = new State();
        filter.setCountryId(states.get(states.size() / 2).getCountryId());
        states = this.spaceService.getStates(filter);
        Assert.assertFalse(states.isEmpty());
        for (final State state : states) {
            Assert.assertEquals(filter.getCountryId(), state.getCountryId());
            Assert.assertTrue(StringUtil.notNullOrEmpty(state.getName()));
            Assert.assertTrue(StringUtil.notNullOrEmpty(state.getStateId()));
        }
        
        filter.setStateId(states.get(states.size() / 2).getStateId());
        states = this.spaceService.getStates(filter);
        Assert.assertEquals(1, states.size());
        Assert.assertEquals(filter.getStateId(), states.get(0).getStateId());
        
        filter.setCountryId(DUMMY_ID);
        states = this.spaceService.getStates(filter);
        Assert.assertTrue(states.isEmpty());
    }
    
    /**
     * Test get cities.
     */
    public final void testGetCities() {
        List<City> cities = this.spaceService.getCities(new City());
        Assert.assertFalse(cities.isEmpty());
        
        final City filter = new City();
        filter.setCountryId(cities.get(cities.size() / 2).getCountryId());
        filter.setStateId(cities.get(cities.size() / 2).getStateId());
        cities = this.spaceService.getCities(filter);
        Assert.assertFalse(cities.isEmpty());
        for (final City city : cities) {
            Assert.assertEquals(filter.getCountryId(), city.getCountryId());
            Assert.assertEquals(filter.getStateId(), city.getStateId());
            Assert.assertTrue(StringUtil.notNullOrEmpty(city.getName()));
            Assert.assertTrue(StringUtil.notNullOrEmpty(city.getCityId()));
        }
        
        filter.setCityId(cities.get(cities.size() / 2).getCityId());
        cities = this.spaceService.getCities(filter);
        Assert.assertEquals(1, cities.size());
        Assert.assertEquals(filter.getCityId(), cities.get(0).getCityId());
        
        filter.setCountryId(DUMMY_ID);
        cities = this.spaceService.getCities(filter);
        Assert.assertTrue(cities.isEmpty());
    }
    
    /**
     * Test get sites.
     */
    public final void testGetSites() {
        final List<Site> sites = this.spaceService.getSites(new Site());
        
        Assert.assertNotNull(sites);
    }
    
    /**
     * Test get buildings.
     */
    public final void testGetBuildings() {
        final Building filter = new Building();
        filter.setSiteId(SITE_ID);
        final List<Building> buildings = this.spaceService.getBuildings(filter);
        
        Assert.assertNotNull(buildings);
    }
    
    /**
     * Test get building details.
     */
    public final void testGetBuildingDetails() {
        final Building building = this.spaceService.getBuildingDetails(BL_ID);
        
        Assert.assertNotNull(building);
        
        Assert.assertEquals(BL_ID, building.getBuildingId());
    }
    
    /**
     * Test get floors.
     */
    public final void testGetFloors() {
        final Floor filter = new Floor();
        filter.setBuildingId(BL_ID);
        final List<Floor> floors = this.spaceService.getFloors(filter);
        
        Assert.assertNotNull(floors);
        
        final Floor floor = floors.get(0);
        
        Assert.assertEquals(BL_ID, floor.getBuildingId());
    }
    
    /**
     * Test get room details.
     */
    public final void testGetRoomDetails() {
        final Room room = this.spaceService.getRoomDetails(BL_ID, FL_ID, RM_ID);
        
        Assert.assertNotNull(room);
        
        Assert.assertEquals(BL_ID, room.getBuildingId());
        Assert.assertEquals(FL_ID, room.getFloorId());
        Assert.assertEquals(RM_ID, room.getId());
    }
    
}
