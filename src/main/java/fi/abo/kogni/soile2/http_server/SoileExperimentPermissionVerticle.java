package fi.abo.kogni.soile2.http_server;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;


/**
 * Permissions for Experiments are saved in the form of JsonArrays containing strings. 
 * The following Permissions rules are followed:
 * Static Data/Running the experiment (e.g. images etc): 
 *    if private, 
 *    - Participants only have (read)access to the files if they have explicit access to the experiment/questionaire
 *    - Users/Researchers have full access if they are owners (both read/write) 
 *    - Other researchers do not have access to the experiment/questionaire etc
 *    if public:
 *    - everyone has read access to the static data of the experiment and can run the experiment (if it is active). 
 *      
 * Source Data:
 *    - if private:
 *    	- Participants have no access. 
 *    	- Only Owners have access
 *    - if public:
 *    	- Participants have no access. 
 *      - Non-owners have read access (can create a copy for their use) 
 *      - Owners have full access
 * Result data :
 *    - Always only accessible to owners/collaborators.		   
 * @author Thomas Pfau
 *
 */
public class SoileExperimentPermissionVerticle extends SoileBaseVerticle{

	static final Logger LOGGER = LogManager.getLogger(SoileExperimentPermissionVerticle.class);
	MongoClient mongo;	
	JsonObject userConfig;
	private Map<RequestType,String> RequestToComFields;
	private Map<RequestType,String> RequestToDbFields;
	static enum RequestType
	{
		Collaborator,
		Participant,
		Owner,
		Private,		
		All
	}
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {		
		LOGGER.debug("Starting SoilePermissionVerticle");
		mongo = MongoClient.createShared(vertx, config().getJsonObject("db"));
		setupConfig(SoileConfigLoader.EXPERIMENT_CFG);
		userConfig = config().getJsonObject(SoileConfigLoader.USERMGR_CFG);
		init();
		setupEvents();
		startPromise.complete();		
		LOGGER.debug("Started successfully");
	}
	
	private void init()
	{
		RequestToComFields = new HashedMap<SoileExperimentPermissionVerticle.RequestType, String>();
		RequestToDbFields = new HashedMap<SoileExperimentPermissionVerticle.RequestType, String>();
		RequestToComFields.put(RequestType.Collaborator, SoileConfigLoader.getCommunicationField("collaboratorField"));
		RequestToComFields.put(RequestType.Owner, SoileConfigLoader.getCommunicationField("ownerField"));
		RequestToComFields.put(RequestType.Participant, SoileConfigLoader.getCommunicationField("participantField"));
		RequestToComFields.put(RequestType.Private, SoileConfigLoader.getCommunicationField("logonRequireField"));
		RequestToDbFields.put(RequestType.Collaborator, SoileConfigLoader.getdbField("collaboratorField"));
		RequestToDbFields.put(RequestType.Owner, SoileConfigLoader.getdbField("ownerField"));
		RequestToDbFields.put(RequestType.Participant, SoileConfigLoader.getdbField("participantField"));
		RequestToComFields.put(RequestType.Private, SoileConfigLoader.getdbField("privateField"));
	}
	
	/**
	 * Set up eventbus consumers. 
	 * Request Consumers expect a JsonObject with the following structure, returning structures containing the requested data (see e.g. :
	 * {
	 * 	"<IDField>" : "ObjectID" 
	 * }
	 * Setting permissions Commands contain the structure:
	 * {
	 *  "<IDField>" : "ObjectID",
	 *  "<operationField>" : "<addCommand>"/"<setCommand>"/"<removeCommand>",
	 *  "<ownerField>" : [ "ChangedOwner1", "ChangedOwner2"...]
	 * }
	 * Setting the private flag is done by:
	 * {
	 *  "<IDField>" : "ObjectID"
	 *  "<logonRequiredField"> : true/false
	 * }
	 * No individual Permissions can be set for an experiment. That depends on whether a user is owner or not and whether the object is private or not.
	 */
	
	void setupEvents()
	{
		
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"changeOwner"), this::setOwners);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"changeCollaborators"), this::setCollaborators);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"changeParticipants"), this::setParticipants);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"changePrivate"), this::setPrivate);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"getAllAccess"), this::getAll);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"getOwner"), this::getOwners);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"getCollaborators"), this::getCollaborators);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"getParticipants"), this::getParticipants);
		vertx.eventBus().consumer(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG,"getPrivate"), this::getPrivate);
	}
	
	
	private void setOwners(Message<Object> msg)
	{
		handleSetRequest(msg, RequestType.Owner);
	}	

	private void setPrivate(Message<Object> msg)
	{
		handleSetRequest(msg, RequestType.Private);
	}	
	
	private void setCollaborators(Message<Object> msg)
	{
		handleSetRequest(msg, RequestType.Collaborator);
	}	

	private void setParticipants(Message<Object> msg)
	{
		handleSetRequest(msg, RequestType.Participant);
	}	
	
	private void getOwners(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.Owner);
	}	

	private void getPrivate(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.Private);
	}	
	
	private void getCollaborators(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.Collaborator);
	}	

	private void getParticipants(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.Participant);
	}
	
	private void getAll(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.All);
	}	
	
	/**
	 * Handle a get request. The request fails if the Id of the request is not available. 
	 * The reply will contain a jsonObject with the following fields:
	 * {
	 *   "Result" : "Success/Error", # depending on whether the request is successful or not
	 *   "Reason" : "Error Reason", # The reason for a potential Error, only set if the operation failed
	 *   "<ownerField> : [ Owner1,...] # only set if the owners are requested
	 *   "<participantField> : [ Participant1,...] # only set if the participants are requested
	 *   "<collaboratorField> : [ collaborator1,...] # only set if the collaborators are requested
	 *   "<logonRequireField> : True/False # only set if privacy of the experiment is requested
	 *   }
	 * @param msg A json Message containing an IDField specified by this config
	 * @param rqType The type of access requested, i.e. for results, data or static files
	 */
	private void handleGetRequest(Message<Object> msg, RequestType rqType)
	{
		try 
		{
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();
			String expID = command.getString(getConfig("IDField"));
			JsonObject query = new JsonObject().put(getConfig("IDField"), expID);
			mongo.find(SoileConfigLoader.getCollectionName("experimentCollection"), query).onSuccess(dbResponse -> {
				//This has to be a unique ID!!
				if(dbResponse.size() == 1)
				{
					JsonObject targetObject = dbResponse.get(0);
					// if it does not exist, it is not private
					replyWithField(msg, rqType, targetObject);
				}
				else
				{
					msg.reply(SoileCommUtils.errorObject("There are " + dbResponse.size() + " objects with this ID in the database. There should be only one."));
					LOGGER.error("Requested experiment with ID: " +expID +  " had " + dbResponse.size() + " representations in the database!");
				}
				
			}).onFailure(failedResult -> {
				msg.reply(SoileCommUtils.errorObject(failedResult.getCause().getMessage()));			
			});
		}	
		else
		{
			msg.reply(SoileCommUtils.errorObject("Invalid Request"));
		}
		}
		catch(Exception e)
		{
			msg.reply(SoileCommUtils.errorObject("Invalid Request (" + e.getMessage() + ")"));
		}
	}
	
	private void replyWithField(Message<Object> msg, RequestType requestedPermissions, JsonObject dbObject)
	{
		JsonObject success = SoileCommUtils.successObject();
		if(requestedPermissions == RequestType.All)
		{
			for(RequestType t : RequestToComFields.keySet())
			{
				success.put(RequestToComFields.get(t), dbObject.getValue(RequestToDbFields.get(t)));
			}
		}
		else
		{
			success.put(RequestToComFields.get(requestedPermissions), dbObject.getValue(RequestToDbFields.get(requestedPermissions)));
		}
		msg.reply(success);
	}
	
	
	/**
	 * Handle a change request. The request fails if the Id of the request is not available. 
	 * The reply will contain a jsonObject with the following fields:
	 * {
	 *   "Result" : "Success/Error", # depending on whether the request is successful or not
	 *   "Reason" : "Error Reason", # The reason for a potential Error
	 *   }
	 * @param msg A json Message containing an IDField specified by this config
	 * @param rqType The type of access requested, i.e. for results, data or static files
	 */
	private void handleSetRequest(Message<Object> msg, RequestType rqType)
	{
		try 
		{
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();
			String expID = command.getString(getConfig("IDField"));
			JsonObject query = new JsonObject().put(getConfig("IDField"), expID);
			mongo.find(SoileConfigLoader.getCollectionName("experimentCollection"), query).onSuccess(dbResponse -> {
				//This has to be a unique ID!!
				if(dbResponse.size() == 1)
				{
					JsonObject targetObject = dbResponse.get(0);
					// if it does not exist, it is not private
				    switch(rqType)
					{
					case Owner:
						changeOwners(msg,command.getString(SoileConfigLoader.getCommunicationField("operationField")),command.getJsonArray(getConfig("userListField")),targetObject);
						break;
					case Private:
						setPrivacy(msg,command.getBoolean(getConfig("logonRequiredField")),targetObject);
						break;
					case Collaborator:
						changeCollaborators(msg,command.getString(SoileConfigLoader.getCommunicationField("operationField")),command.getJsonArray(getConfig("userListField")),targetObject);
						break;
					case Participant:
						changeParticipants(msg,command.getString(SoileConfigLoader.getCommunicationField("operationField")),command.getJsonArray(getConfig("userListField")),targetObject);
						break; 
					}																
				}
				else
				{
					msg.reply(SoileCommUtils.errorObject("There are " + dbResponse.size() + " objects with this ID in the database. There should be only one."));
					LOGGER.error("Requested experiment with ID: " +expID +  " had " + dbResponse.size() + " representations in the database!");
				}
				
			}).onFailure(failedResult -> {
				msg.reply(SoileCommUtils.errorObject(failedResult.getCause().getMessage()));			
			});
		}	
		else
		{
			msg.reply(SoileCommUtils.errorObject("Invalid Request"));
		}
		}
		catch(Exception e)
		{
			msg.reply(SoileCommUtils.errorObject("Invalid Request (" + e.getMessage() + ")"));
		}
	}
	

	private void setPrivacy(Message<Object> msg, boolean newSetting, JsonObject originalData)	
	{
		// build queries and update the database
		JsonObject query = new JsonObject().put(getConfig("IDField"), originalData.getString(getConfig("IDField")));
		JsonObject update = new JsonObject().put("$set", new JsonObject().put(getConfig("logonRequiredField"), newSetting));
		mongo.updateCollection(getConfig("collectionName"), query, update, res -> {
			if(res.succeeded())
			{
				msg.reply(SoileCommUtils.successObject());	
			}
			else
			{
				msg.reply(SoileCommUtils.errorObject("Could not change Visibility setting (" + res.cause().getMessage() + ")"));
			}
		});
	}
	
	private void changePermissionsForType(Message<Object> msg, String command, JsonArray changedOwners, JsonObject originalData, String targetField, String Role)
	{
		
		JsonArray originalOwners = originalData.getJsonArray(targetField);
		ChangeLists changes = new ChangeLists();
		if(!changes.setupLists(originalOwners, changedOwners, command))
			{
			msg.reply(SoileCommUtils.errorObject("Invalid Request"));
			};
		// build queries and update the database
		JsonObject query = new JsonObject().put(getConfig("IDField"), originalData.getString(getConfig("IDField")));
		JsonObject update = new JsonObject().put("$set", new JsonObject().put(targetField,originalOwners));
		mongo.updateCollection(getConfig("collectionName"), query, update, res -> {
			if(res.succeeded())
			{
				//Now, we also need to update the users.
				List<Future> changedUsers = new LinkedList();												
				submitChanges(changes, changedUsers, originalData.getString(getConfig("IDField")), Role);
				CompositeFuture.all(changedUsers).onComplete(userAddedRes ->
				{
					if(userAddedRes.succeeded())
					{
						msg.reply(SoileCommUtils.successObject());
					}
					else
					{
						msg.reply(SoileCommUtils.errorObject(userAddedRes.cause().getMessage()));	
					}
				});
					
			}
			else
			{
				msg.reply(SoileCommUtils.errorObject( "Could not set owners  (" + res.cause().getMessage() + ")"));
			}
		});
	}
	
	private void changeOwners(Message<Object> msg, String command, JsonArray changedOwners, JsonObject originalData)	
	{
		changePermissionsForType(msg, command, changedOwners, originalData, getConfig("ownerField"),SoileConfigLoader.Owner);		
	}
	
	private void changeCollaborators(Message<Object> msg, String command, JsonArray changedOwners, JsonObject originalData)	
	{
		changePermissionsForType(msg, command, changedOwners, originalData, getConfig("collaboratorField"),SoileConfigLoader.Collaborator);
	}
	
	private void changeParticipants(Message<Object> msg, String command, JsonArray changedOwners, JsonObject originalData)	
	{
		changePermissionsForType(msg, command, changedOwners, originalData, getConfig("participantField"),SoileConfigLoader.Participant);
	}
	
	
	private void submitChanges(ChangeLists changes, List<Future> changedUsers, String ID, String Role)
	{
		// Add Role to all users who are added.
		for(Object o : changes.toAdd)
		{
			JsonObject userCommand = new JsonObject()
					.put(SoileConfigLoader.getCommunicationField("usernameField"),o)
					.put(SoileConfigLoader.getCommunicationField("operationField"),SoileCommUtils.getCommunicationField("addCommand"))
					.put(SoileConfigLoader.getCommunicationField("experimentID"),ID)
					.put(SoileConfigLoader.getCommunicationField("roleChanged"), Role);


			changedUsers.add(Future.<AsyncResult<Message>>future(promise -> 
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "permissionOrRoleChange"), userCommand, messageHandled ->
			{
				if(messageHandled.succeeded())
				{
					promise.complete();
				}
				else
				{
					LOGGER.error("FATAL: Requested adding access to users, and experiment was updated, but some users could not be updated!!");
					LOGGER.error("FATAL: Users could include:\n" + changes.toAdd.encodePrettily());
					promise.fail(messageHandled.cause().getMessage());
				}
			})));

		}
		// remove role for all users that are remobved.
		for(Object o : changes.toRemove)
		{
			JsonObject userCommand = new JsonObject()
					.put(SoileConfigLoader.getCommunicationField("usernameField"),o)
					.put(SoileConfigLoader.getCommunicationField("operationField"),SoileCommUtils.getCommunicationField("removeCommand"))
					.put(SoileConfigLoader.getCommunicationField("experimentID"),ID)
					.put(SoileConfigLoader.getCommunicationField("roleChanged"), Role);


			changedUsers.add(Future.<AsyncResult<Message>>future(promise -> 
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "permissionOrRoleChange"), userCommand, messageHandled ->
			{
				if(messageHandled.succeeded())
				{
					promise.complete();
				}
				else
				{
					LOGGER.error("FATAL: Requested removing access to users, and experiment was updated, but some users could not be updated!!");
					LOGGER.error("FATAL: Users could include:\n" + changes.toRemove.encodePrettily());
					promise.fail(messageHandled.cause().getMessage());
				}
			})));

		}
	}

	
	
	private class ChangeLists
	{
		public JsonArray toAdd = new JsonArray();
		public JsonArray toRemove = new JsonArray();				
		
		public boolean setupLists(JsonArray original, JsonArray changed, String command)
		{		
			if(command.equals(getCommandString("setCommand")))
			{
				for(Object o : original)
				{
					if(!changed.contains(o))
					{
						toRemove.add(o);					
					}
				}
				for(Object o : changed)
				{
					if(!original.contains(o))
					{
						toAdd.add(o);					
					}
				}
				original = changed;
				
			}
			else if(command.equals(getCommandString("addCommand")))
			{
				for(Object o : changed)
				{
					// Add all owners not yet in the owner List
					if(!original.contains(o))
					{
						toAdd.add(o);
						original.add(o);
					}
				}
			}
			else if(command.equals(getCommandString("removeCommand")))
			{			
				// remove all owners from the current list
				for(Object o : changed)
				{
					toRemove.add(o);
					original.remove(o);
				}
				
			}
			else
			{			
				return false;
			}
			return true;
		}
		
	}
}
