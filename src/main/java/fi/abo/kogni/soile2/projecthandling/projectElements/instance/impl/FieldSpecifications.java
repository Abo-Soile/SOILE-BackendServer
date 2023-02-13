package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashMap;
import java.util.Set;

import io.vertx.core.json.JsonObject;

public class FieldSpecifications {

	
	private HashMap<String,FieldSpecification> fieldSpecs = new HashMap<>();
	
	public FieldSpecifications() {
		// TODO Auto-generated constructor stub
	}
	
	
	public Set<String> getFields()
	{
		return fieldSpecs.keySet();
	}
	
	public Class getClassForField(String field)
	{
		if(fieldSpecs.containsKey(field))
		{
			return fieldSpecs.get(field).getClass();	
		}
		else
		{
			return null;
		}
	}
	
	public boolean hasField(String field)
	{
		return fieldSpecs.containsKey(field);		
	}
	
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
	public FieldSpecifications put(FieldSpecification spec)
	{
		fieldSpecs.put(spec.getFieldName(), spec);
		return this;
	}
	
	
	
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

}
