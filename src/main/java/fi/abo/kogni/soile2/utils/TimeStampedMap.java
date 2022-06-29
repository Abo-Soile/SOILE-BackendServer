package fi.abo.kogni.soile2.utils;

import java.util.HashMap;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
/**
 * A Map that provides timestamped data obtained with a given retriever, which is used to fill in the data.
 * @author Thomas Pfau
 *
 */
public class TimeStampedMap<K,T> {

	private HashMap<K, TimeStampedData<T>> experimentMap = new HashMap<K, TimeStampedData<T>>();
	DataRetriever<K,T> retriever;
	long ttl;
	public TimeStampedMap(DataRetriever<K,T> retriever, long TTL) {
		this.retriever = retriever;
		ttl = TTL;
	}
	
	public void cleanup()
	{
		experimentMap.keySet().removeIf(key -> !experimentMap.get(key).isValid());
	}
	
	/**
	 * Obtain the properties of this experiment for the given resultHandler  
	 * @param experimentUUID
	 * @return
	 */
	public void getData(K experimentUUID, Handler<AsyncResult<T>> resultHandler)
	{
		TimeStampedData<T> expData = experimentMap.get(experimentUUID);
		if(expData == null)
		{
			retriever.getElement(experimentUUID, result -> {
				if(result.succeeded())
				{
					experimentMap.put(experimentUUID, new TimeStampedData<T>(result.result(),ttl));
				}
				resultHandler.handle(result);				
			});
			
		}
		else
		{
			resultHandler.handle(Future.succeededFuture(expData.getData()));
		}
	}
	
	
}
