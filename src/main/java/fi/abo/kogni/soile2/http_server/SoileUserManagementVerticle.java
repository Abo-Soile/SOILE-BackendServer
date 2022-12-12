package fi.abo.kogni.soile2.http_server;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.tsp.ers.SortedHashList;

import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager;
import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager.PermissionChange;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserAlreadyExistingException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserDoesNotExistException;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
/**
 * This class provides functionality for the user management of a soile project. it processes messages addressed to 
 * the {UserManagement_prefix} and a command suffix.
 * The messages need to contain JsonObjects of the following structure:
 * { 
 *   <usernameField> : "username",
 *   <passwordField> : "password",
 *   <userEmailField> : "email", # optional
 *   <userFullNameField> : "fullname", # optional
 *   <userRolesField> : [ "role1" , "role2" ] # optional
 *   <userPermissionsField> : [ "permission1" , "permission2" ] # optional
 *   <userTypeField> : "type"
 *   } 
 * Passwords need to be transferred using the systems hashing algorithm
 * @author thomas
 *
 */
public class SoileUserManagementVerticle extends SoileBaseVerticle {

	SoileUserManager userManager;
	SoileUserManager participantManager;
	MongoClient mongo;
	private static final Logger LOGGER = LogManager.getLogger(SoileUserManagementVerticle.class);
	private JsonObject sessionFields;
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		LOGGER.info("Starting UserManagementVerticle");
		mongo = MongoClient.createShared(vertx, config().getJsonObject("db"));
		setupConfig(SoileConfigLoader.USERMGR_CFG);		
		sessionFields = config().getJsonObject(SoileConfigLoader.SESSION_CFG);
		userManager = new SoileUserManager(mongo);		

		setupChannels();
		LOGGER.info("User Management Verticle Started");
		//LOGGER.debug("\n\nUser Management Verticle Started\n\n");
		startPromise.complete();
	}

	/**
	 * Set up the channels this Manager is listening to.
	 */
	void setupChannels()
	{				
		LOGGER.debug("Setting up channels");
		LOGGER.debug("Adding channel: " + getEventbusCommandString("addUser"));
		vertx.eventBus().consumer(getEventbusCommandString("addUser"), this::addUser);
		vertx.eventBus().consumer(getEventbusCommandString("addUserWithEmail"), this::addUserWithEmail);
		vertx.eventBus().consumer(getEventbusCommandString("removeUser"), this::removeUser);		
		vertx.eventBus().consumer(getEventbusCommandString("permissionOrRoleChange"), this::permissionOrRoleChange);		
		vertx.eventBus().consumer(getEventbusCommandString("setUserFullNameAndEmail"), this::setUserFullNameAndEmail);
		vertx.eventBus().consumer(getEventbusCommandString("getUserData"), this::getUserData);
		LOGGER.debug("Reistering eventbus consumer for:" + getEventbusCommandString("checkUserSessionValid")); 
		vertx.eventBus().consumer(getEventbusCommandString("checkUserSessionValid"), this::isSessionValid);
		vertx.eventBus().consumer(getEventbusCommandString("addSession"), this::addValidSession);
		vertx.eventBus().consumer(getEventbusCommandString("removeSession"), this::invalidateSession);

	}	
		
	/**
	 * Add a session to a user
	 * {
	 *  <usernameField> : username,
	 *  <sessionID> : sessionID,
	 *  }
	 *  If the session was successfully added    
	 *  {
	 *  "Result" : "Success", 
	 *  }
	 *  and if there was an Error, the result will be:
	 *  {
	 *  "Result" : "Error",
	 *  "Reason" : "Explanation"
	 *  }
	 * @param msg
	 */
	void addValidSession(Message<Object> msg)
	{
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();			

			LOGGER.debug("Trying to save a session with the following data: \n" + command.encodePrettily());
			userManager.addUserSession(command.getString(getCommunicationField("usernameField"))
												  ,command.getString(getCommunicationField("sessionID"))
												  ,res ->
			{
				if(res.succeeded())
				{
					LOGGER.debug("Adding Session suceeded");
					msg.reply(SoileCommUtils.successObject());
				}
				else
				{
					LOGGER.debug("Adding Session Failed: " + res.cause().getMessage());
					msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, res.cause().getMessage());						
				}
			});
		}	
		else
		{
			msg.fail(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid Request");
		}
	}
	
	/**
	 * Remove a session from a user
	 * {
	 *  <usernameField> : username,
	 *  <sessionID> : sessionID,
	 *  }
	 *  If the session was successfully added    
	 *  {
	 *  "Result" : "Success", 
	 *  }
	 *  and if there was an Error, the result will be:
	 *  {
	 *  "Result" : "Error",
	 *  "Reason" : "Explanation"
	 *  }
	 * @param msg
	 */
	void invalidateSession(Message<Object> msg)
	{
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();			
			
			userManager.removeUserSession(command.getString(getCommunicationField("usernameField"))
												  ,command.getString(getCommunicationField("sessionID"))
												  ,res ->
			{
				if(res.succeeded())
				{
					msg.reply(SoileCommUtils.successObject());
				}
				else
				{
					msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, res.cause().getMessage());						
				}
			});
		}	
		else
		{
			msg.fail(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid Request");
		}
	}
	
	/**
	 * Test whether a given session is still valid for a given user.
	 * {
	 *  <usernameField> : username,
	 *  <sessionID> : sessionID,
	 *  }
	 *  I the session is valid the result will be a json with   
	 *  {
	 *  "Result" : "Success",
	 *  <sessionIsValid> : true 
	 *  }
	 *  If the session is not valid, the result will be:
	 *  {
	 *  "Result" : "Success",
	 *  <sessionIsValid> : False 
	 *  }
	 *  and if there was an Error, the result will be:
	 *  {
	 *  "Result" : "Error",
	 *  "Reason" : "Explanation"
	 *  }
	 * @param msg
	 */
	void isSessionValid(Message<Object> msg)
	{
		LOGGER.debug("Got a request for a session validation");
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();						
			LOGGER.debug("Checking, whether a session is valid:" + command.encodePrettily());
			userManager.isSessionValid(command.getString(getCommunicationField("usernameField"))
												  ,command.getString(getCommunicationField("sessionID"))
												  ,res ->
			{
				if(res.succeeded())
				{
					if(res.result())
					{		
						LOGGER.debug("Session Valid. Replying accordingly");
						msg.reply(SoileCommUtils.successObject().put(sessionFields.getString("sessionIsValid"), true));
					}
					else
					{
						LOGGER.debug("Session no longer valid.");
						msg.fail(HttpURLConnection.HTTP_UNAUTHORIZED, "Session not valid");
					}
				}
				else
				{
					LOGGER.error(res.cause().getMessage() ) ;
					msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, "Session could not be validated " +  res.cause().getMessage());						
				}
			});
		}	
		else
		{
			
			msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, "Request Invalid" );
		}
	}
	
	
	/**
	 * Get the data user information for the given user name
	 * {
	 *  <usernameField> : username,
	 *  <userTypeField> : userType,
	 *  <userPasswordField> : userPassword,
	 *  }
	 * @param msg
	 */
	void getUserData(Message<Object> msg)
	{		
		//make sure we actually get the right thing
		
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();						
			
			userManager.getUserData(command.getString(getDBField("usernameField")), userRes ->{
				if(userRes.succeeded())
				{
					msg.reply(SoileCommUtils.successObject().put("Data", userRes.result()));
					return;
				}
				else
				{
					msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, "User could not be fetched: " +  userRes.cause().getMessage());					return;
				}
					
			});
		}	
		else
		{
			msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, "Request Invalid" );
		}
	}
	
	
	
	void addUserWithEmail(Message<Object> msg)
	{
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();			
			
			//LOGGER.debug("Verticle: Creating user");
			userManager.createUser(command.getString(getCommunicationField("usernameField")),
					command.getString(getCommunicationField("passwordField")), res -> 
			{
				//TODO: Implement				
			});
		}
	}
	
	/**
	 * Add a User to the database. he message body must be a jsonObject with at least the following fields:
	 * {
	 *  <usernameField> : username,
	 *  <userTypeField> : userType,
	 *  <userPasswordField> : userPassword,
	 *  <userFullNameField> : Full Name (optional)
	 *  <userEmailField> : Full Name (optional)
	 *  }
	 * @param msg
	 */
	void addUser(Message<Object> msg)
	{		
		//LOGGER.debug("Getting a user Creation request");
		//make sure we actually get the right thing
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();			
			
			//LOGGER.debug("Verticle: Creating user");
			userManager.createUser(command.getString(getCommunicationField("usernameField")),
					command.getString(getCommunicationField("passwordField")), id -> 
					{
						// do this only if the user was created Successfully
						if(id.succeeded()) {
							LOGGER.debug("User created successfully");
							LOGGER.debug("Username: " + command.getString(getDBField("usernameField"))
												+ " password: " + command.getString(getCommunicationField("passwordField"))
												+ " type: " + command.getString(getCommunicationField("userTypeField")));
							msg.reply(SoileCommUtils.successObject()); 			    					
						}
						else
						{
							if(id.cause() instanceof UserAlreadyExistingException)
							{
								msg.fail(HttpURLConnection.HTTP_CONFLICT, "User Exists");	
								return;
							}
							else
							{

								msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR,id.cause().getMessage());							
							}
						}
						});
		}	
		else
		{
			msg.fail(HttpURLConnection.HTTP_BAD_REQUEST,"Invalid Request");
		}
	}
	
	
	
	/**
	 * Remove a user. The message body must contain the 
	 * @param msg
	 */
	void removeUser(Message<Object> msg)
	{
		//make sure we actually get the right thing
		JsonObject answer = new JsonObject();
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();			
				
			userManager.deleteUser(command.getString(getDBField("usernameField")), res -> {
				if(res.succeeded())
				{
					MongoClientDeleteResult delRes = res.result();
					if( delRes.getRemovedCount() >= 1)
					{
						msg.reply(SoileCommUtils.successObject());	
					}
					else
					{
						msg.fail(UserDoesNotExistException.ERRORCODE, "User does not exist");
					}
				}
				else
				{
					msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR,res.cause().getMessage());				}
			});						    							
		}	
		else
		{
			msg.fail(HttpURLConnection.HTTP_BAD_REQUEST,"Invalid Request");
		}
	}
	/**
	 * Change the permissions of a user. The command needs to have the following structure:
	 * {
	 * 		"<usernameField>" : "UserName",
	 * 		"<changeTypeField>" : "<addCommand>"/"<setCommand>"/"<removeCommand>"
	 * 		"<experimentID>" : ID,
	 *      "<roleChanged>" : Owner/Participant/Collaborator
	 *      
	 * @param msg
	 */
	void permissionOrRoleChange(Message<JsonObject> msg)
	{
		//make sure we actually get the right thing
		if (msg.body() instanceof JsonObject)
		{
			//first get the data from the body
			JsonObject command = (JsonObject) msg.body();
			String userName = command.getString("username");			
			String changeType = command.getString("command");			
			String role = command.getString("role");
			JsonObject permissions = command.getJsonObject("permissions");			
			if(permissions != null && role != null) 
			{
				msg.fail(400, "Cannot change permission and role settings at the same time");
				return;
			}
			if(role != null)
			{
			userManager.updateRole(userName, getOptionForType("role"), role, 
				res ->
				{
					if(res.succeeded())
					{
						msg.reply(new JsonObject().put(SoileCommUtils.RESULTFIELD, SoileCommUtils.SUCCESS));
					}
					else
					{
						LOGGER.error("Could not update permissions for request:\n" + command.encodePrettily() );
						LOGGER.error("Error was:\n" + res.cause().getMessage());
						msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, res.cause().getMessage());	
					}					
				});
			}
			if(permissions != null)
			{
				userManager.changePermissions(userName, getOptionForType(permissions.getString("type")), permissions.getJsonArray("target"), getChange(changeType), res -> {
					if(res.succeeded())
					{
						msg.reply(new JsonObject().put(SoileCommUtils.RESULTFIELD, SoileCommUtils.SUCCESS));
					}
					else
					{
						LOGGER.error("Could not update permissions for request:\n" + command.encodePrettily() );
						LOGGER.error("Error was:\n" + res.cause().getMessage());
						msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, res.cause().getMessage());	
					}	
				});
			}
			
		}
		else
		{
			msg.fail(HttpURLConnection.HTTP_BAD_REQUEST,"Invalid Request");
		}
	}

	private PermissionChange getChange(String change)
	{
		if(change.equals(SoileConfigLoader.getStringProperty(SoileConfigLoader.COMMUNICATION_CFG, "setCommand")))
		{
			return PermissionChange.Replace;
		}
		if(change.equals(SoileConfigLoader.getStringProperty(SoileConfigLoader.COMMUNICATION_CFG, "addCommand")))
		{
			return PermissionChange.Add;
		}
		if(change.equals(SoileConfigLoader.getStringProperty(SoileConfigLoader.COMMUNICATION_CFG, "removeCommand")))
		{
			return PermissionChange.Remove;
		}
		return null;
	}
	
	private String getDBFieldForRoleType(String role)
	{
		if(role.equals(SoileConfigLoader.Owner))
		{
			return SoileConfigLoader.getdbField("ownerField");
		}
		if(role.equals(SoileConfigLoader.Participant))
		{
			return SoileConfigLoader.getdbField("participantField");
		}
		if(role.equals(SoileConfigLoader.Collaborator))
		{
			return SoileConfigLoader.getdbField("collaboraorField");
		}
		return null;
	}
	
	private MongoAuthorizationOptions getOptionForType(String type)
	{
		switch(type)
		{
			case SoileConfigLoader.TASK: return SoileConfigLoader.getMongoExperimentAuthorizationOptions();
			case SoileConfigLoader.EXPERIMENT: return SoileConfigLoader.getMongoExperimentAuthorizationOptions();
			case SoileConfigLoader.PROJECT: return SoileConfigLoader.getMongoProjectAuthorizationOptions();
			case SoileConfigLoader.INSTANCE: return SoileConfigLoader.getMongoInstanceAuthorizationOptions();				
			default: return SoileConfigLoader.getMongoAuthZOptions();
		}
	}
	
	void setUserFullNameAndEmail(Message<JsonObject> msg)
	{
		//make sure we actually get the right thing
		if (msg.body() instanceof JsonObject)
		{
				JsonObject command = (JsonObject)msg.body();

				//LOGGER.debug("Found fitting UserManager for type " + command.getString("type"));
				
			
				//LOGGER.debug("Verticle: Set Email and Full Name from data");
				userManager.setEmailAndFullName(command.getString(getDBField("usernameField")), 
						command.getString(getDBField("userEmailField")), 
						command.getString(getDBField("userFullNameField")), 
						res -> {
							if(res.succeeded())
							{
								msg.reply(SoileCommUtils.successObject()); 			    					
							}
							else
							{

								msg.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something went wrong when setting username and email: " +  res.cause().getMessage());
							}
						
						});
		}
	}	
	
}
