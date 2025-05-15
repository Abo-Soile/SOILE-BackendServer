package fi.abo.kogni.soile2.datamanagement.utils;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;

/**
 * A JsonObject that also works as a map for output values (Doubles)
 * @author Thomas Pfau
 */
public class OutputMap extends JsonObject{
	static final Logger LOGGER = LogManager.getLogger(OutputMap.class);

	private HashMap<String,Double> numberMap = new HashMap<String,Double>();
	@Override
	public JsonObject put(String key, Object value)
	{
		if(value instanceof Number)
		{					
			numberMap.put(key, ((Number)value).doubleValue());
		}
		else
		{
			// make sure this is not added as a number.
			numberMap.remove(key);		
		}
		return super.put(key,  value);			
	}
	
	@Override
	public Object remove(String key)  
	{
		numberMap.remove(key);
		return super.remove(key);
	}
	/**
	 * Get the map of numbers stored in this Json Object
	 * @return the part of the Json Object that contains numbers.
	 */
	
	public HashMap<String,Double> getOutputMap()	
	{
		return numberMap;
	}
}