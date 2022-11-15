package fi.abo.kogni.soile2.project.participant;

import java.util.NoSuchElementException;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.instance.impl.ProjectManager;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class provides functionalities to interact with the Participant db, creating, saving and deleting participants.
 * @author Thomas Pfau
 *
 */
public class ParticipantManager implements DataRetriever<String, Participant> {

	MongoClient client;
	ProjectManager projManager;
	// should go to the constructor.	
	private String collectionId = SoileConfigLoader.getdbProperty("participantCollection");	
	private ParticipantFactory factory;
	//TODO: needs constructor.
	
	@Override
	public Future<Participant> getElement(String key) {
		Promise<Participant> participantPromise = Promise.<Participant>promise();
		JsonObject query = new JsonObject().put("_id", key);
		client.find(collectionId, query).onSuccess(res ->
		{
			//There should be only one
			if(res.size() != 1)
			{
				if(res.size() == 0)
				{
					participantPromise.fail(new NoSuchElementException());
				}
				else
				{
					participantPromise.fail(new DuplicateUserEntryInDBException(key));
				}
			}
			else
			{
				JsonObject partObject = res.get(0);
				String participantProject = partObject.getString("project");
				projManager.getElement(participantProject).onSuccess( project ->
				{
					Participant p = factory.createParticipant(partObject, project);										
					participantPromise.complete(p);
				}).onFailure(fail ->{
					participantPromise.fail(fail);
				});
			}
		}).onFailure(err -> 
		{
			participantPromise.fail(err.getCause());
		});			
		return participantPromise.future();
	}

	@Override
	public void getElement(String key, Handler<AsyncResult<Participant>> handler) {
		// TODO Auto-generated method stub
		handler.handle(getElement(key));
	}

	/**
	 * Create a new Participant with empty information and retrieve a new ID from the 
	 * database.
	 * @param handler the handler to handle the created participant
	 */
	public void createParticipant(ProjectInstance p,  Handler<AsyncResult<Participant>> handler)
	{
		handler.handle(createParticipant(p));
	}

	/**
	 * Create a new Participant with empty information and retrieve a new ID from the 
	 * database.
	 * @param handler the handler to handle the created participant
	 */
	public Future<Participant> createParticipant(ProjectInstance p)
	{	
		Promise<Participant> participantPromise = Promise.<Participant>promise();
		JsonObject defaultParticipant = getDefaultParticipantInfo(); 
		client.save(collectionId,defaultParticipant).onSuccess(res ->
		{
			defaultParticipant.put("_id", res);
			participantPromise.complete(factory.createParticipant(defaultParticipant, p));
		}).onFailure(err -> {
			participantPromise.fail(err.getCause());
		});
		return participantPromise.future();
	}
	
	
	/**
	 * Delete a participant and all associated data from the database
	 * @param id
	 * @param handler
	 */
	public void deleteParticipant(String id, Handler<AsyncResult<JsonObject>> handler)
	{		
		handler.handle(client.findOneAndDelete(collectionId, new JsonObject().put("_id", id)));
	}
	
	/**
	 * Default schema of a Participant.
	 * @return an empty participant information {@link JsonObject}
	 */
	public static JsonObject getDefaultParticipantInfo()
	{
		return new JsonObject().put("position","")
							   .put("finished", new JsonArray())
							   .put("outputData", new JsonObject())
							   .put("resultData", new JsonObject());
	}
	
	
	public Future<String> save(Participant part)
	{
		JsonObject update = part.toJson();		
		//TODO Correct this call. Needs to be upsert. 
		return client.save(SoileConfigLoader.getdbProperty("participantCollection"),update);
				
	}
}
