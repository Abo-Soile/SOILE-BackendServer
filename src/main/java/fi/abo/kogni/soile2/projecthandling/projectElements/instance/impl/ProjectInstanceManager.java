package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstanceFactory;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class is responsible for project information in the db.
 * This includes the project reference (to the respective git repo), the participants in this 
 * project etc. 
 * @author Thomas Pfau
 *
 */
public class ProjectInstanceManager implements DataRetriever<String, ProjectInstance> {

	MongoClient client;
	// should go to the constructor.
	private ProjectInstanceFactory dbFactory;
	private ProjectInstanceFactory createFactory;
	
	public ProjectInstanceManager(MongoClient client, EventBus eb)
	{
		this.client = client;
		String projectInstanceCollection = SoileConfigLoader.getdbProperty("projectCollection");	
		this.dbFactory = new DBProjectInstanceFactory(client, projectInstanceCollection, eb);
		this.createFactory = new JsonToDBProjectInstanceFactory(client, projectInstanceCollection, eb);		
	}

	public ProjectInstanceManager(MongoClient client, ProjectInstanceFactory createFactory, ProjectInstanceFactory dbFactory)
	{
		this.client = client;
		this.dbFactory = dbFactory;
		this.createFactory = createFactory;
		
	}
	
	@Override
	public Future<ProjectInstance> getElement(String key) {		
		return ProjectInstance.instantiateProject(new JsonObject().put("instanceID", key), dbFactory);				
	}

	@Override
	public void getElement(String key, Handler<AsyncResult<ProjectInstance>> handler) {
		// TODO Auto-generated method stub
		handler.handle(getElement(key));
	}

	/**
	 * Create a new Project with empty information provide it to the Handler
	 * database.
	 * @param handler the handler to handle the created participant
	 */
	public void startProject(String projectID, String projectVersion, Handler<AsyncResult<ProjectInstance>> handler)
	{
		handler.handle(startProject(projectID,projectVersion));
	}

	/**
	 * Create a new Project with empty information and retrieve a new ID from the 
	 * database.
	 * @param handler the handler to handle the created participant
	 */
	public Future<ProjectInstance> startProject(String projectID, String projectVersion )
	{	
		JsonObject defaultProjectInfo = getDefaultProjectInfo(projectID,projectVersion); 		
		return ProjectInstance.instantiateProject(defaultProjectInfo, createFactory);
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
							   .put("private", false);
	}
	
	public Future<JsonObject> save(ProjectInstance proj)
	{
		return proj.save();
	}
}