package com.archibus.app.reservation.service;

import junit.framework.*;

/**
 * All tests in the service package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other service tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor for service suite.
     * 
     * @param name name
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get the test suite for service package.
     * 
     * @return suite suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(ApproveReservationServiceTest.class);
        suite.addTestSuite(EmployeeServiceTest.class);
        suite.addTestSuite(SpaceServiceTest.class);
        suite.addTestSuite(ReservationServiceTest.class);
        suite.addTestSuite(ReservationRemoteTest.class);
        suite.addTestSuite(TimelineServiceTest.class);
        suite.addTestSuite(ResourceTimelineServiceTest.class);
        suite.addTestSuite(ResourceReservationServiceTest.class);
        suite.addTestSuite(ResourceFinderServiceTest.class);
        suite.addTestSuite(ReservationUpgradeServiceTest.class);
        return suite;
    }
}
