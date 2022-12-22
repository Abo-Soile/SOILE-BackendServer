package fi.abo.kogni.soile2.projecthandling.participant;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fi.abo.kogni.soile2.datamanagement.utils.CheckDirtyMap;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
/**
 * The participantHandler keeps track of all participants. 
 * It stores 
 * @author Thomas Pfau
 *
 */
public class ParticipantHandler {
	MongoClient client;
	ProjectInstanceHandler project;
	CheckDirtyMap<String,Participant> activeparticipants;
	
	ParticipantManager manager;
	Vertx vertx;
	
	
	
	
	public ParticipantHandler(MongoClient client, ProjectInstanceHandler project, Vertx vertx) {
		super();		
		this.client = client;
		this.project = project;
		this.manager = new ParticipantManager(client);
		this.vertx = vertx;
		activeparticipants = new CheckDirtyMap<String, Participant>(manager, 2*3600); //Keep for two hours
	}
	/**
	 * Create a participant in the database, store that  and let the handler handle it
	 * @param handler
	 */
	public void create(ProjectInstance p, Handler<AsyncResult<Participant>> handler)
	{
		handler.handle(create(p));			
	}
	
	/**
	 * Create a participant in the database
	 * @param handler
	 */
	public Future<Participant> create(ProjectInstance p)
	{
		return manager.createParticipant(p);
	}
	
	
	/**
	 * Clean up the data currently stored by this Participant handler. 
	 * This is necessary to avoid excessive data in memory.
	 */
	public void cleanup()
	{
		Collection<Participant> partsToClean = activeparticipants.cleanup();
	}
	
	/**
	 * Retrieve a participant from the database (or memory) and return the participant
	 * based on the participants uID.
	 * @param id the uid of the participant
	 * @param handler the handler that requested the participant.
	 */
	public void getParticpant(String id, Handler<AsyncResult<Participant>> handler)
	{
		activeparticipants.getData(id, handler);
	}
	
	/**
	 * Retrieve a participant from the database (or memory) and return the participant
	 * based on the participants uID.
	 * @param id the uid of the participant
	 * @param handler the handler that requested the participant.
	 */
	public Future<Participant> getParticpant(String id)
	{
		return activeparticipants.getData(id);		
	}
	
	/**
	 * Create a participant for a project with a given instanceID. 
	 * @param id the uid of the participant
	 * @param handler the handler that requested the participant.
	 */
	public Future<Participant> createParticipant(String projectInstanceID)
	{
		Promise<Participant> particpantPromise = Promise.promise();
		project.loadProject(projectInstanceID)
		.onSuccess( projectInstance -> {
			manager.createParticipant(projectInstance)
			.onSuccess(participant -> particpantPromise.complete(participant))
			.onFailure(err -> particpantPromise.fail(err));
		})
		.onFailure(err -> particpantPromise.fail(err));
		return particpantPromise.future();

	}

	
	/**
	 * Delete a participant and all data associated with the participant from the project.
	 * @param id
	 */
	public Future<Void> deleteParticipant(String id)
	{
		Promise<Void> deletionPromise = Promise.<Void>promise();
		manager.getParticipantResults(id)
		.onSuccess(resultJson-> 
		{			
			List<Future> deletionFutures = new LinkedList<Future>();
			for(File f : project.getFilesinProject(manager.getFilesFromResults(resultJson)))
			{
				deletionFutures.add(vertx.fileSystem().delete(f.getAbsolutePath()));
			}
			CompositeFuture.all(deletionFutures).onFailure(failure ->
			{
				deletionPromise.fail(failure.getCause());
			}).onSuccess(success ->
			{
				List<Future> deletedFolders = new LinkedList<Future>();
				for(File f : project.getTaskFoldersForParticipant(manager.getTaskWithFilesFromResults(resultJson), id))
				{
					deletedFolders.add(vertx.fileSystem().delete(f.getAbsolutePath()));
				}
				CompositeFuture.all(deletedFolders)
				.onSuccess(deltionDone ->{
					// now, all files and folders have been removed. So we will delete the participant ID.
					manager.deleteParticipant(id)
					.onSuccess(Void -> {
						deletionPromise.complete();
					})
					.onFailure(err -> deletionPromise.fail(err));
				})
				.onFailure(err -> deletionPromise.fail(err));
			})
			.onFailure(err -> deletionPromise.fail(err));			
		})
		.onFailure(err -> deletionPromise.fail(err));
		return deletionPromise.future();
	}
		
	
	/**
	 * Get a {@link JsonArray} of {@link JsonObject} elements that contain the 
	 * @param project
	 * @return
	 */
	public Future<JsonArray> getParticipantStatusForProject(ProjectInstance project)
	{
		return manager.getParticipantStatusForProject(project);
	}

	/**
	 * Get a {@link JsonArray} of {@link JsonObject} elements that contain the 
	 * @param project
	 * @return
	 */
	public Future<List<JsonObject>> getTaskDataforParticipants(JsonArray participantIDs, String taskID, String projectID)
	{
		return manager.getParticipantsResultsForTask(participantIDs, projectID, taskID);
	}
	
	/**
	 * Get a {@link List} of {@link JsonObject} elements that contain the results along with some additional information for each participant. 
	 * @param project
	 * @return
	 */
	public Future<List<JsonObject>> getParticipantData(ProjectInstance project, JsonArray particpantIDs)
	{
		Promise<List<JsonObject>> dataPromise = Promise.<List<JsonObject>>promise();
		if(particpantIDs == null)
		{// this is a request for all Participants, so just collect the data.
			project.getParticipants().onSuccess(participants -> 
			{				
				getParticipantData(project, participants)
				.onSuccess(res -> 
				{
					dataPromise.complete(res);
				})
				.onFailure(err -> dataPromise.fail(err));
			})
			.onFailure(err -> dataPromise.fail(err));
					
		}
		else
		{
			manager.getParticipantsResults(particpantIDs, project.getID())
			.onSuccess( res -> {
				dataPromise.complete(res);
			})
			.onFailure(err -> dataPromise.fail(err));
			
		}
		return dataPromise.future();
	}
}
