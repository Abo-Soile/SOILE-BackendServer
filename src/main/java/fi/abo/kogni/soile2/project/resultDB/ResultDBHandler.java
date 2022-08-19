package fi.abo.kogni.soile2.project.resultDB;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class ResultDBHandler {

	MongoClient client;

	/**
	 * Create a results document in the database and return the id for saving in the  
	 * @param resultID
	 * @param results
	 * @return
	 */
	public Future<String> createResults(JsonArray results)
	{	
		JsonObject query = new JsonObject().put("data", results); 
		return client.insert(SoileConfigLoader.getdbProperty("resultdb"),query);	
	}
	
	
}
