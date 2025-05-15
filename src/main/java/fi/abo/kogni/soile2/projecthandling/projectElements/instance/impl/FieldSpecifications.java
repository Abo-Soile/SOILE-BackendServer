package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashMap;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A Set of field specifications for an object
 * @author Thomas Pfau
 *
 */
public class FieldSpecifications {


	private HashMap<String,FieldSpecification> fieldSpecs = new HashMap<>();

	/**
	 * Default constructor
	 */
	public FieldSpecifications() { 
	}


	/**
	 * get the Fields listed in the Specs
	 * @return a Set of fields in this spec
	 */
	public Set<String> getFields()
	{
		return fieldSpecs.keySet();
	}

	/**
	 * Get the class for a specific field
	 * @param field the field to get the class for
	 * @return the class of the field elements
	 */
	public Class getClassForField(String field)
	{
		if(fieldSpecs.containsKey(field))
		{
			return fieldSpecs.get(field).getFieldType();	
		}
		else
		{
			return null;
		}
	}

	/**
	 * Whether a field is in the specs 
	 * @param field the field to check
	 * @return whether it is in (true) or not (false)
	 */
	public boolean hasField(String field)
	{
		return fieldSpecs.containsKey(field);		
	}

	/**
	 * Get the default for a specific field
	 * @param field the target field 
	 * @return a default object suitable for the given field
	 */
	public Object getDefaultForField(String field)
	{
		if(fieldSpecs.containsKey(field))
		{
			return fieldSpecs.get(field).getDefaultValue();	
		}
		else
		{
			return null;
		}		
	}	

	/**
	 * Check whether a field is optional
	 * @param field the field
	 * @return whether it's optional or not
	 */
	public boolean isFieldOptional(String field)
	{
		if(fieldSpecs.containsKey(field))
		{
			return fieldSpecs.get(field).isOptional();	
		}
		else
		{
			return true;
		}
	}	
	/**
	 * Add a new field specification (potentially replacing an old one)
	 * @param spec the new spec
	 * @return this object for fluent use
	 */
	public FieldSpecifications put(FieldSpecification spec)
	{
		fieldSpecs.put(spec.getFieldName(), spec);
		return this;
	}


	/**
	 * Create a new object based on the data in the provided object that adheres to the specs 
	 * @param source the source json object
	 * @param specs the specs to use
	 * @return a Valid field with all values that were ok from the original object.
	 */
	public static JsonObject filterFieldBySpec(JsonObject source, FieldSpecifications specs)
	{
		JsonObject target = new JsonObject();				
		for(String field : specs.getFields())
		{
			if(source.containsKey(field) && specs.getClassForField(field).isInstance(source.getValue(field)))
			{
				
				target.put(field, source.getValue(field));
			}
			else
			{
				if(!specs.isFieldOptional(field))
				{
					target.put(field, specs.getDefaultForField(field));
				}
			}
		}
		return target;
	}
	
	/**
	 * Same as @{link #filterFieldBySpec} but for an array of objects.
	 * @param sourceArray the source array of objects
	 * @param specs the specs each object shoudl adhere to
	 * @return a Array with one element per entry entry in the source Array which all adhere to the specs
	 */
	public static JsonArray applySpecToArray(JsonArray sourceArray, FieldSpecifications specs)
	{
		JsonArray result = new JsonArray();
		for(int i = 0; i < sourceArray.size(); ++i)
		{
			JsonObject source = sourceArray.getJsonObject(i);
			JsonObject target = new JsonObject(); 				
			for(String field : specs.getFields())
			{
				if(source.containsKey(field) && specs.getClassForField(field).isInstance(source.getValue(field)))
				{
					target.put(field, source.getValue(field));
				}
				else
				{
					if(!specs.isFieldOptional(field))
					{
						target.put(field, specs.getDefaultForField(field));
					}
				}
			}
			result.add(target);
		}
		return result;
	}

}
