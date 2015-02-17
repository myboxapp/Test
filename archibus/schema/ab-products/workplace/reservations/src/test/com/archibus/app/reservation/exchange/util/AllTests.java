package com.archibus.app.reservation.exchange.util;

import junit.framework.*;

/**
 * All tests in the exchange util package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other actions tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor specifying a name for the exchange util test.
     * 
     * @param name the name
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get test suite for the exchange util package.
     * 
     * @return suite suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(TimeZoneMapperTest.class);
        suite.addTestSuite(AttendeesHelperTest.class);
        return suite;
    }
}
