package com.archibus.app.reservation;

import junit.framework.*;

/**
 * All tests in this package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor for the reservation application test suite, specifying a name for the test.
     * 
     * @param name the name
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get the test suite for this package.
     * 
     * @return suite suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        // non-datasource tests first
        suite.addTest(com.archibus.app.reservation.service.actions.AllTests.suite());

        suite.addTest(com.archibus.app.reservation.util.AllTests.suite());
        suite.addTest(com.archibus.app.reservation.domain.AllTests.suite());
        suite.addTest(com.archibus.app.reservation.domain.recurrence.AllTests.suite());
        suite.addTest(com.archibus.app.reservation.dao.datasource.AllTests.suite());
        suite.addTest(com.archibus.app.reservation.service.AllTests.suite());
        suite.addTest(com.archibus.app.reservation.exchange.util.AllTests.suite());
        
        // this one last because it takes the most time
        suite.addTest(com.archibus.app.reservation.exchange.service.AllTests.suite());
        return suite;
    }
}
