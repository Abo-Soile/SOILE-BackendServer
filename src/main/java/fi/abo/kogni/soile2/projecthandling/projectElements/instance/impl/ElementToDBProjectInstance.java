package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ShortCutExistsException;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
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

	private static final Logger log = LogManager.getLogger(ElementToDBProjectInstance.class.getName());	

	public ElementToDBProjectInstance(ElementManager<Project> manager, MongoClient client, EventBus eb) {
		super(manager, client, eb);
	}
	
	
	/**
	 * The Json provided to this Instance needs to contain:
	 * 1. "sourceUUID" of the project from which this was started
	 * 2. "Version" of the project from which this was started
	 * 3. "private" field wrt access for this 
	 * 4. "name" a name field.
	 * 5. "shortcut" (optional), that can be used as a shortcut to the project.
	 */
	@Override
	public Future<JsonObject> load(JsonObject inputJson)
	{
		Promise<JsonObject> savePromise = Promise.<JsonObject>promise();

		// This is a new project, so we first check, whether the provided shortcut OR name already exist.
		// if they do, this load is not allowed and will fail with a elementExist or ShortCutExist exception, depending on the returned value. 
		JsonObject query = new JsonObject();
		if(inputJson.containsKey("shortcut") && !"".equals(inputJson.getString("shortcut")))
		{
			// the shortcut is not allowed to clash with either the IDs OR other shortcuts 
			query.put("$or", new JsonArray().add(new JsonObject()
													 .put("name",inputJson.getString("name")))
											.add(new JsonObject()
													.put("shortcut",inputJson.getString("shortcut")))
											.add(new JsonObject()
													.put("_id",inputJson.getString("shortcut"))));
		}
		else
		{
			query.put("name",inputJson.getString("name"));
		}		
		client.findOne(getTargetCollection(), query, null)
		.onSuccess(res -> {
			log.debug("Got reply:" + res);
			if(res == null)
			{				
				// no collisions exist, so lets save it.
				// We will retrieve all necessary information from the input json.
				// it needs to contain the version, source project uuid, privacy, shortcut and name.
				// we add an empty participants array.
				JsonObject dbJson = new JsonObject();
				dbJson.put("participants", new JsonArray());
				dbJson.put("name", inputJson.getString("name"));
				if(inputJson.containsKey("sourceUUID"))
				{
					dbJson.put("sourceUUID", inputJson.getValue("sourceUUID"));
				}
				else
				{
					dbJson.put("sourceUUID", inputJson.getValue("UUID"));
				}
				dbJson.put("version", inputJson.getValue("version"));
				dbJson.put("private", inputJson.getBoolean("private",false));
				String shortcut = inputJson.getString("shortcut",null);
				if(shortcut != null)
				{
					dbJson.put("shortcut", inputJson.getString("shortcut"));
				}				
				log.debug("Trying to save Instance:\n" + dbJson.encodePrettily());

				client.save(getTargetCollection(), dbJson)
				.onSuccess( dbID -> {
					log.debug("DB Item has ID: " + dbID);
					// load it, so we got all we need.
					super.load(new JsonObject().put("_id", dbID))
					.onSuccess(dbResult -> {
						log.debug("Successfully loaded db Result");						
						savePromise.complete(dbResult);	
					})
					.onFailure(fail -> savePromise.fail(fail));
				})
				.onFailure(err -> savePromise.fail(err));				
			}
			else
			{
				log.debug(res.encodePrettily());
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
				if(res.getString("_id").equals(inputJson.getString("shortcut")))
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
