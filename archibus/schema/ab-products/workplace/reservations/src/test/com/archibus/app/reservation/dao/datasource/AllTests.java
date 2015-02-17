package com.archibus.app.reservation.dao.datasource;

import junit.framework.*;

/**
 * All tests in the data source package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor specifying a name for the test.
     * 
     * @param name the name
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get the test suite for this package.
     * 
     * @return suite the suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(ArrangeTypeDataSourceTest.class);
        suite.addTestSuite(ResourceDataSourceTest.class);
        suite.addTestSuite(ResourceAllocationDataSourceTest.class);
        suite.addTestSuite(RoomArrangementDataSourceTest.class);
        suite.addTestSuite(RoomAllocationDataSourceTest.class);
        suite.addTestSuite(RoomReservationDataSourceTest.class);
        suite.addTestSuite(VisitorDataSourceTest.class);
        return suite;
    }
}
