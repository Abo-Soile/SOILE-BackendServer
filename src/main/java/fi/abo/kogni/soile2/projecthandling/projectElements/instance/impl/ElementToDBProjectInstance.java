package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ShortCutExistsException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class builds a project which does not yet have a Database representation, but which upon creation 
 * stores itself and updates using the received ID.
 * @author Thomas Pfau
 *
 */
public class ElementToDBProjectInstance extends DBProjectInstance{

	public ElementToDBProjectInstance(GitManager manager, MongoClient client, String projectInstanceDB, EventBus eb) {
		super(manager, client, projectInstanceDB, eb);
	}
	
	
	/**
	 * The Json provided to this Instance needs to contain:
	 * 1. "UUID" of the project from which this was started
	 * 2. "Version" of the project from which this was started
	 * 3. "private" field wrt access for this 
	 * 4. "name" a name field.
	 * 5. "shortcut" (optional), that can be used as a shortcut to the project.
	 */
	@Override
	public Future<JsonObject> load(JsonObject inputJson)
	{
		Promise<JsonObject> savePromise = Promise.<JsonObject>promise();
		// We will retrieve all necessary information from the input json.
		// it needs to contain the version, privacy
		JsonObject query = new JsonObject();
		if(inputJson.containsKey("shortcut") && !"".equals(inputJson.getString("shortcut")))
		{
			query.put("$or", new JsonArray().add(new JsonObject()
													 .put("name",inputJson.getString("name")))
											.add(new JsonObject()
													.put("shortcut",inputJson.getString("shortcut"))));
		}
		else
		{
			query.put("name",inputJson.getString("name"));
		}
		
		client.findOne(projectInstanceDB, query, null)
		.onSuccess(res -> {
			if(res == null)
			{
				// no collisions exist, so lets save it.
				client.save(projectInstanceDB, inputJson)
				.onSuccess( dbID -> {
					super.load(new JsonObject().put("_id", dbID))
					.onSuccess(dbResult -> {
						savePromise.complete(dbResult);	
					})
					.onFailure(fail -> savePromise.fail(fail));
				})
				.onFailure(err -> savePromise.fail(err));				
			}
			else
			{
				if(res.getString("name").equals(inputJson.getString("name")))
				{
					savePromise.fail(new ElementNameExistException(inputJson.getString("name"), res.getString("_id")));					
					return;
				}
				if(res.getString("shortcut").equals(inputJson.getString("shortcut")))
				{
					savePromise.fail(new ShortCutExistsException(inputJson.getString("shortcut")));
					return;
				}
			}
		})
		.onFailure(err -> savePromise.fail(err));		
		return savePromise.future();
	}
}
