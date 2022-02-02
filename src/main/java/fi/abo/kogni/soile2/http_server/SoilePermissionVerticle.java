package fi.abo.kogni.soile2.http_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;


/**
 * Permissions for Experiments are save in the form of JsonArrays containing strings. 
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
 *    - Always only accessible to owners.		   
 * @author Thomas Pfau
 *
 */
public class SoilePermissionVerticle extends SoileBaseVerticle {

	static final Logger LOGGER = LogManager.getLogger(SoilePermissionVerticle.class);
	final String DBFIELD;
	MongoClient mongo;	
	JsonObject userConfig;
	static enum RequestType
	{
		FilePermission,
		DataPermission,
		ResultPermission,
		OwnerChange,
		PrivateChange
	}
	
	public SoilePermissionVerticle(String target)
	{
		DBFIELD = target;
	}
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {		
		System.out.println("Starting SoilePermissionVerticle");
		mongo = MongoClient.createShared(vertx, config().getJsonObject("db"));		
		setupConfig(DBFIELD);
		userConfig = config().getJsonObject(SoileConfigLoader.USERMGR_CFG);
		setupEvents();
		startPromise.complete();
		System.out.println("Started successfully");
	}
	
	
	/**
	 * Set up eventbus consumers. 
	 * Request Consumers expect a JsonObject with the following structure, returning structures containing the requested data (see e.g. :
	 * {
	 * 	"<IDField>" : "ObjectID" 
	 * }
	 * Setting permissions Commands contain the structure:
	 * {
	 *  "<IDField>" : "ObjectID"
	 *  "<changeTypeField>" : "<addCommand>"/"<setCommand>"/"<removeCommand>"
	 *  "<ownerField>" : [ "ChangedOwner1", "ChangedOwner2"...]
	 * }
	 * Setting the private flag is done by:
	 * Setting permissions Commands contain the structure:
	 * {
	 *  "<IDField>" : "ObjectID"
	 *  "<logonRequiredField"> : true/false
	 * }
	 * No individual Permissions can be set for an experiment. That depends on whether a user is owner or not and whether the object is private or not.
	 */
	
	void setupEvents()
	{
		vertx.eventBus().consumer(getEventbusCommandString("getFilePermissions"), this::getFilePermissions);
		vertx.eventBus().consumer(getEventbusCommandString("getDataPermissions"), this::getDataPermissions);
		vertx.eventBus().consumer(getEventbusCommandString("getResultPermissions"), this::getResultPermissions);
		vertx.eventBus().consumer(getEventbusCommandString("setOwner"), this::setOwner);
		vertx.eventBus().consumer(getEventbusCommandString("setPrivate"), this::setPrivate);
	}
		
	
	private void setOwner(Message<Object> msg)
	{
		handleSetRequest(msg, RequestType.OwnerChange);
	}	

	private void setPrivate(Message<Object> msg)
	{
		handleSetRequest(msg, RequestType.PrivateChange);
	}	
	
	private void getFilePermissions(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.FilePermission);
	}	

	private void getDataPermissions(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.DataPermission);
	}	

	private void getResultPermissions(Message<Object> msg)
	{
		handleGetRequest(msg, RequestType.ResultPermission);
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
			mongo.find(DBFIELD, query).onSuccess(dbResponse -> {
				//This has to be a unique ID!!
				if(dbResponse.size() == 1)
				{
					JsonObject targetObject = dbResponse.get(0);
					// if it does not exist, it is not private
				    switch(rqType)
					{
					case FilePermission :
						msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request"));
						return;
					case DataPermission : 
						msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request"));
						return;
					case ResultPermission :
						msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request"));
						return;
					case OwnerChange:
						changeOwner(msg,command.getString(getConfig("changeTypeField")),command.getJsonArray(getConfig("ownerField")),targetObject);
						break;
					case PrivateChange:
						setPrivacy(msg,command.getBoolean(getConfig("logonRequiredField")),targetObject);
						break;
					}											
					
				}
				else
				{
					msg.reply(new JsonObject().put("Result", "Error").put("Reason","There are " + dbResponse.size() + " objects with this ID in the database"));
				}
				
			}).onFailure(failedResult -> {
				msg.reply(new JsonObject().put("Result", "Error").put("Reason", failedResult.getCause().getMessage()));			
			});
		}	
		else
		{
			msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request"));
		}
		}
		catch(Exception e)
		{
			msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request (" + e.getMessage() + ")"));
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
				msg.reply(new JsonObject().put("Result", "Success"));	
			}
			else
			{
				msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Could not change Visibility setting (" + res.cause().getMessage() + ")"));
			}
		});
	}
	
	private void changeOwner(Message<Object> msg, String command, JsonArray changedOwners, JsonObject originalData)	
	{
		JsonArray originalOwners = originalData.getJsonArray(getConfig("ownerField"));
		if(command.equals(getCommandString("setCommand")))
		{
			originalOwners = changedOwners;
		}
		else if(command.equals(getCommandString("addCommand")))
		{
			for(Object o : changedOwners)
			{
				// Add all owners not yet in the owner List
				if(!originalOwners.contains(o))
				{
					originalOwners.add(o);
				}
			}
		}
		else if(command.equals(getCommandString("removeCommand")))
		{			
			// remove all owners from the current list
			for(Object o : changedOwners)
			{
				originalOwners.remove(o);
			}	
		}
		else
		{
			msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request, command " + command + " unknown"));
			return;
		}
		// build queries and update the database
		JsonObject query = new JsonObject().put(getConfig("IDField"), originalData.getString(getConfig("IDField")));
		JsonObject update = new JsonObject().put("$set", new JsonObject().put(getConfig("ownerField"),originalOwners));
		mongo.updateCollection(getConfig("collectionName"), query, update, res -> {
			if(res.succeeded())
			{
				msg.reply(new JsonObject().put("Result", "Success"));	
			}
			else
			{
				msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Could not set owners  (" + res.cause().getMessage() + ")"));
			}
		});
	}
	
	/**
	 * Handle a permissions request. The request fails if the Id of the request is not available. 
	 * The reply will contain a jsonObject with the following fields:
	 * {
	 *   "Result" : "Success/Error", # depending on whether the request is successful or not
	 *   "Reason" : "Error Reason", # The reason for a potential Error
	 *   "<readRolesField>" : [ "Role1", "Role2", ...] # All Roles that can read the data
	 *   "<writeRolesField>" : [ "Role1", "Role2", ...] # All Roles that can write/modify the data
	 *   "<readPermissionsField>" : [ "Permission1", "Permission2", ...] # All Individual Permissions that are necessary to access the data (if the role does not provide the permission"
	 *   "<logonRequiredField>" : true/false # Whether a logon is required to read the information (false only for static files for now)
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
			mongo.find(DBFIELD, query).onSuccess(dbResponse -> {
				//This has to be a unique ID!!
				if(dbResponse.size() == 1)
				{
					JsonObject targetObject = dbResponse.get(0);
					// if it does not exist, it is not private
					buildPermissions(msg, targetObject, expID, rqType);											
					
				}
				else
				{
					if( dbResponse.size() == 0 )
					{
						msg.reply(new JsonObject().put("Result", "Error").put("Reason", getConfig("objectNotExistent")));
					}
					else
					{
						msg.reply(new JsonObject().put("Result", "Error").put("Reason","There are " + dbResponse.size() + " objects with this ID in the database"));
					}
				}
				
			}).onFailure(failedResult -> {
				msg.reply(new JsonObject().put("Result", "Error").put("Reason", failedResult.getCause().getMessage()));			
			});
		}	
		else
		{
			msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request"));
		}
		}
		catch(Exception e)
		{
			msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request (" + e.getMessage() + ")"));
		}
	}
	
	
	
	private void buildPermissions(Message<Object> msg, JsonObject targetObject, String expID, RequestType rqType)
	{
		boolean isPrivate = targetObject.getBoolean(getConfig("privateField")) == null ? true :targetObject.getBoolean(getConfig("privateField"));
	    JsonObject response = new JsonObject().put("Result", "Success");
	    JsonArray readPermissions = new JsonArray();
	    JsonArray readRoles = new JsonArray();
	    JsonArray writeRoles= new JsonArray();
	    // Reading is always allowed if you have the actual permission.
	    readPermissions.add(expID);
	    switch(rqType)
		{
		case FilePermission :
			buildFilePermissions(response, isPrivate, readRoles, writeRoles, targetObject);
			break;
		case DataPermission : 
			buildDataPermissions(response, isPrivate, readRoles, writeRoles, targetObject);
			break;
		case ResultPermission :
			buildResultPermissions(response, isPrivate, readRoles, writeRoles, targetObject);
			break;
		case OwnerChange:
			msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request"));
			return;
		case PrivateChange:
			msg.reply(new JsonObject().put("Result", "Error").put("Reason", "Invalid Request"));
			return;
		}
	    response.put(getConfig("writeRolesField"), writeRoles);
	    response.put(getConfig("readRolesField"), readRoles);
	    response.put(getConfig("readPermissions"), readPermissions);
	    msg.reply(response);
	}
	
	private void buildResultPermissions(JsonObject response, boolean isPrivate, JsonArray readRoles,
			JsonArray writeRoles, JsonObject targetObject) {
			//Only owners can read/write;
	    	response.put(getConfig("logonRequired"), true);
	    	readRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));
	    	writeRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));		
	}

	private void buildDataPermissions(JsonObject response, boolean isPrivate, JsonArray readRoles, JsonArray writeRoles,
			JsonObject targetObject) {
    	response.put(getConfig("logonRequired"), true);
		if(isPrivate)
	    {
	    	//Only owners can read/write;
	    	readRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));
	    	writeRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));
	    }
	    else
	    {
	    	readRoles.add(userConfig.getString("researcherType"));
	    	writeRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));	
	    }		
	}

	private void buildFilePermissions(JsonObject response, boolean isPrivate, JsonArray readRoles, JsonArray writeRoles, 
			JsonObject targetObject)
	{
		if(isPrivate)
	    {
	    	//Only owners can read/write;
	    	response.put(getConfig("logonRequired"), true);
	    	readRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));
	    	writeRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));
	    }
	    else
	    {
	    	response.put(getConfig("logonRequired"), false);
	    	readRoles.add(userConfig.getString("participantType")).add(userConfig.getString("researcherType"));
	    	writeRoles.addAll(targetObject.getJsonArray(getConfig("ownerField")));	
	    }
	}
}
