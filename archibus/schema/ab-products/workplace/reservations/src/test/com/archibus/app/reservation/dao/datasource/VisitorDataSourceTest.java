package com.archibus.app.reservation.dao.datasource;

import java.util.List;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.Visitor;
import com.archibus.context.ContextStore;
import com.archibus.datasource.DataSourceTestBase;
import com.archibus.datasource.data.DataRecord;

/**
 * Test for VisitorDataSource.
 */
public class VisitorDataSourceTest extends DataSourceTestBase {
    
    /** The data source under test. */
    private VisitorDataSource visitorDataSource;
    
    /**
     * Test method for the primary functionality.
     */
    public void testVisitors() {
        Assert.assertNotNull(ContextStore.get().getDatabase());
        
        final List<DataRecord> allRecords = this.visitorDataSource.getRecords();
        
        Assert.assertNotNull(allRecords);
        
        final List<Visitor> allVisitors = this.visitorDataSource.findAll();
        
        Assert.assertNotNull(allVisitors);
        
        Assert.assertEquals(allRecords.size(), allVisitors.size());
    }
    
    /**
     * Test method for VisitorDataSource.get().
     */
    public void testGetVisitor() {
        final List<Visitor> allVisitors = this.visitorDataSource.findAll();
        if (!allVisitors.isEmpty()) {
            final Visitor visitor = this.visitorDataSource.get(allVisitors.get(0).getVisitorId());
            Assert.assertNotNull(visitor);
            Assert.assertEquals(allVisitors.get(0).getEmail(), visitor.getEmail());
        }
    }
    
    /**
     * Set the VisitorDataSource.
     * 
     * @param visitorDataSource the data source to set.
     */
    public void setVisitorDataSource(final VisitorDataSource visitorDataSource) {
        this.visitorDataSource = visitorDataSource;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.archibus.fixture.IntegrationTestBase#getConfigLocations()
     */
    @Override
    protected final String[] getConfigLocations() {
        return new String[] { "context\\core\\core-infrastructure.xml", "appContext-test.xml",
                "reservation-datasources.xml" };
    }
    
}
