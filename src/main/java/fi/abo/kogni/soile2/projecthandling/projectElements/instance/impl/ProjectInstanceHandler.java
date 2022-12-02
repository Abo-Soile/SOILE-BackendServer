package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.participant.impl.DBParticipant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class ProjectInstanceHandler {

	static final Logger LOGGER = LogManager.getLogger(ProjectInstanceHandler.class);

	private ParticipantHandler participants;
	private TimeStampedMap<String, ProjectInstance> projects;
	private String dataLakeFolder;
	private ProjectInstanceManager manager;	 
	/**
	 * Default constructor that sets up a Manager with DB connections.
	 * @param participants the participants handler to obtain participants
	 * @param dataLakeFolder The Folder where the dataLake for result files is located
	 * @param client the mongoclient for connecting to the mongo database
	 */
	public ProjectInstanceHandler(ParticipantHandler participants, String dataLakeFolder,
			MongoClient client, EventBus eb) {
		super();
		this.participants = participants;
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
	public ProjectInstanceHandler(ParticipantHandler participants, String dataLakeFolder,
			MongoClient client, ProjectInstanceManager manager) {
		super();
		this.participants = participants;
		this.dataLakeFolder = dataLakeFolder;
		this.manager = manager;
		projects = new TimeStampedMap<String, ProjectInstance>(manager, 1000*60*60);
	}

	
	/**
	 * Get a list of all Files associated with the specified {@link DBParticipant} within this {@link ProjectInstance}.
	 * @param p the {@link DBParticipant} for which to retrieve the file results.
	 * @return
	 */
	public Set<File> getFilesForParticipant(Participant p)
	{
		HashSet<File> fileSet = new HashSet<File>();
		for(TaskFileResult res : p.getFileResults())
		{
			try {
				fileSet.add(res.getFile(dataLakeFolder));
			}
			catch(FileNotFoundException e)
			{
				//TODO: properly handle this.
				// if a file can't be found, that's fine it seems to already have been deleted.
				continue;
			}
		}
		return fileSet;
	}
	
	/**
	 * Get the folders for a specific participant
	 * @param p the participant for which to retrieve the folders within this project.
	 * @return a set of Files representing the folders for this participant, the folders have been checked for existance at time of generation.
	 */
	public Set<File> getFoldersForParticipant(Participant p)
	{
		HashSet<File> fileSet = new HashSet<File>();
		for(String taskID : p.getTasksWithFiles())
		{
			File folder = new File(dataLakeFolder + File.separator +  taskID + File.separator + p.getID());
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
	public Future<Participant> getParticipant(String projectID, String participantID)
	{
		
		Promise<Participant> participantPromise = Promise.<Participant>promise();
				
		// if no ID is provided, create a new Participant for the project handled by this handler and add that participant to the list.		
		if(participantID == null)
		{
			projects.getData(projectID).onSuccess(targetProject -> 
			{		
				participants.create(targetProject).onSuccess(participant ->
				{
					targetProject.addParticipant(participant);					
					participantPromise.complete(participant);
				}).onFailure(fail -> {
					participantPromise.fail(fail);
				});
			}).onFailure(fail -> {
				participantPromise.fail(fail);	
			});
		}
		else
		{
			participants.getParticpant(participantID).onSuccess(participant -> {
				participantPromise.complete(participant);
			}).onFailure(fail -> {
				participantPromise.fail(fail);
			});
		}
		return participantPromise.future();
	}
	
	/**
	 * Get the participant for the given id. if id is null, a new participant will be created.
	 * @param id The id of the participant, or null if a new participant needs to be created.
	 * @param handler the handler that handles the created participant.
	 */
	public Future<ProjectInstance> createProject(JsonObject projectInformation)	
	{
		return manager.startProject(projectInformation);
	}
	
	
}
