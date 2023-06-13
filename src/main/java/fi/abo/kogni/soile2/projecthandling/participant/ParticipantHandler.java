package fi.abo.kogni.soile2.projecthandling.participant;

import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.utils.CheckDirtyMap;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
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
	StudyHandler studyHandler;
	CheckDirtyMap<String,Participant> activeparticipants;	
	ParticipantManager manager;
	Vertx vertx;
	String dataLakeFolder;
	static final Logger LOGGER = LogManager.getLogger(ParticipantHandler.class);




	public ParticipantHandler(MongoClient client, StudyHandler project, Vertx vertx) {
		super();		
		this.client = client;
		this.studyHandler = project;
		this.manager = new ParticipantManager(client);
		this.vertx = vertx;
		activeparticipants = new CheckDirtyMap<String, Participant>(manager, 2*3600); //Keep for two hours
		dataLakeFolder = SoileConfigLoader.getServerProperty("soileResultDirectory");
	}
	/**
	 * Create a participant in the database, store that  and let the handler handle it
	 * @param handler
	 */
	public void create(Study p, Handler<AsyncResult<Participant>> handler)
	{
		handler.handle(create(p));			
	}

	/**
	 * Create a participant in the database
	 * @param p the {@link Study} for which to create a participant
	 */
	public Future<Participant> create(Study p)
	{
		return manager.createParticipant(p);
	}

	/**
	 * Create a participant in the database
	 * @param p the {@link Study} for which to create a participant
	 * 
	 */
	public Future<Participant> createTokenParticipant(Study p, String usedToken)
	{
		return manager.createTokenParticipant(p, usedToken);
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
	 * @param token the token to use for the {@link Participant}
	 * @param studyID the ID of the {@link Study} to create a {@link Participant} in
	 * @return a {@link Future} of the {@link Participant}
	 */
	public Future<Participant> getParticipantForToken(String token, String studyID)
	{
		Promise<Participant> partPromise = Promise.<Participant>promise();
		manager.getParticipantIDForToken(token, studyID)
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
	 * Create a new participant for a study with a given instanceID. 
	 * @param studyID the uuid of the study to create a participant in
	 * @return a {@link Future} of the {@link Participant}
	 */
	public Future<Participant> createParticipant(String studyID)
	{
		Promise<Participant> particpantPromise = Promise.promise();
		studyHandler.loadUpToDateStudy(studyID)
		.onSuccess( study -> {
			manager.createParticipant(study)
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
	public Future<Void> deleteParticipant(String id, boolean participantHasToBeInStudy)
	{
		Promise<Void> deletionPromise = Promise.<Void>promise();
		// all Files for a participant are stored in the folder: datalake/PARTICIPANTID
		getParticipant(id)
		.onSuccess( participant -> {

			vertx.fileSystem().exists(Path.of(dataLakeFolder, id).toString())
			.onSuccess(deleteFiles -> {

				if(deleteFiles)
				{
					vertx.fileSystem().deleteRecursive(Path.of(dataLakeFolder, id).toString(), true)
					.onSuccess(filesDeleted -> 			
					{
						//TODO: Need to change this, so that it is FIRST removed from the projectInstance and THEN deleted from the participant db.... 
						removeParticipantFromManager(participant, id, participantHasToBeInStudy)
						.onSuccess(done -> deletionPromise.complete())
						.onFailure(err -> deletionPromise.fail(err));
						// now, all files and folders have been removed. So we will delete the participant ID.

					})
					.onFailure(
					err -> {						
						LOGGER.error("Error while deleting files for participant " + id + "!");
						LOGGER.error(err);
						deletionPromise.fail(err);	
					});
				}
				else
				{
					removeParticipantFromManager(participant, id, participantHasToBeInStudy)
					.onSuccess(done -> deletionPromise.complete())
					.onFailure(err -> deletionPromise.fail(err));
				}
			}).onFailure(
			err -> {						
				LOGGER.error("Error while determining if files exist!");
				LOGGER.error(err);
				deletionPromise.fail(err);	
			});
		})
		.onFailure(err -> deletionPromise.fail(err));			
		return deletionPromise.future();
	}

	private Future<Void> removeParticipantFromManager(Participant participant, String id, boolean participantHasToBeInStudy)
	{
		Promise<Void> removedPromise = Promise.promise();
		studyHandler.removeParticipant(participant.getProjectID(), participant, participantHasToBeInStudy)
		.onSuccess( success -> {
			manager.deleteParticipant(id)
			.onSuccess(deletionSuccess -> {

				activeparticipants.cleanElement(id);
				removedPromise.complete();
			}).onFailure(err -> {
				LOGGER.error("Error while deleting participant " + id + ". Couldnt remove from the participant database!");
				LOGGER.error(err);
				removedPromise.fail(err);	
			});										
		})
		.onFailure(err -> {
			LOGGER.error("Error while deleting participant " + id + ". Couldnt remove from the Project !");
			LOGGER.error(err);
			removedPromise.fail(err);	
		});
		return removedPromise.future();
	}

	/**
	 * Get a {@link JsonArray} of {@link JsonObject} elements that contain the 
	 * @param project
	 * @return
	 */
	public Future<JsonArray> getParticipantStatusForProject(Study project)
	{
		return manager.getParticipantStatusForProject(project);
	}

	/**
	 * Get a {@link JsonArray} of {@link JsonObject} elements that contain the 
	 * @param studyHandler
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
	public Future<List<JsonObject>> getParticipantData(Study project, JsonArray particpantIDs)
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
