package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.function.Supplier;

import io.vertx.core.json.JsonObject;

public class FieldSpecification
{
	private String fieldName;
	private Class fieldType;
	private Supplier<Object> defaultValueSupplier;
	boolean optional;
	public FieldSpecification(String fieldName, Class fieldType, Supplier<Object> defaultValueSupplier, boolean optional) {
		super();
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.defaultValueSupplier = defaultValueSupplier;
		this.optional = optional;
	}
	public String getFieldName() {
		return fieldName;
	}
	public Class getFieldType() {
		return fieldType;
	}
	public Object getDefaultValue() {
		return defaultValueSupplier.get();
	}		
	public boolean isOptional() {
		return optional;
	}
	
}