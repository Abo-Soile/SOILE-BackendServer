package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.function.Supplier;
/**
 * Specification for a field
 * @author Thomas Pfau
 *
 */
public class FieldSpecification
{
	private String fieldName;
	private Class fieldType;
	private Supplier<Object> defaultValueSupplier;
	boolean optional;
	/**
	 * Default constructor
	 * @param fieldName name of the field
	 * @param fieldType the type (class) of the element i the field
	 * @param defaultValueSupplier a default Supplier of objects of the specified type
	 * @param optional whether the field is optional
	 */
	public FieldSpecification(String fieldName, Class fieldType, Supplier<Object> defaultValueSupplier, boolean optional) {
		super();
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.defaultValueSupplier = defaultValueSupplier;
		this.optional = optional;
	}
	/**
	 * Get the name of the field
	 * @return the name
	 */
	public String getFieldName() {
		return fieldName;
	}
	/**
	 * Get the type of the field
	 * @return the Class indicator
	 */
	public Class getFieldType() {
		
		return fieldType;
	}
	/**
	 * Get a defautl value
	 * @return an default Object for the value of this field
	 */
	public Object getDefaultValue() {
		return defaultValueSupplier.get();
	}		
	/**
	 * Get whether the field is optional
	 * @return the optional setting 
	 */
	public boolean isOptional() {
		return optional;
	}
	
}