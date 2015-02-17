package com.archibus.app.reservation.dao.datasource;

import java.util.List;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.ArrangeType;
import com.archibus.datasource.DataSourceTestBase;
import com.archibus.datasource.data.DataRecord;

/**
 * Test for ArrangeTypeDataSource.
 */
public class ArrangeTypeDataSourceTest extends DataSourceTestBase {
    
    /** test. */
    private static final String TEST = "TEST";
    
    /**
     * The data source under test.
     */
    private ArrangeTypeDataSource arrangeTypeDataSource;
    
    /**
     * Test getting all arrange types.
     */
    public final void testGetAll() {
        final List<DataRecord> records = this.arrangeTypeDataSource.getRecords();
        final List<ArrangeType> objects = this.arrangeTypeDataSource.find(null);
        
        Assert.assertEquals(records.size(), objects.size());
    }
    
    /**
     * test save.
     */
    public final void testSave() {
        ArrangeType arrangeType = new ArrangeType(TEST, "test description");
        this.arrangeTypeDataSource.save(arrangeType);
        
        arrangeType = this.arrangeTypeDataSource.get(TEST);
        
        Assert.assertNotNull(arrangeType);
    }
    
    /**
     * Sets the arrange type data source.
     * 
     * @param arrangeTypeDataSource the new arrange type data source
     */
    public final void setArrangeTypeDataSource(final ArrangeTypeDataSource arrangeTypeDataSource) {
        this.arrangeTypeDataSource = arrangeTypeDataSource;
    }
    
    // Disable StrictDuplicate CHECKSTYLE warning. Justification: setup test
    /**
     * {@inheritDoc}
     */
    @Override
    protected final String[] getConfigLocations() {
        return new String[] { "context\\core\\core-infrastructure.xml", "appContext-test.xml",
                "reservation-datasources.xml" };
    }
    
}
