package com.archibus.app.reservation.dao.datasource;

import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.beans.*;

import com.archibus.context.ContextStore;
import com.archibus.datasource.ObjectDataSourceImpl;
import com.archibus.datasource.data.*;
import com.archibus.datasource.restriction.Restrictions.Restriction;
import com.archibus.eventhandler.EventHandlerBase.DbField;
import com.archibus.model.view.datasource.ParsedRestrictionDef;

/**
 * The Class ObjectDataSourceBase.
 * 
 * @param <T> the generic type
 */
public abstract class AbstractObjectDataSourceBase<T> extends ObjectDataSourceImpl<T> {
    
    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(AbstractObjectDataSourceBase.class);
    
    /**
     * Instantiates a new object data source base.
     * 
     * @param beanName the bean name
     * @param tableName the table name
     */
    protected AbstractObjectDataSourceBase(final String beanName, final String tableName) {
        super(beanName, tableName);
        super.addTable(this.tableName);
        
        final Map<String, String> mapping = createFieldToPropertyMapping();
        // strip key table name !!!
        for (final String key : mapping.keySet()) {
            if (key.contains(".")) {
                final String[] keys = key.split("\\.");
                super.addField(keys[0], keys[1]);
            } else {
                super.addField(key);
            }
        }
    }
    
    /**
     * Convert object for new record.
     * 
     * @param object the object
     * @return the data record
     */
    public DataRecord convertObjectForNewRecord(final T object) {
        final DataRecord record = this.createNewRecord();
        
        final Map<String, String> mapping = createFieldToPropertyMapping();
        
        final BeanWrapper beanWrapper = new BeanWrapperImpl(object);
        /*
         * for (Iterator<String> it = mapping.keySet().iterator(); it.hasNext(); ) { String key =
         * it.next(); Object value = beanWrapper.getPropertyValue(mapping.get(key));
         * record.setValue(key, value); }
         */
        
        for (final DataValue field : record.getFields()) {
            final String key = field.getName();
            if (mapping.containsKey(key)) {
                final Object value = beanWrapper.getPropertyValue(mapping.get(key));
                record.setValue(key, value);
            }
        }
        
        return record;
    }
    
    /**
     * Convert object to record.
     * 
     * @param object the object
     * @return the data record
     */
    public DataRecord convertObjectToRecord(final T object) {
        final DataRecord record = this.createRecord();
        final Map<String, String> mapping = createFieldToPropertyMapping();
        
        final BeanWrapper beanWrapper = new BeanWrapperImpl(object);
        /*
         * for (Iterator<String> it = mapping.keySet().iterator(); it.hasNext(); ) { String key =
         * it.next(); Object value = beanWrapper.getPropertyValue(mapping.get(key));
         * record.setValue(key, value); }
         */
        
        for (final DbField field : this.getDbFieldsForVisibleFields()) {
            final String key = field.name;
            if (mapping.containsKey(key)) {
                final Object value = beanWrapper.getPropertyValue(mapping.get(key));
                record.setValue(key, value);
            }
        }
        
        return record;
    }
    
    /**
     * * Convert.
     * 
     * @param records the records
     * @return the list
     */
    
    @Override
    public List<T> convertRecordsToObjects(final List<DataRecord> records) {
        final Map<String, String> mapping = createFieldToPropertyMapping();
        final List<T> result = new ArrayList<T>();
        
        for (final DataRecord record : records) {
            // prototype
            final Object bean = ContextStore.get().getBean(this.beanName);
            result.add(getWrappedInstance(record, mapping, bean));
        }
        
        return result;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.archibus.datasource.ObjectDataSourceImpl#convertRecordToObject(com.archibus.datasource
     * .data.DataRecord)
     */
    @Override
    public T convertRecordToObject(final DataRecord record) {
        final Map<String, String> mapping = createFieldToPropertyMapping();
        final Object bean = ContextStore.get().getBean(this.beanName);
        
        return getWrappedInstance(record, mapping, bean);
    }
    
    /**
     * Find.
     * 
     * @return the list
     */
    public List<T> find() {
        final List<DataRecord> records = this.getRecords();
        return convertRecordsToObjects(records);
    }
    
    /**
     * Find.
     * 
     * @param restrictionDef the restriction def
     * @return the list
     */
    public List<T> find(final ParsedRestrictionDef restrictionDef) {
        final List<DataRecord> records = this.getRecords(restrictionDef);
        return convertRecordsToObjects(records);
    }
    
    /**
     * Find.
     * 
     * @param restriction the restriction
     * @return the list
     */
    public List<T> find(final Restriction restriction) {
        this.createCopy();
        this.addRestriction(restriction);
        
        final List<DataRecord> records = this.getRecords();
        return convertRecordsToObjects(records);
    }
    
    /**
     * Find.
     * 
     * @param restrictions the restrictions
     * @return the list
     */
    public List<T> find(final Restriction[] restrictions) {
        this.clearRestrictions();
        for (final Restriction restriction : restrictions) {
            this.addRestriction(restriction);
        }
        final List<DataRecord> records = this.getRecords();
        return convertRecordsToObjects(records);
    }
    
    /**
     * Find all.
     * 
     * @return the list
     */
    public List<T> findAll() {
        final List<DataRecord> records = this.getAllRecords();
        return convertRecordsToObjects(records);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.archibus.datasource.ObjectDataSourceImpl#getFieldsToProperties()
     */
    @Override
    protected String[][] getFieldsToProperties() {
        final Map<String, String> mapping = createFieldToPropertyMapping();
        
        final String[][] fieldsToProperties = new String[mapping.size()][2];
        int index = 0;
        for (final String key : mapping.keySet()) {
            fieldsToProperties[index++] = new String[] { key, mapping.get(key) };
        }
        
        return fieldsToProperties;
    }
    
    /**
     * Gets the wrapped instance.
     * 
     * @param record the record
     * @param mapping the mapping
     * @param bean the bean
     * @return the wrapped instance
     */
    protected T getWrappedInstance(final DataRecord record, final Map<String, String> mapping,
            final Object bean) {
        String key = null;
        T result = null;
        try {
            final BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
            for (final Iterator<String> it = mapping.keySet().iterator(); it.hasNext();) {
                key = it.next();
                beanWrapper.setPropertyValue(mapping.get(key), record.getValue(key));
            }
            result = (T) beanWrapper.getWrappedInstance();
        } catch (final BeansException e) {
            this.logger.error("key " + key, e);
        }
        
        return result;
        
    }
}
