package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
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
public class ElementToDBStudy extends DBStudy{

	private static final Logger LOGGER = LogManager.getLogger(ElementToDBStudy.class.getName());	

	/**
	 * Default constructor
	 * @param manager {@link ElementManager} for {@link Project} access
	 * @param client {@link MongoClient} for db access 
	 * @param eb {@link EventBus} for communication
	 */
	public ElementToDBStudy(ElementManager<Project> manager, MongoClient client, EventBus eb) {
		super(manager, client, eb);
	}


	/**
	 * The Json provided to this Instance needs to contain, essentially it can be either a APIProject or a JsonObject that has the respective fields.:
	 * 1 a. either "sourceProject" a JsonObject with the fields { "UUID" the UUID of the source project and "version", the version of the project  }
	 * 	  Example:   {"sourceProject" : { "UUID" : "abcde" , "version" : "defgh"}}
	 * 
	 * 2. "private" field wrt access for this 
	 * 3. "name" a name field.
	 * 4. "shortcut" (optional), that can be used as a shortcut to the project.
	 */
	@Override
	public Future<JsonObject> load(JsonObject inputJson)
	{
		Promise<JsonObject> savePromise = Promise.<JsonObject>promise();

		projectManager.getAPIElementFromDB(inputJson.getJsonObject("sourceProject").getString("UUID"), inputJson.getJsonObject("sourceProject").getString("version"))
		.onSuccess(apiProject -> {
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
				LOGGER.debug("Got reply:" + res);
				LOGGER.debug(inputJson.encodePrettily());
				if(res == null)
				{				
					// no collisions exist, so lets save it.
					// We will retrieve all necessary information from the input json.
					// it needs to contain the version, source project UUID, privacy, shortcut and name.
					// we add an empty participants array.
					JsonObject dbJson = new JsonObject();
					dbJson.put("participants", new JsonArray());
					dbJson.put("name", inputJson.getString("name"));
					dbJson.put("language", inputJson.getString("language", "en"));
					dbJson.put("description", inputJson.getString("description", "This is a new project"));
					dbJson.put("shortDescription", inputJson.getString("shortDescription", "This is a new project"));
					// now, handle the randomizers
					dbJson.put("randomizerPasses", createRandomizerArray(((APIProject)apiProject).getRandomizers()));
					if(inputJson.containsKey("sourceProject"))
					{
						// this is created via a path
						dbJson.put("sourceUUID", inputJson.getJsonObject("sourceProject").getValue("UUID"));
						dbJson.put("version", inputJson.getJsonObject("sourceProject").getValue("version"));
					}
					else
					{
						//this is created directly from a project API instance.
						dbJson.put("sourceUUID", inputJson.getValue("UUID"));
						dbJson.put("version", inputJson.getValue("version"));
					}
					dbJson.put("private", inputJson.getBoolean("private",false));
					dbJson.put("active", inputJson.getBoolean("active",false));
					String shortcut = inputJson.getString("shortcut",null);
					if(shortcut != null && !"".equals(shortcut)) // an empty string can't be a shortcut, and we won't put it in.
					{
						dbJson.put("shortcut", inputJson.getString("shortcut"));
					}				
					LOGGER.debug("Trying to save Instance:\n" + dbJson.encodePrettily());
					dbJson.put("modifiedStamp", new Date().getTime());
					client.save(getTargetCollection(), dbJson)
					.onSuccess( dbID -> {
						LOGGER.debug("DB Item has ID: " + dbID);
						// load it, so we got all we need.
						super.load(new JsonObject().put("_id", dbID))
						.onSuccess(dbResult -> {
							LOGGER.debug("Successfully loaded db Result");						
							savePromise.complete(dbResult);	
						})
						.onFailure(fail -> savePromise.fail(fail));
					})
					.onFailure(err -> savePromise.fail(err));				
				}
				else
				{
					LOGGER.debug(res.encodePrettily());
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
		})
		.onFailure(err -> savePromise.fail(err));
		return savePromise.future();

	}
}
