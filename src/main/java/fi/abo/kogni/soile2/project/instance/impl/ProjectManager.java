package fi.abo.kogni.soile2.project.instance.impl;

import fi.abo.kogni.soile2.project.instance.ProjectFactory;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.utils.DataRetriever;
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
public class ProjectManager implements DataRetriever<String, ProjectInstance> {

	MongoClient client;
	// should go to the constructor.
	private ProjectFactory dbFactory;
	private ProjectFactory createFactory;
	//TODO: needs constructor.
	
	public ProjectManager(MongoClient client, EventBus eb)
	{
		this.client = client;
		String projectInstanceCollection = SoileConfigLoader.getdbProperty("projectCollection");	
		this.dbFactory = new DBProjectFactory(client, projectInstanceCollection, eb);
		this.createFactory = new JsonToDBProjectFactory(client, projectInstanceCollection, eb);
		
	}

	public ProjectManager(MongoClient client, ProjectFactory createFactory, ProjectFactory dbFactory)
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
	 * Create a new Procject with empty information provide it to the Handler
	 * database.
	 * @param handler the handler to handle the created participant
	 */
	public void createProject(String projectID, String projectVersion, Handler<AsyncResult<ProjectInstance>> handler)
	{
		handler.handle(createProject(projectID,projectVersion));
	}

	/**
	 * Create a new Project with empty information and retrieve a new ID from the 
	 * database.
	 * @param handler the handler to handle the created participant
	 */
	public Future<ProjectInstance> createProject(String projectID, String projectVersion )
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
							   .put("participants", new JsonArray());
	}
	
	
	public Future<JsonObject> save(ProjectInstance proj)
	{
		return proj.save();
	}
}