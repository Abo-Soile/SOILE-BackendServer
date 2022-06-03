package fi.abo.kogni.soile2.utils;

import java.util.HashMap;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
/**
 * A Map that provides timestamped data obtained with a given retriever, which is used to fill in the data.
 * @author Thomas Pfau
 *
 */
public class TimeStampedDataMap {

	private HashMap<String, TimeStampedProperties> experimentMap = new HashMap<String, TimeStampedProperties>();
	DataRetriever retriever;
	long ttl;
	public TimeStampedDataMap(DataRetriever retriever, long TTL) {
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
	public void getProperties(String experimentUUID, Handler<AsyncResult<JsonObject>> resultHandler)
	{
		TimeStampedProperties expData = experimentMap.get(experimentUUID);
		if(expData == null)
		{
			retriever.getElement(experimentUUID, result -> {
				if(result.succeeded())
				{
					experimentMap.put(experimentUUID, new TimeStampedProperties(result.result(),ttl));
				}
				resultHandler.handle(result);				
			});
			
		}
		else
		{
			resultHandler.handle(Future.succeededFuture(expData.getProperties()));
		}
	}
	
	
}
