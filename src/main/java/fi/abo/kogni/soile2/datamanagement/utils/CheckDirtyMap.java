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
 * Map further makes use of indication that a certain element is dirty before returning it. Returning elements can thus take a bit longer but 
 * we can handle elements that are mutable bu need quite some data 
 * @author Thomas Pfau
 *
 */
public class CheckDirtyMap<K,T> {

	private ConcurrentHashMap<K, TimeStampedData<T>> elementMap = new ConcurrentHashMap<K, TimeStampedData<T>>();
	DirtyDataRetriever<K,T> retriever;
	long ttl;
	public CheckDirtyMap(DirtyDataRetriever<K,T> retriever, long TTL) {
		this.retriever = retriever;
		ttl = TTL;
	}
	
	/**
	 * Clean up old data, that is not currently in use
	 * @return the removed data.
	 */
	public Collection<T> cleanup()
	{
		Collection<K> toClean = CollectionUtils.select(elementMap.keySet(), key -> !elementMap.get(key).isValid()); 
		Collection<T> cleaned = CollectionUtils.collect(toClean, key -> elementMap.get(key).getData());				
		elementMap.keySet().removeAll(toClean);
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
		TimeStampedData<T> expData = elementMap.get(itemID);
		Promise<T> itemPromise = Promise.<T>promise();
		if(expData == null)
		{			
				getElementFromRetriever(itemID, itemPromise);	
		}
		else
		{
			// refresh the timestamp.
			retriever.getElementIfDirty(itemID)
			.onSuccess(element -> {
				if(element != null)
				{					
					this.putData(itemID, element);
					itemPromise.complete(element);
				}
				else
				{
					expData.updateStamp();
					itemPromise.complete(expData.getData());
				}
			})
			.onFailure(err -> itemPromise.fail(err));
			
		}
		return itemPromise.future();
	}
	
	private void getElementFromRetriever(K itemID, Promise<T> itemPromise)
	{
		retriever.getElement(itemID).onSuccess(result -> {
			this.putData(itemID, result);
			itemPromise.complete(result);
		})
		.onFailure(err -> itemPromise.fail(err));
	}
	
	/**
	 * Save an item with a given id.
	 * @param ItemID
	 * @param data
	 */
	private TimeStampedData<T> putData(K ItemID, T data)
	{
		return elementMap.put(ItemID, new TimeStampedData<T>(data, ttl));
	}
	
	/**
	 * Maanually clean an element from the Mapping.
	 * @param ItemID
	 */
	public void cleanElement(K ItemID)
	{
		elementMap.remove(ItemID);
	}
}
