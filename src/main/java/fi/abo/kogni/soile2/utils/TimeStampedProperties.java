package fi.abo.kogni.soile2.utils;

import io.vertx.core.json.JsonObject;

public class TimeStampedProperties {

	long validTill;
	JsonObject properties;
	
	public TimeStampedProperties(JsonObject properties, long ttl)
	{		
		this.validTill = System.currentTimeMillis() + ttl;
		this.properties = properties;
	}
		
	public boolean isValid()
	{
		return validTill > System.currentTimeMillis();
	}
	
	public JsonObject getProperties()
	{
		return properties;
	}
		
}
