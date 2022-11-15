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
	 * @param properties the JsonObject to store
	 * @param ttl the time to live
	 */
	public TimeStampedData(T data, long ttl)
	{		
		this(data,System.currentTimeMillis(),ttl);		
	}	
	
	public TimeStampedData(T data, Date date)
	{
		this(data,date,null);
	}
	/**
	 * 
	 * @param data
	 */
	public TimeStampedData(T data)
	{		
		this(data, System.currentTimeMillis(), null);
	}
	
	public TimeStampedData(T data, Long date, Long ttl)
	{
		this.data = data;
		dataDate = date;
		this.timeToLive = ttl;
	}
	
	public TimeStampedData(T data, Date date, Long ttl)
	{
		this.data = data;
		dataDate = date.getTime();
		timeToLive = ttl;
	}
	
	
	/**
	 * Get the actual data
	 * @return
	 */
	public T getData()
	{
		return data;
	}
		
	/**
	 * Test, whether this is still valid
	 * @return
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
		// TODO Auto-generated method stub
		return this.dataDate.compareTo(o.dataDate);
	}
	
}
