package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.DataParticipant;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
/**
 * This is a Project stored in a (most likely git) Database. 
 * @author thomas
 *
 */
public class DBProjectInstance extends ProjectInstance{
	
	MongoClient client;
	EventBus eb;
	ElementManager<Project> projectManager;	
	static final Logger LOGGER = LogManager.getLogger(DBProjectInstance.class);

	public DBProjectInstance(ElementManager<Project> projManager, MongoClient client, EventBus eb) {
		super();
		this.projectManager = projManager;
		this.client = client;
		this.eb = eb;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Future<JsonObject> save() {
		Promise<JsonObject> saveSuccess = Promise.<JsonObject>promise();
		//TODO: If we at some point allow later modification of Name and shortcut, we need to add checks here that they do not conflict
		// For an implementation have a look at @ElementToDBProjectInstance
		JsonObject query = new JsonObject().put("_id", instanceID);		
		// remove id and participants, these are handled directly.		
		JsonObject update = toDBJson();
		update.remove("_id");		

		client.updateCollection(getTargetCollection(), query, update).onSuccess(result ->
		{			
			saveSuccess.complete(toDBJson());					
		}).onFailure(fail ->{
			saveSuccess.fail(fail);
		});		
		return saveSuccess.future();		
	}

	/**
	 * This loader expects that the input json is a query for the mongo db of instances.
	 * It will then extract the remaining data first from the Project Instance database and then from the git repository.
	 */
	@Override
	public Future<JsonObject> load(JsonObject instanceInfo) {
		Promise<JsonObject> loadSuccess = Promise.<JsonObject>promise();
		LOGGER.debug("Trying to load from DB: \n" + instanceInfo.encodePrettily());		
		client.findOne(getTargetCollection(), instanceInfo, null).onSuccess(instanceJson -> {
			LOGGER.debug("Found an entry");
			if(instanceJson == null)
			{				
				loadSuccess.fail(new ObjectDoesNotExist(instanceID));
			}
			else
			{		
				LOGGER.debug(instanceJson.encodePrettily());									
				projectManager.getGitJson(instanceJson.getString("sourceUUID"),instanceJson.getString("version"))
				.onSuccess(projectData -> 
				{
					LOGGER.debug("The data from the project git file is: \n" + projectData.encodePrettily());
					// we got a positive reply.
					instanceJson.mergeIn(projectData);
					loadSuccess.complete(instanceJson);					
				}
				).onFailure(fail -> {
					LOGGER.debug("Loading git File failed");	
					loadSuccess.fail(fail);
				});
			}
		}).onFailure(fail -> {
			loadSuccess.fail(fail);
		});
		return loadSuccess.future();		
	}

	@Override
	public Future<JsonObject> delete() {
		JsonObject query = new JsonObject().put("_id", this.instanceID);
		return client.findOneAndDelete(getTargetCollection(), query);		
	}
	
	
	/**
	 * Add a participant to the list of participants of this projects
	 * @param p the participant to add
	 */
	@Override
	public synchronized Future<Boolean> addParticipant(Participant p)
	{
		Promise<Boolean> updatePromise = Promise.promise();
		JsonObject update = new JsonObject().put("$push", new JsonObject().put("participants",p.getID()));
		client.updateCollection(getTargetCollection(), new JsonObject().put("_id", instanceID), update )
		.onSuccess(res -> {
			if(res.getDocModified() != 1)
			{
				LOGGER.error("Modified multiple object while only one ID was provided! Project was: " + instanceID );
				updatePromise.fail("Mongo Error");
				
			}
			else
			{
				participants.add(p.getID());		
				updatePromise.complete(true);
			}
		})
		.onFailure(err -> updatePromise.fail(err));		
		return updatePromise.future();
	}
	
	/**
	 * Delete a participant from the list of participants of this projects
	 * @param p the participant to remove
	 */
	@Override
	public synchronized Future<Boolean> deleteParticipant(Participant p)
	{
		Promise<Boolean> updatePromise = Promise.promise();
		JsonObject update = new JsonObject().put("$pull", new JsonObject().put("participants",p.getID()));
		client.updateCollection(getTargetCollection(), new JsonObject().put("_id", instanceID), update )
		.onSuccess(res -> {
			if(res.getDocModified() != 1)
			{
				LOGGER.error("Modified multiple objects while only one ID was provided! Project was: " + instanceID );
				updatePromise.fail("Mongo Error");
				
			}
			else
			{
				participants.remove(p.getID());		
				updatePromise.complete(true);
			}
		})
		.onFailure(err -> updatePromise.fail(err));		
		return updatePromise.future();
	}

	
	@Override
	public synchronized Future<JsonArray> getParticipants()
	{
		Promise<JsonArray> listPromise = Promise.promise();
		
		client.findOne(getTargetCollection(), new JsonObject().put("_id", instanceID), new JsonObject().put("participants", 1) )
		.onSuccess(res -> {								
				listPromise.complete(res.getJsonArray("participants"));
		})
		.onFailure(err -> listPromise.fail(err));		
		return listPromise.future();		
	}
	

}
