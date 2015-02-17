package com.archibus.app.reservation.domain;

import junit.framework.*;

/**
 * All tests in the domain package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other domain tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor for domain objects.
     * 
     * @param name to test
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get the test suite for domain objects.
     * 
     * @return the suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(ReservationExceptionTest.class);
        suite.addTestSuite(TimePeriodTest.class);
        return suite;
    }
}
