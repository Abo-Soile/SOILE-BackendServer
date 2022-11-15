package fi.abo.kogni.soile2.datamanagement.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public interface DataRetriever<K,T> {
	
	Future<T> getElement(K key);

	void getElement(K key, Handler<AsyncResult<T>> handler);	

}