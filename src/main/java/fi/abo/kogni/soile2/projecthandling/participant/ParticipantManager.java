package fi.abo.kogni.soile2.projecthandling.participant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import fi.abo.kogni.soile2.datamanagement.utils.DirtyDataRetriever;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.projecthandling.participant.impl.DBParticipantFactory;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskFileResult;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class provides functionalities to interact with the Participant db, creating, saving and deleting participants.
 * @author Thomas Pfau
 *
 */
public class ParticipantManager implements DirtyDataRetriever<String, Participant> {

	MongoClient client;
	// should go to the constructor.	
	private String collectionId = SoileConfigLoader.getdbProperty("participantCollection");	
	private ParticipantFactory factory;
	private HashMap<String, Long> dirtyTimeStamps; 
	//TODO: needs constructor.
	private String participantCollection = SoileConfigLoader.getdbProperty("participantCollection");
	private Vertx vertx;
	
	public ParticipantManager(MongoClient client)
	{
		this.client  = client;
		this.factory = new DBParticipantFactory(this);
		dirtyTimeStamps = new HashMap<>();
	}
	
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
				Participant p = factory.createParticipant(partObject);
				dirtyTimeStamps.put(key,System.currentTimeMillis());
				participantPromise.complete(p);				
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
			Participant part = factory.createParticipant(defaultParticipant);
			p.addParticipant(part).onSuccess( Void -> 
			{
					participantPromise.complete(part);
			})
			.onFailure(err -> participantPromise.fail(err));
			
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
	 * Delete a participant from the participant database. 
	 * This does NOT remove other data associated with the participant.
	 * @param id
	 * @return 
	 */
	public Future<Void> deleteParticipant(String id)
	{				
		return client.findOneAndDelete(collectionId, new JsonObject().put("_id", id)).mapEmpty();
	}
	/**
	 * Get The files stored for a specific participant. 
	 * @param resultJson a {@link JsonObject} that contains at least a "resultData" array from a participant along with the "_id" field.
	 * @return 
	 */
	public Set<TaskFileResult> getFilesFromResults(JsonObject resultJson)
	{
		
		Set<TaskFileResult> results = new HashSet<>();
		JsonArray resultData = resultJson.getJsonArray("resultData", new JsonArray());
		for(int i = 0; i < resultData.size(); i++)
		{
			JsonObject taskResults = resultData.getJsonObject(i);
			JsonArray fileResults = taskResults.getJsonArray("fileData", new JsonArray());
			for(int j = 0; i < fileResults.size(); j++)
			{
				JsonObject fileResult = fileResults.getJsonObject(j);				
				results.add(new TaskFileResult(fileResult.getString("filename"),
											   fileResult.getString("targetid"),
											   fileResult.getString("fileformat"),
											   taskResults.getString("task"), 
											   resultJson.getString("_id")));
			}
		}
		return results;
	}
	
	/**
	 * Get a list of dbResults for the given participants. 
	 * @param participantIDs
	 * @return A fture of the list of JsonResults.
	 */
	public Future<List<JsonObject>> getDataBaseResultsForParticipants(List<String> participantIDs, boolean finished)
	{
		JsonObject matchObj = new JsonObject().put("_id", new JsonArray(participantIDs));
		if(finished)
		{
			matchObj.put("finished", true);
		}
		FindOptions options = new FindOptions();
		options.setFields(new JsonObject().put("_id", 1).put("resultData", new JsonObject().put("task", 1).put("dbData", 1)));
		return client.findWithOptions(collectionId, matchObj,options);
	}
	
	/**
	 * Get the tasks from a result object that contain files
	 * @param resultJson a {@link JsonObject} that contains at least a "resultData" array from a participant.  
	 * @return 
	 */
	public Set<String> getTaskWithFilesFromResults(JsonObject resultJson)
	{
		
		Set<String> results = new HashSet<>();
		JsonArray resultData = resultJson.getJsonArray("resultData", new JsonArray());
		for(int i = 0; i < resultData.size(); i++)
		{
			JsonObject taskResults = resultData.getJsonObject(i);
			JsonArray fileResults = taskResults.getJsonArray("fileData", new JsonArray());
			if(fileResults.size() > 0)
			{
				results.add(taskResults.getString("task"));
			}
		}
		return results;
	}
	
	
	/**
	 * Get The files stored for a specific participant. 
	 * @param id the ID of the participant.
	 * @return 
	 */
	public Future<JsonObject> getParticipantResults(String participantID)
	{		
		return client.findOne(collectionId, new JsonObject().put("_id", participantID), new JsonObject().put("resultData", 1).put("_id", 1));
	}
	
	
	/**
	 * Default schema of a Participant.
	 * @return an empty participant information {@link JsonObject}
	 */
	public static JsonObject getDefaultParticipantInfo()
	{
		return new JsonObject().put("position","")
							   .put("finished", false)							   
							   .put("outputData", new JsonArray())
							   .put("modifiedStamp", System.currentTimeMillis())
							   .put("resultData", new JsonArray());
		
	}
	
	
	public Future<String> save(Participant p)
	{
		//TODO: Possibly we need to fix this in some way to avoid concurrent handling 
		// of participants, but 
		Promise<String> savePromise = Promise.<String>promise();
		p.toJson()
		.onSuccess(update -> {
			update.put("modifiedStamp", System.currentTimeMillis());
			
			//TODO Correct this call. Needs to be upsert. 
			client.save(participantCollection,update)
			.onSuccess(res -> {
				savePromise.complete(res);
			})
			.onFailure(err -> savePromise.fail(err));
		})
		.onFailure(err -> savePromise.fail(err));				
		return savePromise.future();
	}
	
	
	@Override
	public Future<Participant> getElementIfDirty(String key) {
		Promise<Participant> dirtyPromise = Promise.promise();
		client.findOne(collectionId,  new JsonObject().put("_id", key), new JsonObject().put("modifiedStamp",1))
		.onSuccess(res -> {
			if(dirtyTimeStamps.get(key) >= res.getLong("modifiedStamp"))
			{
				dirtyPromise.complete(null);
			}
			else
			{
				getElement(key).onSuccess(part ->
				{
							dirtyPromise.complete(part);
				})
				.onFailure(err -> dirtyPromise.fail(err));
			}
		})
		.onFailure(err -> dirtyPromise.fail(err));
		return dirtyPromise.future();
	}

	@Override
	public void getElementIfDirty(String key, Handler<AsyncResult<Participant>> handler) {
		handler.handle(getElementIfDirty(key));		
	}
	
	
	/**
	 * Update the results for the given task. 
	 * @param taskID the Task to be updated
	 * @param results the results { an element of the resultData array with (or without) the task ID}
	 * @return
	 */
	public Future<Void> updateResults(Participant p, int step, String taskID, JsonObject results)
	{
		// we will pull whatever data was set in the results for this element.
		// and put in what we just got.
		JsonObject pullUpdate = new JsonObject().put("$pull", new JsonObject()
																  .put("resultData", new JsonObject()
																		  				 .put("$and", new JsonArray()
																		  						 		  .add( new JsonObject()
																		  						 				  	.put("task", taskID)
																		  						 				  	.put("step", step)))));
		JsonObject pushUpdate = new JsonObject().put("$push", new JsonObject()
				  .put("resultData", new JsonObject().put("task", taskID).put("step", step).mergeIn(results)));
		JsonObject setUpdate = new JsonObject().put("$set", new JsonObject()
				  .put("modifiedStamp", System.currentTimeMillis()));		
		JsonObject itemQuery = new JsonObject().put("_id", p.getID());
		List<BulkOperation> pullAndPut = new LinkedList<>();
		BulkOperation pullOp = BulkOperation.createUpdate(itemQuery, pullUpdate);
		BulkOperation pushOp = BulkOperation.createUpdate(itemQuery, pushUpdate);
		BulkOperation setOp = BulkOperation.createUpdate(itemQuery, setUpdate);
		pullAndPut.add(pullOp);
		pullAndPut.add(pushOp);
		pullAndPut.add(setOp);

		return client.bulkWrite(participantCollection, pullAndPut).mapEmpty();
	}
}
