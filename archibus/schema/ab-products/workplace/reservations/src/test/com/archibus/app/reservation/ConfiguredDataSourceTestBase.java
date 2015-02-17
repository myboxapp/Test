package com.archibus.app.reservation;

import com.archibus.datasource.DataSourceTestBase;

/**
 * Provides configuration file location for DataSource tests.
 *<p>
 * Used by Data Source Tests in the reservations package.
 *
 * @author Yorik Gerlo
 * @since 21.2
 * <p>
 *        Suppress warning "PMD.TestClassWithoutTestCases".
 *        <p>
 *        Justification: this is a base class for other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class ConfiguredDataSourceTestBase extends DataSourceTestBase {
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] { "context\\core\\core-infrastructure.xml", "appContext-test.xml",
                "reservation-service.xml" };
    }
    
}
