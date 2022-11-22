package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class builds a project which does not yet have a Database representation, but which upon creation 
 * stores itself and updates using the received ID.
 * @author Thomas Pfau
 *
 */
public class JsonToDBProjectInstance extends DBProjectInstance{

	public JsonToDBProjectInstance(MongoClient client, String projectInstanceDB, EventBus eb) {
		super(client, projectInstanceDB, eb);
	}
	
	
	@Override
	public Future<JsonObject> load(JsonObject inputJson)
	{
		Promise<JsonObject> savePromise = Promise.<JsonObject>promise();
		// the input json contains all data except for the _id field, which we need to retrieve 
		// from the database by storing an initial version of this project.		
		client.save(projectInstanceDB, inputJson).onSuccess( dbID -> {
			inputJson.put("_id", dbID);
			super.load(inputJson).onSuccess(dbResult -> {
				savePromise.complete(dbResult);
			}).onFailure(fail -> {
				savePromise.fail(fail);
			});
		}).onFailure(fail -> {
			savePromise.fail(fail);
		});
		return savePromise.future();
	}
}
