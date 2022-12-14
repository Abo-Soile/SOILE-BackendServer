package fi.abo.kogni.soile2.http_server.userManagement;

import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.EmailAlreadyInUseException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserAlreadyExistingException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserDoesNotExistException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashString;
import io.vertx.ext.auth.HashingStrategy;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.auth.mongo.MongoUserUtil;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.ext.mongo.MongoClientUpdateResult;


/**
 * This class encapsulates all activity correlating to user management interaction with the MongoDB database. 
 *	This includes authorization, authentication as well as user generation, and password hashing.  
 * @author thomas
 *
 */
public class SoileUserManager implements MongoUserUtil{

	private static Logger LOGGER = LogManager.getLogger(SoileUserManager.class.getName()); 
	private final MongoClient client;
	private final HashingStrategy strategy;	
	private final SecureRandom random = new SecureRandom();
	private final MongoAuthenticationOptions authnOptions;
	private final MongoAuthorizationOptions authzOptions;
	private final String hashingAlgorithm;
	private HashMap<PermissionChange, String> permissionMap;
	public static enum PermissionChange{
		Remove,
		Add,
		Replace
	}
	

	
	public SoileUserManager(MongoClient client) {
		this.client = client;
		this.authnOptions = SoileConfigLoader.getMongoAuthNOptions();
		this.authzOptions = SoileConfigLoader.getMongoAuthZOptions();		
		strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
		hashingAlgorithm = SoileConfigLoader.getUserProperty("hashingAlgorithm");
		permissionMap = new HashMap<>();
		permissionMap.put(PermissionChange.Remove, "$pull");
		permissionMap.put(PermissionChange.Add, "$push");
		permissionMap.put(PermissionChange.Replace, "$set");
	}

	public SoileUserManager getUserList(int startpos)
	{
		return this;
	}

	public SoileUserManager removeUser(String username , Handler<AsyncResult<MongoClientDeleteResult>> resultHandler)
	{
		client.removeDocument(authnOptions.getCollectionName(),
				new JsonObject().put(authnOptions.getUsernameField(), username),
				resultHandler);
		return this;
	}

	/**
	 * Check whether the Email listed is present in the email list of the database.
	 * The handler needs to handle the AsyncResult that is the number of entries with that email.
	 * @param email The email address to check
	 * @param handler
	 * @return
	 */
	public SoileUserManager checkEmailPresent(String email, Handler<AsyncResult<Long>> handler)
	{
		JsonObject emailQuery = new JsonObject()		
				.put(SoileConfigLoader.getdbField("userEmailField"), email.toLowerCase());
		client.count(authzOptions.getCollectionName(), emailQuery, handler);
		
		return this;
	}
		
	
	/**
	 * Set Full name and Email address of a user.
	 * @param username username of the user
	 * @param email email address of the user
	 * @param fullName full name of the user
	 * @param resultHandler handler to handle the resulting MongoClientUpdateResult
	 * @return this
	 */
	public SoileUserManager setEmailAndFullName(String username, String email, String fullName, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{

		JsonObject targetQuery = new JsonObject()						
				.put(authzOptions.getUsernameField(), username);
		
		JsonObject update = new JsonObject()						
				.put("$set", new JsonObject()
				.put(SoileConfigLoader.getdbField("userEmailField"),email.toLowerCase())
				.put(SoileConfigLoader.getdbField("userFullNameField"),fullName));						

		
		
		checkEmailPresent(email, emailPresent ->
		{
			// if the email is not yet set, we can use it. 
			if(emailPresent.succeeded() && emailPresent.result() == 0L)
			{
				// Otherwise there is  no user with that username, so don't just add it!				
				client.updateCollection(
						authzOptions.getCollectionName(),
						targetQuery,
						update,										
						ares -> {
							if(ares.succeeded())
							{
								long docsMatched = ares.result().getDocMatched(); 
								if(docsMatched == 1L)
								{
									resultHandler.handle(ares);
								}
								else
								{
									if(docsMatched == 0L)
									{
										//the requested username was not in the database
										resultHandler.handle(Future.<MongoClientUpdateResult>failedFuture(new UserDoesNotExistException(username)));
									}
									else
									{
										resultHandler.handle(Future.<MongoClientUpdateResult>failedFuture("Multiple users with the same username in the database!!"));
									}
								}

							}
							else
							{
								resultHandler.handle(Future.<MongoClientUpdateResult>failedFuture(ares.cause()));
							}
						});		
			}
			else
			{
				if(emailPresent.failed())
				{
					resultHandler.handle(Future.<MongoClientUpdateResult>failedFuture(emailPresent.cause()));
				}
				else
				{
					resultHandler.handle(Future.<MongoClientUpdateResult>failedFuture(new EmailAlreadyInUseException(email)));	
				}
			}
		});
		return this;
	}

	
	/**
	 * Get the user data
	 * @param username - the id of the user
	 * @param resultHandler - a result handler to handle the results.
	 * @return
	 */
	public SoileUserManager getUserData(String username, Handler<AsyncResult<JsonObject>> resultHandler)
	{
		JsonObject query = new JsonObject().put(authnOptions.getUsernameField(), username);
		
		client.find(authzOptions.getCollectionName(),query, res ->
		{
			if(res.succeeded())
			{
				if(res.result().size() == 1)
				{
					JsonObject result = res.result().get(0);
					result.put("Result","Success").remove("password");
					resultHandler.handle(Future.succeededFuture(result));
				}
				else
				{
					resultHandler.handle(Future.failedFuture("No Unique entry ( " + res.result().size() + " entries found ) for user " + username));
				}
			}
			else
			{
				resultHandler.handle(Future.failedFuture(res.cause()));	
			}
		});
		return this;
	}		 


	/**
	 * Change roles or permissions indicating the correct field of the database. 
	 * @param username - the id to add the roles/permissions for.
	 * @param options - the roles or permissions database field
	 * @param rolesOrPermissions - the list of roles or permissions to change
	 * @param alterationFlag - Whether to add, remove or replace the indicated permissions. 
	 * @param resultHandler - the handler for the results.	 
	 * @return this
	 */
	public SoileUserManager changePermissions(String username, MongoAuthorizationOptions options, JsonArray rolesOrPermissions, PermissionChange alterationFlag, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{
		
		JsonObject queryObject = new JsonObject().put(options.getUsernameField(), username);		
		JsonObject updateObject = new JsonObject().put(permissionMap.get(alterationFlag), new JsonObject().put(options.getPermissionField(), rolesOrPermissions));
		client.updateCollection(options.getCollectionName(), queryObject, updateObject)
		.onComplete(res -> {
			resultHandler.handle(res);
				
		});		
		return this;
	}

	/**
	 * Update the permissions for a given user, replacing the old ones by the new ones.
	 * @param username - the id of the user to remove permissions
	 * @param options - the options to use for the update
	 * @param permission - the the permission to remove
	 * @param resultHandler - a result handler to handle the results.
	 * @return
	 */
	public SoileUserManager updatePermissions(String username, MongoAuthorizationOptions options, JsonArray permissions, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{
		return changePermissions(username, options, permissions,PermissionChange.Replace, resultHandler);		
	}		 


	/**
	 * Update the roles for a given user, replacing the old ones by the new ones.
	 * @param username - the id of the user to remove permissions
	 * @param permission - the the permission to remove
	 * @param resultHandler - a result handler to handle the results.
	 * @return
	 */
	public SoileUserManager updateRole(String username, MongoAuthorizationOptions options, String role, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{
		JsonObject queryObject = new JsonObject().put(options.getUsernameField(), username);		
		JsonObject updateObject = new JsonObject().put("$set", new JsonObject().put(options.getRoleField(), role));
		client.updateCollection(options.getCollectionName(), queryObject, updateObject)
		.onComplete(res -> {
			resultHandler.handle(res);				
		});		
		return this;
	}
	/**
	 * Remove a permission for a specific user
	 * @param username - the id of the user to remove permissions
	 * @param permission - the the permission to remove
	 * @param resultHandler - a result handler to handle the results.
	 * @return
	 */
	public SoileUserManager addPermission(String username, MongoAuthorizationOptions options, String permission, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{
		addPermissions(username, options, new JsonArray().add(permission), resultHandler);
		return this;
	}		

	/**
	 * Remove a permission for a specific user
	 * @param username - the id of the user to remove permissions
	 * @param permissions - the the permissions to remove
	 * @param resultHandler - a result handler to handle the results.
	 * @return
	 */
	public SoileUserManager addPermissions(String username, MongoAuthorizationOptions options, JsonArray permissions, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{
		return this.changePermissions(username, options, permissions, PermissionChange.Add, resultHandler);

	}

	/**
	 * Remove a permission for a specific user
	 * @param username - the id of the user to remove permissions
	 * @param permission - the the permission to remove
	 * @param resultHandler - a result handler to handle the results.
	 * @return
	 */
	public SoileUserManager removePermission(String username, MongoAuthorizationOptions options, String permission, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{
		removePermissions(username,options, new JsonArray().add(permission), resultHandler);
		return this;
	}		

	/**
	 * Remove a permission for a specific user
	 * @param username - the id of the user to remove permissions
	 * @param permissions - the the permissions to remove
	 * @param resultHandler - a result handler to handle the results.
	 * @return
	 */
	public SoileUserManager removePermissions(String username, MongoAuthorizationOptions options, JsonArray permissions, Handler<AsyncResult<MongoClientUpdateResult>> resultHandler)
	{
		return this.changePermissions(username, options, permissions, PermissionChange.Remove, resultHandler);

	}
	
	public SoileUserManager createUser(String username, String password, Handler<AsyncResult<String>> resultHandler) {
		if (username == null || password == null) {
			resultHandler.handle(Future.failedFuture("username or password are null"));			
			return this;
		}
		// This needs to be updated!
    // we have all required data to insert a user
    final byte[] salt = new byte[32];
    random.nextBytes(salt);
    return createHashedUser(
    		username,
    		strategy.hash(hashingAlgorithm,
    				null,
    				base64Encode(salt),
    				password),
    		resultHandler
    		);
	}
		

	/**
	 * Delete a user from the database
	 * @param username
	 * @param resultHandler
	 * @return this {@link MongoUserUtil}
	 */	
	public SoileUserManager deleteUser(String username, Handler<AsyncResult<MongoClientDeleteResult>> resultHandler)
	{
		client.removeDocument(
				authnOptions.getCollectionName(),
				new JsonObject().
				put(authnOptions.getUsernameField(), username),
				resultHandler);
		return this;
	}

	public Future<String> getParticipantIDForUserInProject(String username, String project)
	{
		Promise<String> participantPromise = Promise.promise();
		client.findOne(authnOptions.getCollectionName(),new JsonObject().put(authnOptions.getUsernameField(), username), new JsonObject().put(SoileConfigLoader.getdbField("participantField"),1 ))
		.onSuccess(participantJson -> {
			JsonArray participantInfo = participantJson.getJsonArray(SoileConfigLoader.getdbField("participantField"), new JsonArray());
			String particpantID = null;
			for(int i = 0; i < participantInfo.size(); i++)
			{
				if(participantInfo.getJsonObject(i).getString("uuid").equals(project))
				{
					particpantID = participantInfo.getJsonObject(i).getString("participantID");
					break;
				}
				
			}
			participantPromise.complete(particpantID);
		})
		.onFailure(err -> participantPromise.fail(err));
		
		return participantPromise.future();
																				  													  
																																					 
	}
	
	public Future<Void> makeUserParticpantInProject(String username, String projectInstanceID, String participantID)
	{
		JsonObject query = new JsonObject().put(authnOptions.getUsernameField(), username);
		JsonObject pullUpdate = new JsonObject().put("$pull", new JsonObject()
				  .put(SoileConfigLoader.getdbField("participantField"), new JsonObject()
						  				 .put("uuid", new JsonObject()
						  				 .put("$eq", projectInstanceID))));
		JsonObject pushUpdate = new JsonObject().put("$push", new JsonObject()
				.put(SoileConfigLoader.getdbField("participantField"), new JsonObject().put("uuid", projectInstanceID).put("participantID", participantID)));
		List<BulkOperation> pullAndPut = new LinkedList<>();
		BulkOperation pullOp = BulkOperation.createUpdate(query, pullUpdate);
		BulkOperation pushOp = BulkOperation.createUpdate(query, pushUpdate);
		pullAndPut.add(pullOp);
		pullAndPut.add(pushOp);		
		return client.bulkWrite(authnOptions.getCollectionName(), pullAndPut).mapEmpty();
		
	}
	
	public SoileUserManager createHashedUser(String username, String hash, Handler<AsyncResult<String>> resultHandler) {
		if (username == null || hash == null) {
			resultHandler.handle(Future.failedFuture("username or password hash are null"));
			return this;
		}
		if (username.contains("@"))
		{
			resultHandler.handle(Future.failedFuture("@ not allowed in usernames"));	
		}
		client.find(
				authnOptions.getCollectionName(),
				new JsonObject()
				.put(authnOptions.getUsernameField(), username),
				res -> {
					if(res.succeeded())
					{
						if(res.result().size() > 0)
						{
							resultHandler.handle(Future.failedFuture(new UserAlreadyExistingException(username)));
							return;
						}
						else {
							client.save(
									authnOptions.getCollectionName(),
									new JsonObject()
									.put(authnOptions.getUsernameField(), username)
									.put(authnOptions.getPasswordField(), hash),
									resultHandler
									);							
						}
					}
					else
					{
						resultHandler.handle(Future.failedFuture("Unable to access user database"));
						return;
					}
				});
		
		return this;
	}

	
	public SoileUserManager createUserRolesAndPermissions(String username, List<String> roles, List<String> permissions,
			Handler<AsyncResult<String>> resultHandler) {

		if (username == null) {
			resultHandler.handle(Future.failedFuture("username is null"));
			return this;
		}

		client.save(
				authzOptions.getCollectionName(),
				new JsonObject()
				.put(authzOptions.getUsernameField(), username)
				.put(authzOptions.getRoleField(), roles == null ? Collections.emptyList() : roles)
				.put(authzOptions.getPermissionField(), permissions == null ? Collections.emptyList() : permissions),
				resultHandler);

		return this;
	}
	
	public SoileUserManager getUserSalt(String username, Handler<AsyncResult<String>> handler)
	{
		client.find(authzOptions.getCollectionName(),
				new JsonObject()
				.put(authzOptions.getUsernameField(), username),ar ->{
					if(ar.succeeded())
					{
						List<JsonObject> res = ar.result();
						if(res.size() == 0)
						{
							handler.handle(Future.failedFuture(new UserDoesNotExistException(username)));
						}
						else if(res.size() == 1)
						{							
							HashString pwstring = new HashString(res.get(0).getString(authnOptions.getPasswordField()));
							handler.handle(Future.succeededFuture(pwstring.salt()));
						}
						else
						{
							handler.handle(Future.failedFuture(new DuplicateUserEntryInDBException(username)));
						}					
					}
					else
					{
						handler.handle(Future.failedFuture(ar.cause().getMessage()));
					}					
				});				
		return this;
	}	
	
	public SoileUserManager removeUserSession(String username, String sessionID, Handler<AsyncResult<MongoClientUpdateResult>> handler)	
	{
		if (username == null  || sessionID == null) {
			handler.handle(Future.failedFuture("Username or session not given"));
			return this;
		}
		// We hash this vs timing attacks.
		String hashedSessionID = strategy.hash(hashingAlgorithm,
											   null,
											   SoileConfigLoader.getSessionProperty("sessionStoreSecret"),
											   sessionID); 
		JsonObject query = new JsonObject()
				.put(authnOptions.getUsernameField(), username); 
		client.find(
				authnOptions.getCollectionName(),
				query,
				res -> {
					if(res.succeeded())
					{
						if(res.result().size() == 1)
						{
							// Adding the current session ID as valid session for the user.
							JsonObject validSessions = res.result()
																.get(0)
																.getJsonObject(SoileConfigLoader.getdbField("storedSessions"));
							if(validSessions != null)
							{	//if it's not initialized.
								//check for sessions that are too old;
								for(String session : validSessions.fieldNames())
								{								
									Long ctime = validSessions.getLong(session);
									//if this session is still valid keep it.
									if(System.currentTimeMillis() - ctime >SoileConfigLoader.getSessionLongProperty("maxTime"))
									{
										validSessions.remove(session);
									}
									if(session.equals(hashedSessionID))
									{
										validSessions.remove(session);
									}
								}
							}
							else
							{
								validSessions = new JsonObject();
							}
							client.updateCollection(authnOptions.getCollectionName(),
									query,
									new JsonObject()
									.put("$set", new JsonObject()
											.put(authnOptions.getUsernameField(), username)
											.put(SoileConfigLoader.getdbField("storedSessions"), validSessions)),
									handler);							
							return;
						}
						else {		
							if(res.result().size() == 0)
							{
								handler.handle(Future.failedFuture(new UserDoesNotExistException(username)));	
							}
							else
							{
								handler.handle(Future.failedFuture(new DuplicateUserEntryInDBException(username)));
							}
							return;	
						}
						
						
						
					}
					else
					{
						handler.handle(Future.failedFuture(new Exception("Unable to access user database")));
						return;
					}
				});
		return this;
	}
	
	
	public SoileUserManager addUserSession(String username, String sessionID, Handler<AsyncResult<MongoClientUpdateResult>> handler)	
	{
		if (username == null  || sessionID == null) {
			handler.handle(Future.failedFuture("Username or session not given"));
			return this;
		}
		// We hash this vs timing attacks.
		String hashedSessionID = strategy.hash(hashingAlgorithm,
											   null,
											   SoileConfigLoader.getSessionProperty("sessionStoreSecret"),
											   sessionID);
		LOGGER.debug("sessionID is :" + sessionID);
		LOGGER.debug("hashed ID is :" + hashedSessionID);
		JsonObject query = new JsonObject()
				.put(authnOptions.getUsernameField(), username); 
		client.find(
				authnOptions.getCollectionName(),
				query,
				res -> {
					if(res.succeeded())
					{
						if(res.result().size() == 1)
						{
							LOGGER.debug("Found one user object, trying to update sessions");
							// Adding the current session ID as valid session for the user.
							JsonObject validSessions = res.result()
																.get(0)
																.getJsonObject(SoileConfigLoader.getdbField("storedSessions"));
							if(validSessions != null)
							{	//if it's not initialized.
								//check for sessions that are too old;
								for(String session : validSessions.fieldNames())
								{								
									Long ctime = validSessions.getLong(session);
									//if this session is still valid keep it.
									if(System.currentTimeMillis() - ctime > SoileConfigLoader.getSessionLongProperty("maxTime"))
									{
										validSessions.remove(session);
									}
								}
							}
							else
							{
								validSessions = new JsonObject();
							}
							
							validSessions.put(hashedSessionID, System.currentTimeMillis());
							LOGGER.debug("Trying to add the following sessions:\n" + validSessions.encodePrettily());
							client.updateCollection(authnOptions.getCollectionName(),
									query,
									new JsonObject()
									.put("$set", new JsonObject()
											.put(authnOptions.getUsernameField(), username)
											.put(SoileConfigLoader.getdbField("storedSessions"), validSessions)),
									handler);							
							return;
						}
						else {		
							if(res.result().size() == 0)
							{
								handler.handle(Future.failedFuture(new UserDoesNotExistException(username)));	
							}
							else
							{
								handler.handle(Future.failedFuture(new DuplicateUserEntryInDBException(username)));
							}
							return;	
						}
						
						
						
					}
					else
					{
						handler.handle(Future.failedFuture(new Exception("Unable to access user database")));
						return;
					}
				});
		return this;
	}
	
	public SoileUserManager isSessionValid(String username, String sessionID, Handler<AsyncResult<Boolean>> handler)	
	{
		if (username == null  || sessionID == null) {
			handler.handle(Future.failedFuture("Username or session not given"));
			return this;
		}
		String hashedSessionID = strategy.hash(hashingAlgorithm,
											   null,
											   SoileConfigLoader.getSessionProperty("sessionStoreSecret"),
											   sessionID);
		LOGGER.debug("sessionID is :" + sessionID);
		LOGGER.debug("hashed ID is :" + hashedSessionID);
		JsonObject query = new JsonObject()
							   .put(authnOptions.getUsernameField(), username); 
		client.find(
				authnOptions.getCollectionName(),
				query,
				res -> {
					if(res.succeeded())
					{
						if(res.result().size() == 1)
						{
							LOGGER.debug(res.result().get(0).encodePrettily());
							// Adding the current session ID as valid session for the user.
							JsonObject storedSessions = res.result()
																.get(0)
																.getJsonObject(SoileConfigLoader.getdbField("storedSessions"));
							Long startTime = storedSessions.getLong(hashedSessionID);
							// if this is not present, then (i.e. the result is null, then it's not a stored ID.					
							LOGGER.debug("Current Time: " + System.currentTimeMillis() +  "; StartTime was: " + startTime + "; Max Age is: " + SoileConfigLoader.getSessionLongProperty("maxTime"));
							if(startTime != null && (System.currentTimeMillis() - startTime < SoileConfigLoader.getSessionLongProperty("maxTime")))
							{
								LOGGER.debug("Session validated Successfully");
								handler.handle(Future.succeededFuture(true));
							}							
							else
							{
								LOGGER.debug("Session validated successfully, but no longer valid.");
								handler.handle(Future.succeededFuture(false));	
							}
						}
						else {		
							if(res.result().size() == 0)
							{
								handler.handle(Future.failedFuture(new UserDoesNotExistException(username)));	
							}
							else
							{
								handler.handle(Future.failedFuture(new DuplicateUserEntryInDBException(username)));
							}
							return;	
						}
						
						
						
					}
					else
					{
						handler.handle(Future.failedFuture(new Exception("Unable to access user database")));
						return;
					}
				});
		return this;
	}

	@Override
	public Future<String> createUser(String username, String password) {
		Promise<String> promise = Promise.promise();
		createUser(username, password, promise);
		return promise.future();
	}

	@Override
	public Future<String> createHashedUser(String username, String hash) {
		Promise<String> promise = Promise.promise();
		createHashedUser(username, hash, promise);
		return promise.future();
		}

	@Override
	public Future<String> createUserRolesAndPermissions(String user, List<String> roles, List<String> permissions) {
		Promise<String> promise = Promise.promise();
		createUserRolesAndPermissions(user, roles,permissions, promise);
		return promise.future();
	}


}
