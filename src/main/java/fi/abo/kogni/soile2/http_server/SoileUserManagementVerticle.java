package fi.abo.kogni.soile2.http_server;

import java.net.HttpURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager;
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
		String userCollection = SoileConfigLoader.getdbProperty("userCollection");
		
		sessionFields = config().getJsonObject(SoileConfigLoader.SESSION_CFG);
		MongoAuthenticationOptions userAuthnOptions = createMongoAuthNOptions(userCollection);
		MongoAuthorizationOptions userAuthzOptions = createMongoAuthZOptions(userCollection);;
		userManager = new SoileUserManager(mongo,userAuthnOptions,userAuthzOptions, config());		

		setupChannels();
		LOGGER.info("User Management Verticle Started");
		//System.out.println("\n\nUser Management Verticle Started\n\n");
		startPromise.complete();
	}

	private MongoAuthenticationOptions createMongoAuthNOptions(String collection)
	{
		MongoAuthenticationOptions res = new MongoAuthenticationOptions();
		res.setCollectionName(collection);
		res.setPasswordCredentialField(getDBField("passwordCredentialField"));
		res.setPasswordField(getDBField("passwordField"));
		res.setUsernameCredentialField(getDBField("usernameCredentialField"));
		res.setUsernameField(getDBField("usernameField"));				
		return res;
	}
	
	private MongoAuthorizationOptions createMongoAuthZOptions(String collection)
	{
		MongoAuthorizationOptions res = new MongoAuthorizationOptions();
		res.setCollectionName(collection);
		res.setPermissionField(getDBField("userPermissionsField"));
		res.setRoleField(getDBField("userRolesField"));
		res.setUsernameField(getDBField("usernameField"));				
		return res;
	}
	/**
	 * Set up the channels this Manager is listening to.
	 */
	void setupChannels()
	{				
		System.out.println("Setting up channels");
		System.out.println("Adding channel: " + getEventbusCommandString("addUser"));
		vertx.eventBus().consumer(getEventbusCommandString("addUser"), this::addUser);
		vertx.eventBus().consumer(getEventbusCommandString("addUserWithEmail"), this::addUserWithEmail);
		vertx.eventBus().consumer(getEventbusCommandString("removeUser"), this::removeUser);		
		vertx.eventBus().consumer(getEventbusCommandString("permissionOrRoleChange"), this::permissionOrRoleChange);
		vertx.eventBus().consumer(getEventbusCommandString("setUserFullNameAndEmail"), this::setUserFullNameAndEmail);
		vertx.eventBus().consumer(getEventbusCommandString("getUserData"), this::getUserData);
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

			
			userManager.addUserSession(command.getString(getCommunicationField("usernameField"))
												  ,sessionFields.getString("sessionID")
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
												  ,sessionFields.getString("sessionID")
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
			
			userManager.isSessionValid(command.getString(getCommunicationField("usernameField"))
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
						msg.fail(HttpURLConnection.HTTP_UNAUTHORIZED, "Session not valid");
					}
				}
				else
				{
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
			
			//System.out.println("Verticle: Creating user");
			userManager.createUser(command.getString(getCommunicationField("usernameField")),
					command.getString(getCommunicationField("passwordField"))).onSuccess(id ->
				{

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
		//System.out.println("Getting a user Creation request");
		//make sure we actually get the right thing
		if (msg.body() instanceof JsonObject)
		{
			JsonObject command = (JsonObject)msg.body();			
			
			//System.out.println("Verticle: Creating user");
			userManager.createUser(command.getString(getCommunicationField("usernameField")),
					command.getString(getCommunicationField("passwordField"))).onComplete(
					id -> {
						// do this only if the user was created Successfully
						if(id.succeeded()) {
							System.out.println("User created successfully");
							System.out.println("Username: " + command.getString(getDBField("usernameField"))
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
				JsonObject command = (JsonObject)msg.body();

				//System.out.println("Found fitting UserManager for type " + command.getString("type"));
				
			
				//System.out.println("Verticle: Set Email and Full Name from data");
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
