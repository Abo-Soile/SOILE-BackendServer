package fi.abo.kogni.soile2.datamanagement.utils;

import java.util.function.Function;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * Generic implementation of a DataRetriever using a retrieval function.
 * @author Thomas Pfau
 *
 * @param <K> The Keys
 * @param <T> The target elements to retrieve.
 */
public class DataRetrieverImpl<K,T> implements DataRetriever<K,T> {

	private Function<K,Future<T>> retrievalFunction;
	
	/**
	 * Default constructor
	 * @param retrievalFunction the function that is used to retrieve the data
	 */
	public DataRetrieverImpl(Function<K,Future<T>> retrievalFunction)
	{
		this.retrievalFunction = retrievalFunction;
	}
		
	@Override
	public Future<T> getElement(K key) {
		// TODO Auto-generated method stub
		return retrievalFunction.apply(key);
	}

	@Override
	public void getElement(K key, Handler<AsyncResult<T>> handler) {
		handler.handle(getElement(key));	
	}	
}
