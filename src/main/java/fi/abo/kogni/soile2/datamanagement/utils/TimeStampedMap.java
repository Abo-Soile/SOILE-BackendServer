package fi.abo.kogni.soile2.datamanagement.utils;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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

	private ConcurrentHashMap<K, TimeStampedData<T>> elementMap = new ConcurrentHashMap<K, TimeStampedData<T>>();
	DataRetriever<K,T> retriever;
	long ttl;
	public TimeStampedMap(DataRetriever<K,T> retriever, long TTL) {
		this.retriever = retriever;
		ttl = TTL;
	}
	
	public TimeStampedMap(Function<K,Future<T>> retrievalFunction, long TTL) {
		retriever = new DataRetrieverImpl<>(retrievalFunction);
		ttl = TTL;
	}
	
	/**
	 * Clean up old data, that is not currently in use
	 * @return the removed data.
	 */
	public Collection<T> cleanUp()
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
			retriever.getElement(itemID).onSuccess(result -> {
					this.putData(itemID, result);
					itemPromise.complete(result);
			})
			.onFailure(err -> {
				itemPromise.fail(err);
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
		elementMap.put(ItemID, new TimeStampedData<T>(data, ttl));
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
