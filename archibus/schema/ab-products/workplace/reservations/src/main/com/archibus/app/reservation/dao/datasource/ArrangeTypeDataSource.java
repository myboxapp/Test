package com.archibus.app.reservation.dao.datasource;

import java.util.*;

import com.archibus.app.reservation.domain.ArrangeType;
import com.archibus.app.reservation.util.DataSourceUtils;
import com.archibus.datasource.ObjectDataSourceImpl;

/**
 * DataSource for room arrange types.
 * 
 * @author Bart Vanderschoot
 * 
 */
public class ArrangeTypeDataSource extends ObjectDataSourceImpl<ArrangeType> {
    
    /**
     * Default constructor.
     */
    public ArrangeTypeDataSource() {
        this("arrangeType", "rm_arrange_type");
    }
    
    /**
     * Constructor.
     * 
     * @param beanName Spring bean name
     * @param tableName table name
     */
    protected ArrangeTypeDataSource(final String beanName, final String tableName) {
        super(beanName, tableName);
    }
    
    /**
     * Create fields to properties mapping. To be compatible with version 19.
     * 
     * @return mapping
     */
    @Override
    protected Map<String, String> createFieldToPropertyMapping() {
        
        final Map<String, String> mapping = new HashMap<String, String>();
        mapping.put(this.tableName + ".rm_arrange_type_id", "arrangeTypeId");
        mapping.put(this.tableName + ".arrange_name", "arrangeName");
        mapping.put(this.tableName + ".vn_id", "vendor");
        mapping.put(this.tableName + ".tr_id", "trade");
        
        return mapping;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String[][] getFieldsToProperties() {
        return DataSourceUtils.getFieldsToProperties(createFieldToPropertyMapping());
    }
    
}
