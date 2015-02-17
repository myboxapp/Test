package com.archibus.app.reservation.util;

import junit.framework.*;

/**
 * All tests in the util package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor specifying a test name.
     * 
     * @param name test name
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get the test suite for the util package.
     * 
     * @return suite the suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(DataSourceUtilsTest.class);
        suite.addTestSuite(ReservationsContextHelperTest.class);
        suite.addTestSuite(TimeZoneConverterTest.class);
        return suite;
    }
}
