package fi.abo.kogni.soile2.http_server;

import java.util.Random;

import org.apache.commons.collections4.functors.InstanceofPredicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.userManagement.UserManager;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserAlreadyExistingException;
import fi.abo.kogni.soile2.http_server.utils.SoileCommUtils;
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;
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
public class UserManagementVerticle extends SoileEventVerticle {

	UserManager userManager;
	UserManager participantManager;
	MongoClient mongo;
	public static final String PARTICIPANT_CONFIG = "participant_db";
	public static final String USER_CONFIG = "user_db";
	private static final Logger LOGGER = LogManager.getLogger(UserManagementVerticle.class);
	private JsonObject sessionFields;
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		LOGGER.info("Starting UserManagementVerticle");
		mongo = MongoClient.createShared(vertx, config().getJsonObject("db"));
		setupConfig(SoileConfigLoader.USERMANAGEMENTFIELDS);
		JsonObject partConfig = config().getJsonObject(PARTICIPANT_CONFIG).mergeIn(config().getJsonObject(SoileEventVerticle.DB_FIELD));
		JsonObject userConfig = config().getJsonObject(USER_CONFIG).mergeIn(config().getJsonObject(SoileEventVerticle.DB_FIELD));		
		/* The JsonObject needs the following fields: 
		* collectionName - required in our case for two different (default:user)
		* permissionField - default:
		* roleField - default:  
		* usernameField - default: username
		* usernameCredentialField - default: same as usernameField 
		* passwordField - default: password
		* passwordCredentialField - default: same as passwordField 
		*/
		sessionFields = config().getJsonObject(SoileConfigLoader.SESSIONFIELDS);
		MongoAuthenticationOptions partAuthnOptions = new MongoAuthenticationOptions(partConfig);
		MongoAuthorizationOptions partAuthzOptions = new MongoAuthorizationOptions(partConfig);
		MongoAuthenticationOptions userAuthnOptions = new MongoAuthenticationOptions(userConfig);
		MongoAuthorizationOptions userAuthzOptions = new MongoAuthorizationOptions(userConfig);
		userManager = new UserManager(mongo,userAuthnOptions,userAuthzOptions, config(),USER_CONFIG);
		participantManager = new UserManager(mongo,partAuthnOptions,partAuthzOptions, config(),PARTICIPANT_CONFIG);
		

		setupChannels();
		LOGGER.info("User Management Verticle Started");
		//System.out.println("\n\nUser Management Verticle Started\n\n");
		startPromise.complete();
	}

	/**
	 * Set up the channels this Manager is listening to.
	 */
	void setupChannels()
	{				
		//System.out.println("Registering consumer for " +  UManagement_prefix + uManConfig.getJsonObject("commands").getString("addUser"));
		System.out.println("Adding he following handlers: \n" + getEventbusCommandString("addUser") + "\n" 
				+ getEventbusCommandString("removeUser") + "\n"
				+ getEventbusCommandString("permissionOrRoleChange") + "\n"
				+ getEventbusCommandString("setUserFullNameAndEmail") + "\n"
				+ getEventbusCommandString("getUserData") + "\n"
			);
		vertx.eventBus().consumer(getEventbusCommandString("addUser"), this::addUser);
		vertx.eventBus().consumer(getEventbusCommandString("removeUser"), this::removeUser);
		vertx.eventBus().consumer(getEventbusCommandString("permissionOrRoleChange"), this::permissionOrRoleChange);
		vertx.eventBus().consumer(getEventbusCommandString("setUserFullNameAndEmail"), this::setUserFullNameAndEmail);
		vertx.eventBus().consumer(getEventbusCommandString("getUserData"), this::getUserData);
		vertx.eventBus().consumer(getEventbusCommandString("checkUserSessionValid"), this::isSessionValid);
		vertx.eventBus().consumer(getEventbusCommandString("addSession"), this::addValidSession);

	}	
	
		
	/**
	 * Get the {@link UserManager} fitting to the supplied commands userTypeField.
	 * Two options are currently available, researchers, and participants. 
   	 * @param command a {@link JsonObject} containing at least the field "userTypeField" from the config. 
	 * @return the appropriate {@link UserManager}
	 */
	UserManager getFittingManager(JsonObject command)	
	{
		
		UserManager cman;
		if(command.getString(getDBField("userTypeField")).equals(getConfig("researcherType")))
		{
			cman = userManager;
		}
		else if(command.getString(getDBField("userTypeField")).equals(getConfig("participantType")))
		{
			cman = participantManager;
		}
		else
		{					
			return null;
		}
		return cman;
	}
	
	/**
	 * Add a session to a user
	 * {
	 *  <usernameField> : username,
	 *  <userTypeField> : userType,
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
			UserManager cman = getFittingManager(command);
			if(cman == null)
			{
				msg.reply(SoileCommUtils.errorObject("Invalid UserType Field"));
				return;
			}
			cman.addUserSession(command.getString(getCommunicationField("usernameField"))
												  ,getCommunicationField("userTypeField")
												  ,res ->
			{
				if(res.succeeded())
				{
					msg.reply(SoileCommUtils.successObject());
				}
				else
				{
					msg.reply(SoileCommUtils.errorObject(res.cause().getMessage()));
				}
			});
		}	
		else
		{
			msg.reply(SoileCommUtils.errorObject( "Invalid Request"));
		}
	}
	
	
	/**
	 * Test whether a given session is still valid for a given user.
	 * {
	 *  <usernameField> : username,
	 *  <userTypeField> : userType,
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
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();			
			UserManager cman = getFittingManager(command);
			if(cman == null)
			{
				msg.reply(SoileCommUtils.errorObject("Invalid UserType Field"));
				return;
			}
			cman.isSessionValid(command.getString(getCommunicationField("usernameField"))
												  ,getCommunicationField("userTypeField")
												  ,res ->
			{
				if(res.succeeded())
				{
					if(res.result())
					{
						msg.reply(SoileCommUtils.successObject().put(sessionFields.getString("sessionIsValid"), true));
					}
					else
					{
						msg.reply(SoileCommUtils.successObject().put(sessionFields.getString("sessionIsValid"), false));
					}
				}
				else
				{
					msg.reply(SoileCommUtils.errorObject(res.cause().getMessage()));
				}
			});
		}	
		else
		{
			msg.reply(SoileCommUtils.errorObject( "Invalid Request"));
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
			UserManager cman = getFittingManager(command);
			if(cman == null)
			{
				msg.reply(SoileCommUtils.errorObject("Invalid UserType Field"));
				return;
			}
			cman.getUserData(command.getString(getDBField("usernameField")), userRes ->{
				if(userRes.succeeded())
				{
					msg.reply(SoileCommUtils.successObject().put("Data", userRes.result()));
					return;
				}
				else
				{
					msg.reply(SoileCommUtils.errorObject(userRes.cause().getMessage()));
					return;
				}
					
			});
		}	
		else
		{
			msg.reply(SoileCommUtils.errorObject("Invalid Request"));
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
		//System.out.println("Getting a user Creation request");
		//make sure we actually get the right thing
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();
			UserManager cman = getFittingManager(command);
			//System.out.println("Found fitting UserManager for type " + command.getString("type"));
			
			if(cman == null)
			{
				msg.reply(SoileCommUtils.errorObject("Invalid UserType Field"));
				return;
			}	
			//System.out.println("Verticle: Creating user");
			cman.createUser(command.getString(getDBField("usernameField")),
					command.getString(getDBField("passwordField"))).onComplete(
					id -> {
						// do this only if the user was created Successfully
						if(id.succeeded()) {
							System.out.println("User created successfully");

							//System.out.println("Verticle: Set Email and Full Name from data");
							cman.setEmailAndFullName(command.getString(getDBField("usernameField")), 
									command.getString(getDBField("userEmailField")), 
									command.getString(getDBField("userFullNameField")), 
									result -> {
										if(result.succeeded())
										{
											msg.reply(SoileCommUtils.successObject()); 			    					
										}
										else
										{
											cman.deleteUser(command.getString(getDBField("usernameField")), res -> {

												if(res.succeeded())
												{
													msg.reply(SoileCommUtils.errorObject("Something went wrong when setting email and full name"));			    							
												}
												else
												{			    							
													msg.reply(SoileCommUtils.errorObject("User created, but Full name and email not set properly").put("Error",res.cause().getMessage()));
													LOGGER.error("User " + command.getString(getDBField("usernameField")) + " created, but could not set name + email");
												}			    							
											});
										}
									});
						}
						else
						{
							// Most likely the user existed already. 
							if(id.cause() instanceof UserAlreadyExistingException)
							{
								msg.reply(SoileCommUtils.errorObject("User Exists"));	
								return;
							}
							msg.reply(SoileCommUtils.errorObject(id.cause().getMessage()));
						}
					});

		}	
		else
		{
			msg.reply(SoileCommUtils.errorObject("Invalid Request"));
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
			UserManager cman = getFittingManager(command);
			if(cman == null)
			{
				msg.reply(SoileCommUtils.errorObject("Invalid UserType Field"));
				return;
			}			
			cman.deleteUser(command.getString(getDBField("usernameField")), res -> {
				if(res.succeeded())
				{
					msg.reply(SoileCommUtils.successObject());
				}
				else
				{
					msg.reply(SoileCommUtils.errorObject(res.cause().getMessage()));	
				}
			});						    							
		}	
		else
		{
			msg.reply(SoileCommUtils.errorObject("Invalid Request"));
		}
	}
	
	void permissionOrRoleChange(Message<JsonObject> msg)
	{
		//make sure we actually get the right thing
		if (msg.body() instanceof JsonObject)
		{
			
		}
	}

	
	void setUserFullNameAndEmail(Message<JsonObject> msg)
	{
		//make sure we actually get the right thing
		if (msg.body() instanceof JsonObject)
		{
			
		}
	}	
	
}
