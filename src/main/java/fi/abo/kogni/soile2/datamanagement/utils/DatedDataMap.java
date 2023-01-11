package fi.abo.kogni.soile2.datamanagement.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

public class DatedDataMap<K,T> 
{		
	private HashMap<K,List<TimeStampedData<T>>> data = new HashMap<>(); 
	private HashMap<K,T> newestData = new HashMap<>(); 
	
	public T getLatest(Object key)
	{		
		List<TimeStampedData<T>> dataOptions = data.get(key);
		if(dataOptions == null)
		{
			return null;
		}
		return (T) ObjectUtils.max(dataOptions.toArray(new TimeStampedData[dataOptions.size()])).getData();
	}
	public List<TimeStampedData<T>> get(Object key)
	{		
		return data.get(key);
	}	
	
	public TimeStampedData<T> getLatestTimeStampedElement(Object key)
	{
		List<TimeStampedData<T>> dataOptions = data.get(key);
		if(dataOptions == null)
		{
			return null;
		}
		return ObjectUtils.max(dataOptions.toArray(new TimeStampedData[dataOptions.size()])); 
	}
	public T addDatedEntry(K key, TimeStampedData<T> value)
	{
		if(!this.containsKey(key))
		{
			data.put(key, new LinkedList<TimeStampedData<T>>());
		}
		data.get(key).add(value);		
		return newestData.put(key, getLatest(key));
	}
	
	public HashMap<K,T> getNewestData()
	{
		return newestData;
	}
	
	public void clear() {
		data.clear();
	}

	public boolean containsKey(Object key) {
		return data.containsKey(key);
	}

	public Set<K> keySet() {
		return data.keySet();
	}

	public T put(K arg0, T arg1) {
		return this.addDatedEntry(arg0, new TimeStampedData<T>(arg1));		 
	}

	public int size() {
		return data.size();
	}
	
}
