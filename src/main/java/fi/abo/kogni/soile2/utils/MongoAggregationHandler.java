package fi.abo.kogni.soile2.utils;

import java.util.LinkedList;
import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mongo.MongoClient;

public class MongoAggregationHandler {
		
	public static Future<List<JsonObject>> aggregate(MongoClient client, String targetCollection, JsonArray pipeline )
	{
		Promise<List<JsonObject>> resultPromise = Promise.promise();
		List<JsonObject> result = new LinkedList<>();
		ReadStream<JsonObject> stream = client.aggregate(targetCollection, pipeline);
		stream.handler(object -> {
			// we will pause until we have finished this, this should avoid .
			System.out.println("Got a new Object: \n" + object.toString());
			stream.pause();
			result.add(object);
			stream.resume();
		});
		stream.endHandler(success -> {
			System.out.println("Finished reading");
			System.out.println(resultPromise);
			System.out.println(result);
			resultPromise.complete(result);
		});		
		stream.exceptionHandler(err -> {
			System.out.println(err);
			resultPromise.fail(err);
		});
		return resultPromise.future();
	}
	
	
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
