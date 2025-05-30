package fi.abo.kogni.soile2.datamanagement.utils;

import java.util.Date;

/**
 * A class for time stamped data. Has an optional TTL parameter, which can lead to the data becoming invalid
 * 
 * @author Thomas Pfau
 *
 */
public class TimeStampedData<T> implements Comparable<TimeStampedData<T>>{

	Long timeToLive;
	Long dataDate;
	T data;
	
	/**
	 * Create a new Property with a specified time to live (in ms)
	 * @param data the JsonObject to store
	 * @param ttl the time to live
	 */
	public TimeStampedData(T data, long ttl)
	{		
		this(data,System.currentTimeMillis(),ttl);		
	}	
	/**
	 * Create a new Property with a specified expiry time
	 * @param data the JsonObject to store
	 * @param date the date at which the element expires 
	 */
	public TimeStampedData(T data, Date date)
	{
		this(data,date,null);
	}
	/**
	 * Timestamped data with just data
	 * @param data the data 
	 */
	public TimeStampedData(T data)
	{		
		this(data, System.currentTimeMillis(), null);
	}
	
	/**
	 * Time stamped data with a ttl and a expiration data in ms
	 * if ttl is null there is no expiration date
	 * @param data the data
	 * @param date the generation date
	 * @param ttl the time to live
	 */
	public TimeStampedData(T data, Long date, Long ttl)
	{
		this.data = data;
		dataDate = date;
		this.timeToLive = ttl;
	}
	
	/**
	 * Time stamped data with a ttl and a expiration date as a {@link Date}
	 * if ttl is null there is no expiration date
	 * @param data the data
	 * @param date the generation date
	 * @param ttl the time to live
	 */
	public TimeStampedData(T data, Date date, Long ttl)
	{
		this.data = data;
		dataDate = date.getTime();
		timeToLive = ttl;
	}
	
	
	/**
	 * Get the actual data
	 * @return the data of this object
	 */
	public T getData()
	{
		return data;
	}
		
	/**
	 * Test, whether this is still valid
	 * @return whether the element is valid
	 */
	public boolean isValid()
	{
		if(timeToLive != null)
		{
			return System.currentTimeMillis() - dataDate < timeToLive;
		}
		else
		{
			return true;
		}
	}
	/**
	 * Update the creation Timestamp 
	 * Can be relevant if the timestamp indicates last usage. 
	 */
	public void updateStamp()
	{
		dataDate = System.currentTimeMillis();
	}
	@Override
	public int compareTo(TimeStampedData<T> o) {
		return this.dataDate.compareTo(o.dataDate);
	}
	
	/**
	 * Get the time stamp this Data was stored with
	 * @return the timestamp
	 */
	public Long getTimeStamp()
	{
		return dataDate;
	}
}
