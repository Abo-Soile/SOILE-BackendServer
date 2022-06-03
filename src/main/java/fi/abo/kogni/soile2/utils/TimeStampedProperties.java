package fi.abo.kogni.soile2.utils;

import java.io.InvalidClassException;

import io.vertx.core.json.JsonObject;

public class TimeStampedProperties extends TimeStampedData{

	public TimeStampedProperties(JsonObject properties, long ttl)
	{
		super(properties,ttl);
	}
	
	public JsonObject getProperties() throws InvalidClassException
	{
		if(data instanceof JsonObject)
		{
			return (JsonObject)data;
		}
		else
		{
			throw new InvalidClassException("Expected data to be " + JsonObject.class.getCanonicalName() + ". Found " + data.getClass().getCanonicalName() +  " instead");
		}
	}

}
