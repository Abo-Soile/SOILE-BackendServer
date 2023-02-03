package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstanceFactory;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class is responsible for project information in the db.
 * This includes the project reference (to the respective git repo), the participants in this 
 * project etc. 
 * @author Thomas Pfau
 *
 */
public class ProjectInstanceManager implements DataRetriever<String, ProjectInstance> {

	static final Logger LOGGER = LogManager.getLogger(ProjectInstanceManager.class);

	MongoClient client;
	// should go to the constructor.
	private ProjectInstanceFactory dbFactory;
	private ProjectInstanceFactory createFactory;
	private String instanceCollection;
	public ProjectInstanceManager(MongoClient client, Vertx vertx)
	{						
		this(client,
				new ElementToDBProjectInstanceFactory(ElementManager.getProjectManager(client, vertx),client, vertx.eventBus()),								
				new DBProjectInstanceFactory(ElementManager.getProjectManager(client, vertx),client, vertx.eventBus())				 
				);				
	}		
		
	public ProjectInstanceManager(MongoClient client, ProjectInstanceFactory createFactory, ProjectInstanceFactory dbFactory)
	{
		this.client = client;
		this.dbFactory = dbFactory;
		this.createFactory = createFactory;		
		instanceCollection = SoileConfigLoader.getdbProperty("projectInstanceCollection");
	}
	
	@Override
	public Future<ProjectInstance> getElement(String key) {		
		return ProjectInstance.instantiateProject(new JsonObject().put("_id", key), dbFactory);				
	}

	@Override
	public void getElement(String key, Handler<AsyncResult<ProjectInstance>> handler) {
		handler.handle(getElement(key));
	}

	/**
	 * Create a new Project with empty information provide it to the Handler
	 * database.
	 * @param projectID - The UUID of the project 
	 * @param projectVersion - The version of the project to start
	 * @param projectVersion - The name of the instance.
	 * @param handler the handler to handle the created participant
	 */
	public ProjectInstanceManager startProject(JsonObject projectInformation, Handler<AsyncResult<ProjectInstance>> handler, String projectInstanceName)
	{
		handler.handle(startProject(projectInformation));
		return this;
	}

	/**
	 * Create a new Project with empty information and retrieve a new ID from the 
	 * database.
	 * 1. "UUID" of the project from which this was started
	 * 2. "Version" of the project from which this was started
	 * 3. "private" field wrt access for this 
	 * 4. "name" a name field.
	 * 5. "shortcut" (optional), that can be used as a shortcut to the project.
	 */
	public Future<ProjectInstance> startProject(JsonObject projectInformation )
	{						
		LOGGER.debug("Trying to instanciate Project ");
		return ProjectInstance.instantiateProject(projectInformation, createFactory);
	}
	
	/**
	 * Default schema of a Participant.
	 * @return an empty participant information {@link JsonObject}
	 */
	public static JsonObject getDefaultProjectInfo(String projectVersion, String projectID)
	{
		return new JsonObject().put("projectID", projectID)
							   .put("version", projectVersion)
							   .put("participants", new JsonArray())
							   .put("private", false)
							   .put("accessTokens",new JsonArray());
	}
	
	/**
	 * Save the given Project instance.
	 * @param proj
	 * @return
	 */
	public Future<JsonObject> save(ProjectInstance proj)
	{
		return proj.save();
	}
	
	/**
	 * Get a list of uuid of project instances and their privacy status based on the permissions provided (those are essentially _ids)
	 * @param projectInstanceIDs A JsonArray with strings for each permission/projectID
	 * @return
	 */
	public Future<JsonArray> getProjectInstanceStatus(JsonArray projectInstanceIDs)
	{
		Promise<JsonArray> listPromise = Promise.promise();		 				
		client.findWithOptions(instanceCollection,new JsonObject().put("private", false).put("_id", projectInstanceIDs),new FindOptions().setFields(new JsonObject().put("_id",1).put("name", 1)))
		.onSuccess(items -> 
				{
					JsonArray result = new JsonArray();
					for(JsonObject o : items)
					{
						o.put("uuid", o.getString("_id")).remove("_id");
						result.add(o);
					}
					listPromise.complete(result);
							
				})
		.onFailure(err -> listPromise.fail(err));
		
		return listPromise.future();
	}
	
}