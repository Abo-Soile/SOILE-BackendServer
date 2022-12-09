package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ShortCutExistsException;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
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

	public ElementToDBProjectInstance(ElementManager<Project> manager, MongoClient client, String projectInstanceDB, EventBus eb) {
		super(manager, client, projectInstanceDB, eb);
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
			query.put("$or", new JsonArray().add(new JsonObject()
													 .put("name",inputJson.getString("name")))
											.add(new JsonObject()
													.put("shortcut",inputJson.getString("shortcut"))));
		}
		else
		{
			query.put("name",inputJson.getString("name"));
		}
		log.debug("Trying to load element from DB: \n" + query.encodePrettily());
		client.findOne(projectInstanceDB, query, null)
		.onSuccess(res -> {
			log.debug("Got reply:" + res);
			if(res == null)
			{				
				// no collisions exist, so lets save it.
				// We will retrieve all necessary information from the input json.
				// it needs to contain the version, source project uuid, privacy, shortcut and name.
				// we add an empty participants array.
				inputJson.put("participants", new JsonArray());
				log.debug("Trying to save Instance:\n" + inputJson.encodePrettily());

				client.save(projectInstanceDB, inputJson)
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
			}
		})
		.onFailure(err -> savePromise.fail(err));		
		return savePromise.future();
	}
}
