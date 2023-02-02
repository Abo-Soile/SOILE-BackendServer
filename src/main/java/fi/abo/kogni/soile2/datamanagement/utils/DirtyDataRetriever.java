package fi.abo.kogni.soile2.datamanagement.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * This interface is mainly for {@link CheckDirtyMap}. It allows retrieval of updated data if the existing data is no longer up to date. 
 * @author Thomas Pfau
 *
 * @param <K>
 * @param <T>
 */
public interface DirtyDataRetriever<K,T> {
	/**
	 * Get the element for the given key regardless on whether it is dirty.
	 * @param key The key for which to retrieve an element
	 * @return A {@link Future} of the requested Element
	 */
	Future<T> getElement(K key);
	/**
	 * Get the element for the given key regardless on whether it is dirty.
	 * @param key The key for which to retrieve an element
	 * @param handler The handler to handle the Future of the requested element
	 */
	void getElement(K key, Handler<AsyncResult<T>> handler);
	
	/**
	 * Get a new element only if the requested element is dirty. This retriever needs to track the information on whether it is "dirty"/changed and needs to be reloaded
	 * If not altered this request returns a <code>null</code> object if it was "dirty" a new object is returned.
	 * @param key the key for which to look up an element.
	 * @return
	 */
	Future<T> getElementIfDirty(K key);
	
	/**
	 * Same as getElementIfDirty(key), but the resulting future will be provided to thegiven handler.
	 * @param key
	 * @param handler
	 */
	void getElementIfDirty(K key, Handler<AsyncResult<T>> handler);
}
