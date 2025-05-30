package fi.abo.kogni.soile2.migrations;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.SetupServer;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;

/**
 * This migration adds a Additional fields to the task database used for storing further information.
 * 
 * @author Thomas Pfau
 *
 */
public class TaskFieldAddition {
	static final Logger LOGGER = LogManager.getLogger(TaskFieldAddition.class);

	MongoClient client;
	/**
	 * Default constructor
	 * @param client the {@link MongoClient} for db access
	 */
	public TaskFieldAddition(MongoClient client)
	{
		this.client = client;
	}
	/**
	 * Add all fields
	 * @return A {@link Future} thats successfull if this migration worked
	 */
	public Future<Void> run()
	{
		Promise<Void> updatePromise = Promise.promise();
		LOGGER.info("Updating author field");
		findAndUpdateField("author", "UNKNOWN")
		.compose(v -> {
			return findAndUpdateField("description", "$name");
		})		
		.compose(v -> {
			LOGGER.info("Updating keyword field");
			return findAndUpdateField("keywords", new JsonArray());
		})
		.compose(v -> {
			LOGGER.info("Updating language field");
			return findAndUpdateField("language", "UNKNOWN");
		})
		.compose(v -> {
			LOGGER.info("Updating type");
			return findAndUpdateField("type", "UNKNOWN");
		})
		.compose(v -> {
			LOGGER.info("Updating created");
			return updateCreated();
		})
		.onSuccess(done -> {
			LOGGER.info("Update done");
			updatePromise.complete();
		})
		.onFailure(err -> updatePromise.fail(err));

		return updatePromise.future();

	}


	private Future<Void> findAndUpdateField(String fieldName, Object defaultValue)
	{
		JsonObject command = new JsonObject().put("update", SoileConfigLoader.getCollectionName("taskCollection"))
											 .put("updates", new JsonArray().add(new JsonObject()
													 					.put("q", new JsonObject().put(fieldName, new JsonObject().put("$exists", false)))
													 					.put("u", new JsonArray().add(new JsonObject().put("$set", new JsonObject().put(fieldName, defaultValue))))
													 					.put("multi", true)
													 ));
		
		return this.client.runCommand("update", command)
				.compose(res -> {
					LOGGER.info("Updated " + res.getNumber("nModified") + " documents");
					if(res.getNumber("ok").equals(1.0))
					{
						return Future.succeededFuture();
					}
					else
					{
						return Future.failedFuture("Command did not succeed");
					}
				})
				.mapEmpty();		
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
			// if nothing needs to be updated, nothing needs to be done.
			if(result.size() > 0)
			{
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
			}
			else {
				return Future.succeededFuture();
			}
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


