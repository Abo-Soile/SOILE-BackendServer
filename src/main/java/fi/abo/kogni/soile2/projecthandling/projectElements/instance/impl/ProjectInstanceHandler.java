package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.DataParticipant;
import fi.abo.kogni.soile2.projecthandling.participant.impl.DBParticipant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

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
	public ProjectInstanceHandler(String dataLakeFolder,
			MongoClient client, EventBus eb) {
		super();
		this.dataLakeFolder = dataLakeFolder;
		this.manager = new ProjectInstanceManager(client, eb);
		projects = new TimeStampedMap<String, ProjectInstance>(manager, 1000*60*60);
	}

	/**
	 * Default constructor that sets up a Manager with DB connections.
	 * @param participants the participants handler to obtain participants
	 * @param dataLakeFolder The Folder where the dataLake for result files is located
	 * @param client the mongoclient for connecting to the mongo database
	 * @param manager a custom project Manager.
	 */
	public ProjectInstanceHandler( String dataLakeFolder,
			MongoClient client, ProjectInstanceManager manager) {
		super();
		this.dataLakeFolder = dataLakeFolder;
		this.manager = manager;
		projects = new TimeStampedMap<String, ProjectInstance>(manager, 1000*60*60);
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
	 * Get the folders for a specific participant
	 * @param p the participant for which to retrieve the folders within this project.
	 * @return a set of Files representing the folders for this participant, the folders have been checked for existance at time of generation.
	 */
	public Set<File> getTaskFoldersForParticipant(Set<String> tasks, String participantID)
	{
		HashSet<File> fileSet = new HashSet<File>();
		for(String taskID : tasks)
		{
			File folder = new File(dataLakeFolder + File.separator +  taskID + File.separator + participantID);
			if(folder.exists())
			{
				fileSet.add(folder);
			}			
		}
		return fileSet;
	}
	
	/**
	 * Get the participant for the given id. if id is null, a new participant will be created.
	 * @param id The id of the participant, or null if a new participant needs to be created.
	 * @param handler the handler that handles the created participant.
	 */
	public Future<Void> addParticipant(String projectInstanceID, Participant p)
	{
		
		Promise<Void> addPromise = Promise.<Void>promise();
				
		// if no ID is provided, create a new Participant for the indicated project and add that participant to the list.		
		projects.getData(projectInstanceID).onSuccess(targetProject -> 
		{	
			
			targetProject.addParticipant(p)
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
	 * 2. "Version" of the project from which this was started
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
	 * Get a list of project instances.
	 */
	public Future<JsonArray> getProjectList(JsonArray Permissions)
	{
		return manager.getProjectInstanceStatus(Permissions);
	}
	
	/**
	 * 
	 */
	public Future<JsonObject> getAvailableData(ProjectInstance instance)
	{
		Promise<JsonObject> dataPromise = Promise.promise();
		
		
		return dataPromise.future();
	}
}
