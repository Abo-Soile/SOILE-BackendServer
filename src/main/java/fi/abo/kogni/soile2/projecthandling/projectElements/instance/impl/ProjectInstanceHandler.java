package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.impl.DBParticipant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * Handler for Project instances.
 * @author Thomas Pfau
 *
 */
public class ProjectInstanceHandler {

	static final Logger LOGGER = LogManager.getLogger(ProjectInstanceHandler.class);

	private TimeStampedMap<String, ProjectInstance> projects;
	private String dataLakeFolder;
	private ProjectInstanceManager manager;	 
	
	/**
	 * Default constructor that sets up a Manager with DB connections.
	 * @param participants the participants handler to obtain participants
	 * @param dataLakeFolder The Folder where the dataLake for result files is located
	 * @param client the mongoclient for connecting to the mongo database
	 */
	public ProjectInstanceHandler(MongoClient client, Vertx vertx) {
		this(client, new ProjectInstanceManager(client, vertx) );		
	}

	/**
	 * Default constructor that sets up a Manager with DB connections.
	 * @param participants the participants handler to obtain participants
	 * @param dataLakeFolder The Folder where the dataLake for result files is located
	 * @param client the mongoclient for connecting to the mongo database
	 * @param manager a custom project Manager.
	 */
	public ProjectInstanceHandler(MongoClient client, ProjectInstanceManager manager) {
		super();
		this.dataLakeFolder = SoileConfigLoader.getServerProperty("soileResultDirectory");
		this.manager = manager;
		projects = new TimeStampedMap<String, ProjectInstance>(manager, 1000*60*60);
	}

	/**
	 * Clean up the data in the Data Maps
	 */
	public void cleanup()
	{
		projects.cleanUp();
		manager.cleanUp();
	}
	/**
	 * Get a list of all Files associated with the specified {@link DBParticipant} within this {@link ProjectInstance}.
	 * @param p the {@link DBParticipant} for which to retrieve the file results.
	 * @return
	 */
	public Set<DataLakeFile> getFilesinProject(Set<TaskFileResult> fileResults)
	{
		HashSet<DataLakeFile> fileSet = new HashSet<DataLakeFile>();
		for(TaskFileResult res : fileResults)
		{
			fileSet.add(res.getFile(dataLakeFolder));
		}
		return fileSet;
	}
	
	/**
	 * Add the participant to the project with the given ID.
	 * @param projectInstanceID The id of the project to which to add the participant.
	 * @param participant The id of the participant to add to the project.
	 * @Return a Successfull future if the participant was added
	 */
	public Future<Void> addParticipant(String projectInstanceID, Participant participant)
	{
		
		Promise<Void> addPromise = Promise.<Void>promise();
				
		// if no ID is provided, create a new Participant for the indicated project and add that participant to the list.		
		projects.getData(projectInstanceID).onSuccess(targetProject -> 
		{	
			
			targetProject.addParticipant(participant)
			.onSuccess(success -> {
				addPromise.complete();
			})
			.onFailure(err -> addPromise.fail(err));
		}).onFailure(fail -> {
				addPromise.fail(fail);	
		});		
		return addPromise.future();
	}
	
	/**
	 * Remove the participant with the given id from the project.
	 * @param projectInstanceID The id of the project to which to add the participant.
	 * @param participant The participant to remove from the project.
	 * @Return a Successfull future if the participant was removed
	 */
	public Future<Void> removeParticipant(String projectInstanceID, Participant participant)
	{
		
		Promise<Void> addPromise = Promise.<Void>promise();
				
		// if no ID is provided, create a new Participant for the indicated project and add that participant to the list.		
		projects.getData(projectInstanceID).onSuccess(targetProject -> 
		{				
			targetProject.deleteParticipant(participant)
			.onSuccess(success -> {
				addPromise.complete();
			})
			.onFailure(err -> addPromise.fail(err));
		}).onFailure(fail -> {
				addPromise.fail(fail);	
		});		
		return addPromise.future();
	}
	
	
	/**
	 * Start a project with the given Project Information.
	 * The information must contain:
	 * 1. "UUID" of the project from which this was started
	 * 2. "version" of the project from which this was started
	 * 3. "private" field wrt access for this 
	 * 4. "name" a name field.
	 * 5. "shortcut" (optional), that can be used as a shortcut to the project.
	 * @param projectInformation The information needed to start this project.
	 */
	public Future<ProjectInstance> createProjectInstance(JsonObject projectInformation)	
	{
		LOGGER.debug("Trying to load Project instance");
		return manager.startProject(projectInformation);
	}
	
	/**
	 * Load a project with the given ID. 
	 * This can fail if the project does not exist.
	 * @param projectInstanceID the instance ID of the project to retrieve.
	 */
	public  Future<ProjectInstance> loadProject(String projectInstanceID)
	{
		return projects.getData(projectInstanceID);		
	}
	
	/**
	 * Get a list of project instances. based on the given Permissions
	 * This will return non-private projects and all projects listed in the permissions.
	 * @param Permissions  the permissions for the projects 
	 * @param permissionsOnly whether to only list those projects indicated by the permissions
	 * @return A {@link Future} of the {@link JsonArray} containing the projects (id and name)
	 */
	public Future<JsonArray> getProjectList(JsonArray Permissions, boolean permissionsOnly)
	{
		return manager.getProjectInstanceStatus(Permissions, permissionsOnly);
	}
	
	
	/**
	 * Get the project ID for a given Path-ID (i.e. translate potential shortcuts).  
	 * @param pathID the {id} path parameter  
	 * @return A future of the actual path.
	 */
	public Future<String> getProjectIDForPath(String pathID)
	{
		return manager.getProjectIDForPathID(pathID);
	}
	
}
