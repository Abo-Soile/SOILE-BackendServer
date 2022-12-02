package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedData;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstanceFactory;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
	private GitManager gitManager;
	public ProjectInstanceManager(MongoClient client, EventBus eb)
	{		
		this(client,
				new DBProjectInstanceFactory(new GitManager(eb), client,SoileConfigLoader.getdbProperty("projectCollection"), eb),
				new ElementToDBProjectInstanceFactory(new GitManager(eb), client, SoileConfigLoader.getdbProperty("projectCollection"), eb)
				);				
	}

	public ProjectInstanceManager(MongoClient client, ProjectInstanceFactory createFactory, ProjectInstanceFactory dbFactory)
	{
		this.client = client;
		this.dbFactory = dbFactory;
		this.createFactory = createFactory;		
		 
	}
	
	@Override
	public Future<ProjectInstance> getElement(String key) {		
		return ProjectInstance.instantiateProject(new JsonObject().put("_id", key), dbFactory);				
	}

	@Override
	public void getElement(String key, Handler<AsyncResult<ProjectInstance>> handler) {
		// TODO Auto-generated method stub
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
	 * @param projectID - The UUID of the project 
	 * @param projectVersion - The version of the project to start
	 * @param projectVersion - The name of the instance.
	 */
	public Future<ProjectInstance> startProject(JsonObject projectInformation )
	{						
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
							   .put("private", false);
	}
	
	public Future<JsonObject> save(ProjectInstance proj)
	{
		return proj.save();
	}
}