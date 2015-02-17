package com.archibus.app.reservation.service;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.dao.datasource.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.DataSource;
import com.archibus.datasource.restriction.Restrictions;

/**
 * Test class for ReservationUpgradeService.
 * <p>
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class ReservationUpgradeServiceTest extends ReservationServiceTestBase {
    
    /** Close tag for old recurrence options xml string. */
    private static final String OPTIONS_CLOSETAG = "</options>";
    
    /** Monthly part of the old recurring rule format, when monthly recurrence is not selected. */
    private static final String MONTHLY_UNUSED =
            "<monthly 1st=\"false\" 2nd=\"false\" 3rd=\"false\" 4th=\"false\" last=\"false\" mon=\"false\" tue=\"false\" wed=\"false\" thu=\"false\" fri=\"false\" sat=\"false\" sun=\"false\" />";
    
    /** Weekly part of the old recurring rule format, when weekly recurrence is not selected. */
    private static final String WEEKLY_UNUSED =
            "<weekly mon=\"false\" tue=\"false\" wed=\"false\" thu=\"false\" fri=\"false\" sat=\"false\" sun=\"false\" />";
    
    /** Catering nature. */
    private static final String CATERING_NATURE = "Catering";
    
    /** Quantity used by the upgrade service for conversion from unlimited to limited resources. */
    private static final int MAX_QUANTITY = 999;
    
    /** Quantity used for defining a resource. */
    private static final int RESOURCE_QUANTITY = 50;
    
    /** Resources table name. */
    private static final String RESOURCES = "resources";
    
    /** Resource type field name. */
    private static final String RESOURCE_TYPE = "resource_type";
    
    /** Resource nature field name. */
    private static final String RESOURCE_NATURE = "resource_nature";
    
    /** Resource standard table name and field name. */
    private static final String RESOURCE_STD = "resource_std";
    
    /** Limited resource type. */
    private static final String LIMITED = "Limited";
    
    /** Unlimited resource type. */
    private static final String UNLIMITED = "Unlimited";
    
    /** Resource ID of network cables existing in the database. */
    private static final String CABLES_ID = "NETWORK CABLE HQ";
    
    /** Resource ID of sandwiches existing in the database. */
    private static final String SANDWICH_ID = "SANDW. TRAINING";
    
    /** Data source used for verifying resource conversion test results. */
    private ResourceDataSource resourceDataSource;
    
    /**
     * Test the conversion WFR.
     */
    public void testConvertResources() {
        this.resourceDataSource.addTable(RESOURCE_STD, DataSource.ROLE_STANDARD);
        this.resourceDataSource.addField(RESOURCE_STD, RESOURCE_NATURE);
        this.resourceDataSource.addField(RESOURCE_STD, RESOURCE_STD);
        
        // Create a non-catering unlimited resource.
        Resource resource = resourceDataSource.get(CABLES_ID);
        resource.setResourceType(UNLIMITED);
        resource.setQuantity(0);
        resourceDataSource.update(resource);
        
        // Create a limited catering resource.
        resource = resourceDataSource.get(SANDWICH_ID);
        resource.setResourceType(LIMITED);
        resource.setQuantity(RESOURCE_QUANTITY);
        resourceDataSource.update(resource);
        
        final int uniqueCount = getUniqueResourceCount();
        final int limitedCount = getLimitedResourceCount();
        
        // Check that invalid catering resource and other resource exist.
        Assert.assertFalse(allCateringResourcesValid());
        Assert.assertFalse(allNonCateringResourcesValid());
        
        final ReservationUpgradeService upgradeService = new ReservationUpgradeService();
        upgradeService.convertResources();
        
        Assert.assertTrue(allCateringResourcesValid());
        Assert.assertTrue(allNonCateringResourcesValid());
        Assert.assertEquals(uniqueCount, getUniqueResourceCount());
        Assert.assertEquals(limitedCount, getLimitedResourceCount());
    }
    
    /**
     * Test converting the recurring rule format from the old to the new.
     */
    public void testConvertRecurringRule() {
        // First insert three reservations.
        // Set their recurring rule to the old format and mark them as recurring.
        final RoomReservation dailyReservation = this.createRoomReservation();
        dailyReservation.setReservationType(Constants.TYPE_RECURRING);
        dailyReservation.setRecurringRule("<options type=\"day\"><ndays value=\"3\" />"
                + WEEKLY_UNUSED + MONTHLY_UNUSED + OPTIONS_CLOSETAG);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(dailyReservation.getStartDate());
        calendar.add(Calendar.DATE, 1);
        dailyReservation.setStartDate(calendar.getTime());
        dailyReservation.setEndDate(dailyReservation.getStartDate());
        this.reservationService.saveReservation(dailyReservation);
        dailyReservation.setParentId(dailyReservation.getReserveId());
        this.reservationService.saveReservation(dailyReservation);
        
        final RoomReservation weeklyReservation = this.createRoomReservation();
        weeklyReservation.setReservationType(Constants.TYPE_RECURRING);
        weeklyReservation
            .setRecurringRule("<options type=\"week\"><ndays value=\"\" />"
                    + "<weekly mon=\"false\" tue=\"true\" wed=\"false\" thu=\"true\" fri=\"false\" sat=\"false\" sun=\"false\" />"
                    + MONTHLY_UNUSED + OPTIONS_CLOSETAG);
        calendar.add(Calendar.DATE, 2);
        weeklyReservation.setStartDate(calendar.getTime());
        weeklyReservation.setEndDate(weeklyReservation.getStartDate());
        this.reservationService.saveReservation(weeklyReservation);
        weeklyReservation.setParentId(weeklyReservation.getReserveId());
        this.reservationService.saveReservation(weeklyReservation);
        
        final RoomReservation monthlyReservation = this.createRoomReservation();
        monthlyReservation.setReservationType(Constants.TYPE_RECURRING);
        monthlyReservation
            .setRecurringRule("<options type=\"month\"><ndays value=\"\" />"
                    + WEEKLY_UNUSED
                    + "<monthly 1st=\"false\" 2nd=\"false\" 3rd=\"true\" 4th=\"false\" last=\"false\" mon=\"false\" tue=\"false\" wed=\"true\" thu=\"false\" fri=\"false\" sat=\"false\" sun=\"false\" />"
                    + OPTIONS_CLOSETAG);
        calendar.add(Calendar.DATE, 1);
        monthlyReservation.setStartDate(calendar.getTime());
        monthlyReservation.setEndDate(monthlyReservation.getStartDate());
        this.reservationService.saveReservation(monthlyReservation);
        monthlyReservation.setParentId(monthlyReservation.getReserveId());
        this.reservationService.saveReservation(monthlyReservation);
        
        final ReservationUpgradeService upgradeService = new ReservationUpgradeService();
        upgradeService.convertRecurringRule();
        
        // Check that the conversion is successful.
        Assert
            .assertEquals(
                "<recurring type=\"day\" value1=\"3\" value2=\"\" value3=\"\" value4=\"\" total=\"\" />",
                this.reservationService.getActiveReservation(dailyReservation.getReserveId(), null)
                    .getRecurringRule());
        Assert
            .assertEquals(
                "<recurring type=\"week\" value1=\"0,1,0,1,0,0,0\" value2=\"1\" value3=\"\" value4=\"\" total=\"\" />",
                this.reservationService
                    .getActiveReservation(weeklyReservation.getReserveId(), null)
                    .getRecurringRule());
        Assert
            .assertEquals(
                "<recurring type=\"month\" value1=\"3rd\" value2=\"wed\" value3=\"1\" value4=\"\" total=\"\" />",
                this.reservationService.getActiveReservation(monthlyReservation.getReserveId(),
                    null).getRecurringRule());
    }
    
    /**
     * Set the resource data source used for verifying test results.
     * 
     * @param resourceDataSource the resource data source to test
     */
    public void setResourceDataSource(final ResourceDataSource resourceDataSource) {
        this.resourceDataSource = resourceDataSource;
    }
    
    /**
     * Verify whether all catering resources are defined Unlimited.
     * 
     * @return true if ok, false if not
     */
    private boolean allCateringResourcesValid() {
        resourceDataSource.clearRestrictions();
        resourceDataSource.addRestriction(Restrictions.ne(RESOURCES, RESOURCE_TYPE, UNLIMITED));
        resourceDataSource.addRestriction(Restrictions.eq(RESOURCE_STD, RESOURCE_NATURE,
            CATERING_NATURE));
        return resourceDataSource.getRecords().isEmpty();
    }
    
    /**
     * Verify that no resources other than catering are defined Unlimited.
     * 
     * @return true if ok, false if not
     */
    private boolean allNonCateringResourcesValid() {
        resourceDataSource.clearRestrictions();
        resourceDataSource.addRestriction(Restrictions.eq(RESOURCES, RESOURCE_TYPE, UNLIMITED));
        resourceDataSource.addRestriction(Restrictions.ne(RESOURCE_STD, RESOURCE_NATURE,
            CATERING_NATURE));
        return resourceDataSource.getRecords().isEmpty();
    }
    
    /**
     * Get the number of non-catering unique resources currently in the database.
     * 
     * @return number of non-catering unique resources
     */
    private int getUniqueResourceCount() {
        this.resourceDataSource.clearRestrictions();
        this.resourceDataSource.addRestriction(Restrictions.eq(RESOURCES, RESOURCE_TYPE, "Unique"));
        resourceDataSource.addRestriction(Restrictions.ne(RESOURCE_STD, RESOURCE_NATURE,
            CATERING_NATURE));
        return this.resourceDataSource.getRecords().size();
    }
    
    /**
     * Get the number of non-catering limited resources currently in the database with a quantity
     * less than 999.
     * 
     * @return number of non-catering limited resources with quantity < 999
     */
    private int getLimitedResourceCount() {
        this.resourceDataSource.clearRestrictions();
        this.resourceDataSource.addRestriction(Restrictions.eq(RESOURCES, RESOURCE_TYPE, LIMITED));
        this.resourceDataSource
            .addRestriction(Restrictions.lt(RESOURCES, "quantity", MAX_QUANTITY));
        resourceDataSource.addRestriction(Restrictions.ne(RESOURCE_STD, RESOURCE_NATURE,
            CATERING_NATURE));
        return this.resourceDataSource.getRecords().size();
    }
    
}
