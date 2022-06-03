package fi.abo.kogni.soile2.utils;

/**
 * A class for time stamped data. The data becomes invalid after the time stamp expires.
 * 
 * @author Thomas Pfau
 *
 */
public class TimeStampedData<T> {

	long validTill;
	T data;
	
	/**
	 * Create a new Property with a specified time to live (in ms)
	 * @param properties the JsonObject to store
	 * @param ttl the time to live
	 */
	public TimeStampedData(T data, long ttl)
	{		
		this.validTill = System.currentTimeMillis() + ttl;
		this.data = data;
	}
		

	public T getData()
	{
		return data;
	}
		
	public boolean isValid()
	{
		return validTill > System.currentTimeMillis();
	}
	
	
}
