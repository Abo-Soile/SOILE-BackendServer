package fi.abo.kogni.soile2.datamanagement.utils;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.CollectionUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
/**
 * A Map that provides timestamped data obtained with a given retriever, which is used to fill in the data.
 * @author Thomas Pfau
 *
 */
public class TimeStampedMap<K,T> {

	private ConcurrentHashMap<K, TimeStampedData<T>> experimentMap = new ConcurrentHashMap<K, TimeStampedData<T>>();
	DataRetriever<K,T> retriever;
	long ttl;
	public TimeStampedMap(DataRetriever<K,T> retriever, long TTL) {
		this.retriever = retriever;
		ttl = TTL;
	}
	
	/**
	 * Clean up old data, that is not currently in use
	 * @return the removed data.
	 */
	public Collection<T> cleanup()
	{
		Collection<K> toClean = CollectionUtils.select(experimentMap.keySet(), key -> !experimentMap.get(key).isValid()); 
		Collection<T> cleaned = CollectionUtils.collect(toClean, key -> experimentMap.get(key).getData());				
		experimentMap.keySet().removeAll(toClean);
		return cleaned;
	}
	
	/**
	 * Get an item with a specific key. The Map tries to retrieve it if it can't be found. 
	 * If it can't be found the handler has to handle a failedFuture with a {@link NoSuchElementException} error 
	 * @param itemID
	 * @return the item to be looked for. or null, if it doesn't exist.
	 */
	public void getData(K itemID, Handler<AsyncResult<T>> resultHandler)
	{
		resultHandler.handle(getData(itemID));
	}
	
	/**
	 * Get an item with a specific key. The Map tries to retrieve it if it can't be found. 
	 * If it can't be found the handler has to handle a failedFuture with a {@link NoSuchElementException} error 
	 * @param itemID
	 * @return the item to be looked for. or null, if it doesn't exist.
	 */
	public Future<T> getData(K itemID)
	{
		TimeStampedData<T> expData = experimentMap.get(itemID);
		Promise<T> itemPromise = Promise.<T>promise();
		if(expData == null)
		{
			retriever.getElement(itemID).onComplete(result -> {
				if(result.succeeded())
				{
					this.putData(itemID, result.result());
					itemPromise.complete(result.result());
				}
				else
				{
					itemPromise.fail(result.cause());
				}
			});			
		}
		else
		{
			// refresh the timestamp.
			expData.updateStamp();
			itemPromise.complete(expData.getData());
		}
		return itemPromise.future();
	}
	
	/**
	 * Save an item with a given id.
	 * @param ItemID
	 * @param data
	 */
	public void putData(K ItemID, T data)
	{
		experimentMap.put(ItemID, new TimeStampedData<T>(data, ttl));
	}
	
	/**
	 * Maanually clean an element from the Mapping.
	 * @param ItemID
	 */
	public void cleanElement(K ItemID)
	{
		experimentMap.remove(ItemID);
	}
}
