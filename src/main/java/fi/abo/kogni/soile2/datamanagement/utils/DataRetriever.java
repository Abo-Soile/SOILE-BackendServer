package fi.abo.kogni.soile2.datamanagement.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * A Dataretriever retrieves elements of type T for keys of type K
 * @author Thomas Pfau
 *
 * @param <K> The class of the key values of this retriever
 * @param <T> The class of the target values of this retriever
 */
public interface DataRetriever<K,T> {
	
	/**
	 * Get a Future of an Element of Type T given a key of type K
	 * @param key the key for which to retrieve the Element;
	 * @return A Future of the corresponding element of Type T. This fails if no element can be created by the retriever or no element exists and the retriever does not create elements.
	 */
	Future<T> getElement(K key);

	/**
	 * Same as getElement(K key) but the handler will be given the resulting future to handle; 
	 * @param key the of the element to get
	 * @param handler the result handler
	 */

	void getElement(K key, Handler<AsyncResult<T>> handler);	
}