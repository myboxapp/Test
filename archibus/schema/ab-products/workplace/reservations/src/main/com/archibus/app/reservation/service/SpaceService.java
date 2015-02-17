package com.archibus.app.reservation.service;

import java.util.*;

import com.archibus.app.common.space.dao.datasource.*;
import com.archibus.app.common.space.domain.*;
import com.archibus.app.reservation.dao.datasource.ArrangeTypeDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.utility.*;

/**
 * 
 * Implementation of space service.
 * 
 * @author Bart Vanderschoot
 * @since 20.1
 * 
 */
public class SpaceService implements ISpaceService {
    
    /**
     * Last part of the exists restriction that retrieves only location entities that contain
     * reservable rooms.
     */
    private static final String EXISTS_IN_BL_RM_ARRANGE_PART2 = " and rm_arrange.reservable = 1 ) ";
    
    /**
     * First part of the exists restriction to retrieve only location entities that contain
     * reservable rooms.
     * <p>
     * Suppress warning PMD.AvoidUsingSql.
     * <p>
     * Justification: Case #1: Statement with SELECT WHERE EXISTS ... pattern.
     */
    @SuppressWarnings("PMD.AvoidUsingSql")
    private static final String EXISTS_IN_BL_RM_ARRANGE_PART1 =
            " EXISTS (SELECT 1 FROM bl, rm_arrange WHERE rm_arrange.bl_id = bl.bl_id ";
    
    /** Floor id field name. */
    private static final String FLOOR_ID = "fl_id";
    
    /** Building id field name. */
    private static final String BUILDING_ID = "bl_id";
    
    /** Site id field name. */
    private static final String SITE_ID = "site_id";
    
    /** City id field name. */
    private static final String CITY_ID = "city_id";
    
    /** State id field name. */
    private static final String STATE_ID = "state_id";
    
    /** Name field name. */
    private static final String NAME = "name";
    
    /** Country id field name. */
    private static final String COUNTRY_ID = "ctry_id";
    
    /** The site data source. */
    private SiteDataSource siteDataSource;
    
    /** The building data source. */
    private BuildingDataSource buildingDataSource;
    
    /** The floor data source. */
    private FloorDataSource floorDataSource;
    
    /** The room data source. */
    private RoomDataSource roomDataSource;
    
    /** The arrange type data source. */
    private ArrangeTypeDataSource arrangeTypeDataSource;
    
    /**
     * {@inheritDoc}
     */
    public final List<Country> getCountries(final Country filter) {
        final DataSource dataSource =
                DataSourceFactory.createDataSourceForFields("ctry",
                    new String[] { COUNTRY_ID, NAME });
        
        if (StringUtil.notNullOrEmpty(filter.getCountryId())) {
            dataSource.addRestriction(Restrictions.eq(dataSource.getMainTableName(), COUNTRY_ID,
                filter.getCountryId()));
        }
        
        dataSource.addRestriction(Restrictions.sql(EXISTS_IN_BL_RM_ARRANGE_PART1
                + " and bl.ctry_id = ctry.ctry_id " + EXISTS_IN_BL_RM_ARRANGE_PART2));
        
        final List<DataRecord> records = dataSource.getRecords();
        final List<Country> countries = new ArrayList<Country>(records.size());
        for (final DataRecord record : records) {
            final Country country = new Country();
            country.setCountryId(record.getString("ctry.ctry_id"));
            country.setName(record.getString("ctry.name"));
            countries.add(country);
        }
        return countries;
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<State> getStates(final State filter) {
        final DataSource dataSource =
                DataSourceFactory.createDataSourceForFields("state", new String[] { COUNTRY_ID,
                        STATE_ID, NAME });
        
        if (StringUtil.notNullOrEmpty(filter.getCountryId())) {
            dataSource.addRestriction(Restrictions.eq(dataSource.getMainTableName(), COUNTRY_ID,
                filter.getCountryId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getStateId())) {
            dataSource.addRestriction(Restrictions.eq(dataSource.getMainTableName(), STATE_ID,
                filter.getStateId()));
        }
        
        dataSource.addRestriction(Restrictions.sql(EXISTS_IN_BL_RM_ARRANGE_PART1
                + " and bl.state_id = state.state_id " + EXISTS_IN_BL_RM_ARRANGE_PART2));
        
        final List<DataRecord> records = dataSource.getRecords();
        final List<State> states = new ArrayList<State>(records.size());
        for (final DataRecord record : records) {
            final State state = new State();
            state.setCountryId(record.getString("state.ctry_id"));
            state.setStateId(record.getString("state.state_id"));
            state.setName(record.getString("state.name"));
            states.add(state);
        }
        return states;
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<City> getCities(final City filter) {
        final DataSource dataSource =
                DataSourceFactory.createDataSourceForFields("city", new String[] { COUNTRY_ID,
                        STATE_ID, CITY_ID, NAME });
        
        if (StringUtil.notNullOrEmpty(filter.getCountryId())) {
            dataSource.addRestriction(Restrictions.eq(dataSource.getMainTableName(), COUNTRY_ID,
                filter.getCountryId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getStateId())) {
            dataSource.addRestriction(Restrictions.eq(dataSource.getMainTableName(), STATE_ID,
                filter.getStateId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getCityId())) {
            dataSource.addRestriction(Restrictions.eq(dataSource.getMainTableName(), CITY_ID,
                filter.getCityId()));
        }
        
        dataSource.addRestriction(Restrictions.sql(EXISTS_IN_BL_RM_ARRANGE_PART1
                + " and bl.city_id = city.city_id and bl.state_id = city.state_id "
                + EXISTS_IN_BL_RM_ARRANGE_PART2));
        
        final List<DataRecord> records = dataSource.getRecords();
        final List<City> cities = new ArrayList<City>(records.size());
        for (final DataRecord record : records) {
            final City city = new City();
            city.setCountryId(record.getString("city.ctry_id"));
            city.setStateId(record.getString("city.state_id"));
            city.setCityId(record.getString("city.city_id"));
            city.setName(record.getString("city.name"));
            cities.add(city);
        }
        return cities;
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<Site> getSites(final Site filter) {
        this.siteDataSource.clearRestrictions();
        
        if (StringUtil.notNullOrEmpty(filter.getCtryId())) {
            this.siteDataSource.addRestriction(Restrictions.eq(
                this.siteDataSource.getMainTableName(), COUNTRY_ID, filter.getCtryId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getStateId())) {
            this.siteDataSource.addRestriction(Restrictions.eq(
                this.siteDataSource.getMainTableName(), STATE_ID, filter.getStateId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getCityId())) {
            this.siteDataSource.addRestriction(Restrictions.eq(
                this.siteDataSource.getMainTableName(), CITY_ID, filter.getCityId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getSiteId())) {
            this.siteDataSource.addRestriction(Restrictions.eq(
                this.siteDataSource.getMainTableName(), SITE_ID, filter.getSiteId()));
        }
        
        this.siteDataSource.addRestriction(Restrictions.sql(EXISTS_IN_BL_RM_ARRANGE_PART1
                + " and bl.site_id = site.site_id " + EXISTS_IN_BL_RM_ARRANGE_PART2));
        
        return this.siteDataSource.find(null);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Suppress warning PMD.AvoidUsingSql.
     * <p>
     * Justification: Case #1: Statement with SELECT WHERE EXISTS ... pattern.
     */
    @SuppressWarnings({ "PMD.AvoidUsingSql" })
    public final List<Building> getBuildings(final Building filter) {
        this.buildingDataSource.clearRestrictions();
        
        if (StringUtil.notNullOrEmpty(filter.getCtryId())) {
            this.buildingDataSource.addRestriction(Restrictions.eq(
                this.buildingDataSource.getMainTableName(), COUNTRY_ID, filter.getCtryId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getStateId())) {
            this.buildingDataSource.addRestriction(Restrictions.eq(
                this.buildingDataSource.getMainTableName(), STATE_ID, filter.getStateId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getCityId())) {
            this.buildingDataSource.addRestriction(Restrictions.eq(
                this.buildingDataSource.getMainTableName(), CITY_ID, filter.getCityId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getSiteId())) {
            this.buildingDataSource.addRestriction(Restrictions.eq(
                this.buildingDataSource.getMainTableName(), SITE_ID, filter.getSiteId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getBuildingId())) {
            this.buildingDataSource.addRestriction(Restrictions.eq(
                this.buildingDataSource.getMainTableName(), BUILDING_ID, filter.getBuildingId()));
        }
        
        this.buildingDataSource.addRestriction(Restrictions.sql(" EXISTS (select 1 from rm_arrange"
                + " where rm_arrange.bl_id = bl.bl_id and rm_arrange.reservable = 1) "));
        
        return this.buildingDataSource.find(null);
    }
    
    /**
     * {@inheritDoc}
     */
    public final Building getBuildingDetails(final String blId) {
        return this.buildingDataSource.get(blId);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Suppress warning PMD.AvoidUsingSql.
     * <p>
     * Justification: Case #1: Statement with SELECT WHERE EXISTS ... pattern.
     */
    @SuppressWarnings({ "PMD.AvoidUsingSql" })
    public final List<Floor> getFloors(final Floor filter) {
        this.floorDataSource.clearRestrictions();
        if (StringUtil.notNullOrEmpty(filter.getBuildingId())) {
            this.floorDataSource.addRestriction(Restrictions.eq(
                this.floorDataSource.getMainTableName(), BUILDING_ID, filter.getBuildingId()));
        }
        if (StringUtil.notNullOrEmpty(filter.getFloorId())) {
            this.floorDataSource.addRestriction(Restrictions.eq(
                this.floorDataSource.getMainTableName(), FLOOR_ID, filter.getFloorId()));
        }
        
        this.floorDataSource.addRestriction(Restrictions.sql(" EXISTS (select 1 from rm_arrange "
                + " where rm_arrange.bl_id = fl.bl_id and rm_arrange.fl_id = fl.fl_id "
                + " and rm_arrange.reservable = 1) "));
        
        return this.floorDataSource.find(null);
    }
    
    /**
     * {@inheritDoc}
     */
    public final Room getRoomDetails(final String blId, final String flId, final String rmId) {
        
        final Room room = new Room();
        room.setBuildingId(blId);
        room.setFloorId(flId);
        room.setId(rmId);
        
        return this.roomDataSource.getByPrimaryKey(room);
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<ArrangeType> getArrangeTypes() {
        return this.arrangeTypeDataSource.find(null);
    }
    
    /**
     * Sets the site data source.
     * 
     * @param siteDataSource the new site data source
     */
    public final void setSiteDataSource(final SiteDataSource siteDataSource) {
        this.siteDataSource = siteDataSource;
    }
    
    /**
     * Sets the building data source.
     * 
     * @param buildingDataSource the new building data source
     */
    public final void setBuildingDataSource(final BuildingDataSource buildingDataSource) {
        this.buildingDataSource = buildingDataSource;
    }
    
    /**
     * Sets the floor data source.
     * 
     * @param floorDataSource the new floor data source
     */
    public final void setFloorDataSource(final FloorDataSource floorDataSource) {
        this.floorDataSource = floorDataSource;
    }
    
    /**
     * Sets the room data source.
     * 
     * @param roomDataSource the new room data source
     */
    public final void setRoomDataSource(final RoomDataSource roomDataSource) {
        this.roomDataSource = roomDataSource;
    }
    
    /**
     * Setter for the arrangeTypeDataSource property.
     *
     * @param arrangeTypeDataSource the arrangeTypeDataSource to set
     * @see arrangeTypeDataSource
     */
    public final void setArrangeTypeDataSource(final ArrangeTypeDataSource arrangeTypeDataSource) {
        this.arrangeTypeDataSource = arrangeTypeDataSource;
    }
    
}
