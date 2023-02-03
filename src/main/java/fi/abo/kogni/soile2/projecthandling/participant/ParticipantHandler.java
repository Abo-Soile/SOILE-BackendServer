package fi.abo.kogni.soile2.projecthandling.participant;

import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.utils.CheckDirtyMap;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
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
	String dataLakeFolder;
	static final Logger LOGGER = LogManager.getLogger(ParticipantHandler.class);

	
	
	
	public ParticipantHandler(MongoClient client, ProjectInstanceHandler project, Vertx vertx) {
		super();		
		this.client = client;
		this.project = project;
		this.manager = new ParticipantManager(client);
		this.vertx = vertx;
		activeparticipants = new CheckDirtyMap<String, Participant>(manager, 2*3600); //Keep for two hours
		dataLakeFolder = SoileConfigLoader.getServerProperty("soileResultDirectory");
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
	 * Create a participant in the database
	 * @param handler
	 */
	public Future<Participant> createTokenUser(ProjectInstance p)
	{
		return manager.createTokenParticipant(p);
	}
	
	/**
	 * Clean up the data currently stored by this Participant handler. 
	 * This is necessary to avoid excessive data in memory.
	 */
	public void cleanup()
	{
		activeparticipants.cleanup();
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
	public Future<Participant> getParticipant(String id)
	{
		return activeparticipants.getData(id);		
	}
	
	/**
	 * Retrieve a participant from the database (or memory) and return the participant
	 * based on the participants uID.
	 * @param id the uid of the participant
	 * @param handler the handler that requested the participant.
	 */
	public Future<Participant> getParticipantForToken(String token, String projectID)
	{
		Promise<Participant> partPromise = Promise.<Participant>promise();
		manager.getParticipantIDForToken(token, projectID)
		.onSuccess(id -> {
			getParticipant(id)
			.onSuccess(participant -> {
				partPromise.complete(participant);
			})
			.onFailure(err -> partPromise.fail(err));
		})
		.onFailure(err -> partPromise.fail(err));
		return partPromise.future();
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
		// all Files for a participant are stored in the folder: datalake/PARTICIPANTID
		getParticipant(id)
		.onSuccess( participant -> {
			vertx.fileSystem().deleteRecursive(Path.of(dataLakeFolder, id).toString(), true)
			.onSuccess(filesDeleted -> 			
			{
				//TODO: Need to change this, so that it is FIRST removed from the projectInstance and THEN deleted from the participant db.... 
				
				// now, all files and folders have been removed. So we will delete the participant ID.
				project.removeParticipant(participant.getProjectID(), participant)
				.onSuccess( success -> {
					manager.deleteParticipant(id)
					.onSuccess(deletionSuccess -> {
										
						activeparticipants.cleanElement(id);
						deletionPromise.complete();
					}).onFailure(err -> {
						LOGGER.error("Error while deleting participant " + id + ". Couldnt remove from the participant database!");
						LOGGER.error(err);
						deletionPromise.fail(err);	
					});										
				})
				.onFailure(err -> {
					LOGGER.error("Error while deleting participant " + id + ". Couldnt remove from the Project !");
					LOGGER.error(err);
					deletionPromise.fail(err);	
				});
			})
			.onFailure(
					err -> {
						LOGGER.error("Error while deleting files for participant " + id + "!");
						LOGGER.error(err);
						deletionPromise.fail(err);	
					});
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
