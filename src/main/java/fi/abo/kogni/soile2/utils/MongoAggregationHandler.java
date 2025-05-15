package fi.abo.kogni.soile2.utils;

import java.util.LinkedList;
import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mongo.MongoClient;


/**
 * Helper Methos for Mongo Aggregations, makes using aggregations a bit easier and less code intensive.
 * @author Thomas Pfau
 *
 */
public class MongoAggregationHandler {
		
	/**
	 * Convert the aggregation pipeline from a stream into a List of JsonObjects.
	 * @param client The MongoClient to use an aggregation with
	 * @param targetCollection the targetCollection for the aggregation
	 * @param pipeline the Pipeline (i.e. different aggregation commands)
	 * @return A Future of the all JsonObjects produced by the aggregation if the aggregation succeeded
	 */
	public static Future<List<JsonObject>> aggregate(MongoClient client, String targetCollection, JsonArray pipeline )
	{
		Promise<List<JsonObject>> resultPromise = Promise.promise();
		List<JsonObject> result = new LinkedList<>();
		ReadStream<JsonObject> stream = client.aggregate(targetCollection, pipeline);
		stream.handler(object -> {
			// we will pause until we have finished this, this should avoid concurrency problems.
			stream.pause();
			result.add(object);
			stream.resume();
		});
		stream.endHandler(success -> {
			resultPromise.complete(result);
		});		
		stream.exceptionHandler(err -> {
			resultPromise.fail(err);
		});
		return resultPromise.future();
	}
	
	
	/**
	 * Create a filter that filters checks whether the subField contains the subFieldIn data.
	 * @param field the field to in which to check for the subfield
	 * @param subField the subfield in which to check for the items
	 * @param subFieldIn the items to check
	 * @return the Filter {@link JsonObject}
	 */
	public static JsonObject createInFilter(String field, String subField, JsonArray subFieldIn)
	{
		return new JsonObject().put("$project", 
				new JsonObject().put(field, new JsonObject()
						.put("$filter", new JsonObject().put("input", "$" + field)
								.put("as", "currentField")
								.put("cond", new JsonObject()
										.put("$in", new JsonArray().add("$$currentField." + subField)
												.add(subFieldIn))))));
	}
	
	/**
	 * Create a filter that filters checks whether the subField is equal to a given object
	 * @param field the field to in which to check for the subfield
	 * @param subField the subfield in which to check for the items
	 * @param subFieldEqual the items the subField needs to be equal to.
	 * @return the Filter {@link JsonObject}
	 */
	public static JsonObject createEqFilter(String field, String subField, Object subFieldEqual)
	{
		return new JsonObject().put("$project", 
				new JsonObject().put(field, new JsonObject()
						.put("$filter", new JsonObject().put("input", "$" + field)
								.put("as", "currentField")
								.put("cond", new JsonObject()
										.put("$eq", new JsonArray().add("$$currentField." + subField)
												.add(subFieldEqual))))));
	}
		
}
