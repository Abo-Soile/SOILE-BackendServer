package fi.abo.kogni.soile2.migrations;

import java.util.LinkedList;
import java.util.List;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;

/**
 * This migration adds a Additional fields to the task database used for storing further information.
 * 
 * @author Thomas Pfau
 *
 */
public class TaskFieldAddition {
	MongoClient client;
	public TaskFieldAddition(MongoClient client)
	{
		this.client = client;
	}
	/**
	 * Add all fields
	 * @return
	 */
	public Future<Void> run()
	{
		Promise<Void> updatePromise = Promise.promise();
		findAndUpdateField("author", "UNKNOWN")
		.compose(v -> {
			return findAndUpdateField("description", "$name");
		})		
		.compose(v -> {
			return findAndUpdateField("keywords", new JsonArray());
		})
		.compose(v -> {
			return findAndUpdateField("language", "UNKNOWN");
		})
		.compose(v -> {
			System.out.println("Updating type");
			return findAndUpdateField("type", "UNKNOWN");
		})
		.compose(v -> {
			System.out.println("Updating created");
			return updateCreated();
		})
		.onSuccess(done -> {
			System.out.println("Update done");
			updatePromise.complete();
		})
		.onFailure(err -> updatePromise.fail(err));
		
		return updatePromise.future();
		
	}
	
	
	private Future<Void> findAndUpdateField(String fieldName, Object defaultValue)
	{
		return this.client.updateCollection(SoileConfigLoader.getCollectionName("taskCollection"),
									 new JsonObject().put(fieldName, new JsonObject().put("$exists", false)),
									 new JsonObject().put("$set", new JsonObject().put(fieldName, defaultValue))).mapEmpty();
	}
	/**
	 * Add the created field. This is essentially the earliest possible version.
	 * @return
	 */
	private Future<Void> updateCreated()
	{
		
		Promise<Void> updatedPromise = Promise.promise();
		
		this.client.find(SoileConfigLoader.getCollectionName("taskCollection"),
									 new JsonObject().put("created", new JsonObject().put("$exists", false)))
		.compose(result -> {
			System.out.println("Waaaaaaaaaaaah");
			
			List<BulkOperation> ops = new LinkedList<>();
			for(int i = 0; i < result.size(); i++)
			{
				JsonObject o = result.get(i);
				JsonObject query = new JsonObject().put("_id", o.getString("_id"));				
				JsonObject update = new JsonObject().put("$set", new JsonObject().put("created",getMinVersion(o.getJsonArray("versions"))));
				ops.add(BulkOperation.createUpdate(query, update));
				System.out.println(update.encodePrettily());
				System.out.println(query.encodePrettily());
			}						
			return client.bulkWrite(SoileConfigLoader.getCollectionName("taskCollection"), ops);
		})
		.onSuccess(done -> {
			System.out.println("Update complete");
			updatedPromise.complete();
		})
		.onFailure(err -> updatedPromise.fail(err));
		return updatedPromise.future();
	}
	
	private long getMinVersion(JsonArray versions)
	{
		long minVer = Long.MAX_VALUE;
		for(int i = 0; i < versions.size(); ++i) {
			long currentStamp = versions.getJsonObject(i).getLong("timestamp",System.currentTimeMillis());
			if( currentStamp < minVer) {
				minVer = currentStamp; 
			}
		}
		return minVer;
	}
}


