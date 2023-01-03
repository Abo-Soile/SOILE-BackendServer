package fi.abo.kogni.soile2.http_server.userManagement;

import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.client.model.IndexOptions;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.EmailAlreadyInUseException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.InvalidEmailAddress;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.InvalidRoleException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserAlreadyExistingException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserDoesNotExistException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
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
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;


//TODO: Reactor this to actually create futures. 

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

	public Future<Void> setupDB()
	{		
		IndexOptions options = new IndexOptions();
		options.unique(true);		
		return client.createIndex(authnOptions.getCollectionName(), new JsonObject().put(authnOptions.getUsernameField(), "text")).mapEmpty();
	}
	/**
	 * Check whether the Email listed is present in the email list of the database.
	 * The handler needs to handle the AsyncResult that is the number of entries with that email.
	 * @param email The email address to check
	 * @param handler
	 * @return
	 */
	public SoileUserManager checkEmailPresent(String email, Handler<AsyncResult<Boolean>> handler)
	{		
		handler.handle(isEmailInUse(email));
		return this;
	}

	/**
	 * Check whether the Email listed is present in the email list of the database.
	 * The handler needs to handle the AsyncResult that is the number of entries with that email.
	 * @param email The email address to check
	 * @param handler
	 * @return
	 */
	public Future<Boolean> isEmailInUse(String email)
	{
		if( email == null)
		{
			// there is no email so it can't be in use.
			return Future.succeededFuture(false);
		}
		JsonObject emailQuery = new JsonObject()		
				.put(SoileConfigLoader.getUserdbField("userEmailField"), email.toLowerCase());
		Promise<Boolean> presentPromise = Promise.<Boolean>promise();
		client.count(authzOptions.getCollectionName(), emailQuery)
		.onSuccess(count -> {
			presentPromise.complete(count == 1L);
		})
		.onFailure(err -> presentPromise.fail(err));		
		return presentPromise.future();
	}

	/**
	 * Get the list of users, 
	 * @param skip how many results to skip
	 * @param limit how many results at most to return
	 * @param query a search query to look up in the usernames
	 * @param partsInProject restricts search to users who participate in this project
	 * @return A List of JsonObjects with the results.
	 */
	public Future<JsonArray> getUserList(Integer skip, Integer limit, String query, boolean namesOnly)
	{
		JsonObject fields = new JsonObject().put("_id", 0)
				.put(authnOptions.getUsernameField(), 1);
		if(!namesOnly)
		{
			fields
			.put(SoileConfigLoader.getUserdbField("userEmailField"), 1)
			.put(SoileConfigLoader.getUserdbField("userFullNameField"),1)		  
			.put(authzOptions.getRoleField(), 1);
		}
		JsonObject Query = new JsonObject();
		if(query != null)
		{
			Query.put("$text", new JsonObject().put("$search", query));
		}
		FindOptions options = new FindOptions();
		if(limit != null)
		{
			options.setLimit(limit);
		}
		if(skip != null)
		{
			options.setSkip(skip);
		}
		options.setFields(fields);
		Promise<JsonArray> resultsPromise = Promise.promise();
		client.findWithOptions(authnOptions.getCollectionName(), Query, options)
		.onSuccess(result -> {
			resultsPromise.complete(new JsonArray(result));
		})
		.onFailure(err -> resultsPromise.fail(err));
		return resultsPromise.future();
	}

	/**
	 * Get the list of users, 
	 * @param skip how many results to skip
	 * @param limit how many results at most to return
	 * @param query a search query to look up in the usernames
	 * @return A List of JsonObjects with the results.
	 */
	public SoileUserManager getUserList(Integer skip, Integer limit, String query, boolean namesOnly, Handler<AsyncResult<JsonArray>> handler)
	{
		handler.handle(getUserList(skip, limit, query, namesOnly));
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
						.put(SoileConfigLoader.getUserdbField("userEmailField"),email.toLowerCase())
						.put(SoileConfigLoader.getUserdbField("userFullNameField"),fullName));						

		isEmailInUse(email)
		.onSuccess(present -> { 
			// if the email is not yet set, we can use it. 
			if(!present)
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
				resultHandler.handle(Future.<MongoClientUpdateResult>failedFuture(new EmailAlreadyInUseException(email)));	
			}
		})
		.onFailure(err -> resultHandler.handle(Future.<MongoClientUpdateResult>failedFuture(err)));
		return this;
	}


	/**
	 * Set Full name and Email address of a user.
	 * @param username username of the user
	 * @param email email address of the user
	 * @param resultHandler handler to handle the resulting MongoClientUpdateResult
	 * @return this
	 */
	public Future<Void> setEmail(String username, String email)
	{

		JsonObject targetQuery = new JsonObject()						
				.put(authzOptions.getUsernameField(), username);

		JsonObject update = new JsonObject()						
				.put("$set", new JsonObject()
						.put(SoileConfigLoader.getUserdbField("userEmailField"),email.toLowerCase()));							
		Promise<Void> emailSetPromise = Promise.promise();
		isEmailInUse(email)
		.onSuccess(present ->
		{
			// if the email is not yet set, we can use it. 
			if(!present)
			{
				// Otherwise there is  no user with that username, so don't just add it!				
				client.updateCollection(
						authzOptions.getCollectionName(),
						targetQuery,
						update)
				.onSuccess( updated -> {							
								long docsMatched = updated.getDocMatched(); 
								if(docsMatched == 1L)
								{
									emailSetPromise.complete();
								}
								else
								{
									if(docsMatched == 0L)
									{
										//the requested username was not in the database
										emailSetPromise.fail(new UserDoesNotExistException(username));
									}
									else
									{
										LOGGER.error("Multiple objects updated by a single query. Query was: " + targetQuery.encode());
										// it was changed BUT we have a problem!
										emailSetPromise.complete();
									}
								}

							})
				.onFailure(err -> emailSetPromise.fail(err));		
			}
			else
			{
				emailSetPromise.fail(new EmailAlreadyInUseException(email));	
			}
		})
		.onFailure(err -> emailSetPromise.fail(err));
		return emailSetPromise.future();
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
		try
		  {
			  Roles.valueOf(role);
		  }
		  catch(IllegalArgumentException e)
		  {
			  resultHandler.handle(Future.failedFuture(new InvalidRoleException(role)));
			  return this;
		  }
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
	 * @param username - 
	 * @param cleanupFiles - whether to remove all data from experiments. 
	 * @param resultHandler
	 * @return this {@link SoileUserManager}
	 */	
	public SoileUserManager deleteUser(String username, Handler<AsyncResult<Void>> resultHandler)
	{
		resultHandler.handle(deleteUser(username));		
		return this;
	}

	public Future<Void> deleteUser(String username)
	{
		// CleanUp of the users data needs to be done elsewhere, and this needs to be done after the cleanup.
		Promise<Void> deletionPromise = Promise.promise();	
		client.removeDocuments(
				authnOptions.getCollectionName(),
				new JsonObject().
				put(authnOptions.getUsernameField(), username))
		.onSuccess(res -> { 						
			if( res.getRemovedCount() >= 1)
			{							
				deletionPromise.complete();					
			}
			else
			{
				deletionPromise.fail(new UserDoesNotExistException(username));						
			}				
		}).onFailure(err -> deletionPromise.fail(err));
		return deletionPromise.future();
	}

	/**
	 * Retrieve the participant ID of this user in the given project. 
	 * @param username
	 * @param project
	 * @return
	 */
	public Future<String> getParticipantIDForUserInProject(String username, String project)
	{
		Promise<String> participantPromise = Promise.promise();
		client.findOne(authnOptions.getCollectionName(),new JsonObject().put(authnOptions.getUsernameField(), username), new JsonObject().put(SoileConfigLoader.getUserdbField("participantField"),1 ))
		.onSuccess(participantJson -> {
			JsonArray participantInfo = participantJson.getJsonArray(SoileConfigLoader.getUserdbField("participantField"), new JsonArray());
			String particpantID = null;
			for(int i = 0; i < participantInfo.size(); i++)
			{
				// TODO: Use an aggregation to retrieve this (that's probably faster).
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

	/**
	 * Get all project/participant ID combinations for the given user.
	 * @param username The user for which data is requested.
	 * @return A {@link Future} of a {@link JsonArray} containing objects  with { uuid: <projectInstanceID>, participantID : <IDofParticipantInPRoject> }. 
	 */
	public Future<JsonArray> getParticipantInfoForUser(String username)
	{
		Promise<JsonArray> participantInfoPromise = Promise.promise();
		client.findOne(authnOptions.getCollectionName(),new JsonObject().put(authnOptions.getUsernameField(), username), new JsonObject().put(SoileConfigLoader.getUserdbField("participantField"),1 ))
		.onSuccess(participantJson -> {
			JsonArray participantInfo = participantJson.getJsonArray(SoileConfigLoader.getUserdbField("participantField"), new JsonArray());
			participantInfoPromise.complete(participantInfo);
		})
		.onFailure(err -> participantInfoPromise.fail(err));

		return participantInfoPromise.future();																																				
	}

	/**
	 * Assign the given participantID to the user in the projectInstance indicated by the projectInstanceID.
	 * @param username The username for whom to assign a participantID
	 * @param projectInstanceID the projectinstance in which to assign the id
	 * @param participantID the participantID.
	 * @return A successfull future if this call worked.
	 */
	public Future<Void> makeUserParticpantInProject(String username, String projectInstanceID, String participantID)
	{
		JsonObject query = new JsonObject().put(authnOptions.getUsernameField(), username);
		JsonObject pullUpdate = new JsonObject().put("$pull", new JsonObject()
				.put(SoileConfigLoader.getUserdbField("participantField"), new JsonObject()
						.put("uuid", new JsonObject()
								.put("$eq", projectInstanceID))));
		JsonObject pushUpdate = new JsonObject().put("$push", new JsonObject()
				.put(SoileConfigLoader.getUserdbField("participantField"), new JsonObject().put("uuid", projectInstanceID).put("participantID", participantID)));
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
									.getJsonObject(SoileConfigLoader.getUserdbField("storedSessions"));
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
											.put(SoileConfigLoader.getUserdbField("storedSessions"), validSessions)),
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
									.getJsonObject(SoileConfigLoader.getUserdbField("storedSessions"));
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
											.put(SoileConfigLoader.getUserdbField("storedSessions"), validSessions)),
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
									.getJsonObject(SoileConfigLoader.getUserdbField("storedSessions"));
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

	/**
	 * Set the user information for a user (fullname, email, role) 
	 * @param username the user for which to change the information
	 * @param command the new values ( currently only "email", "userRole" and "fullname" fields are supported.
	 * @return A Future that indicates whether this operation was a success
	 */
	public Future<Void> setUserInfo(String username, JsonObject command) {
		Promise<Void> userUpdatedPromise = Promise.promise();
		JsonObject updates = new JsonObject();
		JsonObject updateCommand = new JsonObject().put("$set", updates);
		for(String key : command.fieldNames())
		{			
			String target = "";
			switch(key)
			{
			case "email": target = SoileConfigLoader.getUserdbField("emailField");
						  if(!UserUtils.isValidEmail(command.getString(key)))
						  {
							  return Future.failedFuture(new InvalidEmailAddress(target));
						  }
						  break;
			case "userRole":  target = authzOptions.getRoleField();
							  try
							  {
								  Roles.valueOf(command.getString(key));
							  }
							  catch(IllegalArgumentException e)
							  {
								  return Future.failedFuture(new InvalidRoleException(command.getString(key)));  
							  }							  
			  break;
			case "fullname": target = SoileConfigLoader.getUserdbField("userFullNameField");
			  break;			
			}
			if(!target.equals(""))
			{
				updates.put(target, command.getString(key));
			}
		}
		if(updates.size() == 0)
		{
			// nothing to do... 
			return Future.succeededFuture();
		}	
		JsonObject query = new JsonObject().put(SoileConfigLoader.getUserdbField("usernameField"), username);	
		isEmailInUse(command.getString("email"))
		.onSuccess(inUse -> {
			if(inUse)
			{
				userUpdatedPromise.fail(new EmailAlreadyInUseException(command.getString("email")));
			}
			else
			{
				client.updateCollection(authnOptions.getCollectionName(), query, updateCommand)
				.onSuccess(res -> {
					if(res.getDocMatched() != 1)
					{
						LOGGER.error("Modified more than one document. This should not happen. Query was: " +  query.encode());						
					}
					userUpdatedPromise.complete();
				})
				.onFailure(err -> userUpdatedPromise.fail(err));
			}
		})
		.onFailure(err -> userUpdatedPromise.fail(err));		
		return userUpdatedPromise.future();
	}

	/**
	 * Set the user information for a user (fullname, email, role) 
	 * @param username the user for which to change the information
	 * @param command the new values ( currently only "email", "userRole" and "fullname" fields are supported.
	 * @return A Future that indicates whether this operation was a success
	 */
	public Future<JsonObject> getUserInfo(String username) {
		JsonObject query = new JsonObject().put(SoileConfigLoader.getUserdbField("usernameField"), username);
		JsonObject fields = new JsonObject().put(SoileConfigLoader.getUserdbField("usernameField"), 1)
											.put(SoileConfigLoader.getUserdbField("userEmailField"), 1)
											.put(SoileConfigLoader.getUserdbField("userRolesField"), 1)
											.put(SoileConfigLoader.getUserdbField("userFullNameField"), 1)
											.put("_id", 0);
		return client.findOne(authnOptions.getCollectionName(), query, fields);
	}

	public Future<JsonObject> getUserAccessInfo(String username) {
		JsonObject query = new JsonObject().put(SoileConfigLoader.getUserdbField("usernameField"), username);
		JsonObject fields = new JsonObject().put(SoileConfigLoader.getUserdbField("userRolesField"), 1)
											.put(SoileConfigLoader.getUserdbField("experimentPermissionsField"), 1)
											.put(SoileConfigLoader.getUserdbField("instancePermissionsField"), 1)
											.put(SoileConfigLoader.getUserdbField("projectPermissionsField"), 1)
											.put(SoileConfigLoader.getUserdbField("taskPermissionsField"), 1)
											.put("_id", 0);
		return client.findOne(authnOptions.getCollectionName(),query,fields);
	}
}
