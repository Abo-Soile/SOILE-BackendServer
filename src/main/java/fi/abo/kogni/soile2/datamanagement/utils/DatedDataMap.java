package fi.abo.kogni.soile2.datamanagement.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

/**
 * A Class that provides data data, to be able to check: 1. what data was there, and what is the latest data.
 * @author Thomas Pfau
 *
 * @param <K> The keys for the map
 * @param <T> the Targets in the map
 */
public class DatedDataMap<K,T> 
{		
	private HashMap<K,List<TimeStampedData<T>>> data = new HashMap<>(); 
	private HashMap<K,T> newestData = new HashMap<>(); 
	
	/**
	 * Get the latest daa for a given key object.
	 * @param key
	 * @return
	 */
	public T getLatest(Object key)
	{		
		List<TimeStampedData<T>> dataOptions = data.get(key);
		if(dataOptions == null)
		{
			return null;
		}
		return (T) ObjectUtils.max(dataOptions.toArray(new TimeStampedData[dataOptions.size()])).getData();
	}
	/**
	 * Get all Data stored under the given key.
	 * @param key
	 * @return
	 */
	public List<TimeStampedData<T>> get(Object key)
	{		
		return data.get(key);
	}	
	
	/**
	 * Get the latest element for a key along with its ime stamp as a {@link TimeStampedData} Object
	 * @param key
	 * @return
	 */
	public TimeStampedData<T> getLatestTimeStampedElement(Object key)
	{
		List<TimeStampedData<T>> dataOptions = data.get(key);
		if(dataOptions == null)
		{
			return null;
		}
		return ObjectUtils.max(dataOptions.toArray(new TimeStampedData[dataOptions.size()])); 
	}
	
	/**
	 * Add a {@link TimeStampedData} object to the list for a given Key. 
	 * @param key 
	 * @param value
	 * @return The Element represented by the {@link TimeStampedData} provided
	 */
	public T addDatedEntry(K key, TimeStampedData<T> value)
	{
		if(!this.containsKey(key))
		{
			data.put(key, new LinkedList<TimeStampedData<T>>());
		}
		data.get(key).add(value);		
		return newestData.put(key, getLatest(key));
	}
	
	/**
	 * Get a Map of the Newest data for each element. This object will be updated if the data in this map changes.
	 * @return
	 */
	public HashMap<K,T> getNewestData()
	{
		return newestData;
	}
	
	/**
	 * Clear the data in this map.
	 */
	public void clear() {
		data.clear();
		newestData.clear();
	}

	/**
	 * Check whether this map contains a given key.
	 * @param key
	 * @return
	 */
	public boolean containsKey(Object key) {
		return data.containsKey(key);
	}

	/**
	 * Get all keys available in this map.
	 * @return
	 */
	public Set<K> keySet() {
		return data.keySet();
	}

	/**
	 * Put a new object to the given key (Will add a {@link TimeStampedData} with a current timeStamp to the List of elements under the key,
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public T put(K arg0, T arg1) {
		return this.addDatedEntry(arg0, new TimeStampedData<T>(arg1));		 
	}

	/**
	 * Get the size of the map.
	 * @return
	 */
	public int size() {
		return data.size();
	}
	
}
