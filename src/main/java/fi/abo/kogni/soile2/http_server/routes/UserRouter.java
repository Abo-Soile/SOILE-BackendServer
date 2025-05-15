package fi.abo.kogni.soile2.http_server.routes;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileIDBasedAuthorizationHandler;
import fi.abo.kogni.soile2.utils.MessageResponseHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

/**
 * This class implements all routes under the "user" tag from the API.
 * The names of the functions are equivalent to the operationIDs of the openAPI access points.
 * @author Thomas Pfau
 *
 */
public class UserRouter extends SoileRouter {

	EventBus eb;


	private static Logger LOGGER = LogManager.getLogger(UserRouter.class.getName()); 


	/**
	 * Default constructor
	 * @param auth The {@link SoileAuthorization} for Auth checks
	 * @param vertx The {@link Vertx} instance for communication
	 * @param client The {@link MongoClient} for db access 
	 */
	public UserRouter(SoileAuthorization auth, Vertx vertx, MongoClient client)
	{
		super(auth,client);
		this.eb = vertx.eventBus();
	}

	/**
	 * List all users (accessible for Admins and Researchers, the latter only get the names)
	 * @param context The {@link RoutingContext} containing the user to check access
	 */
	public void listUsers(RoutingContext context)
	{
		// Admins get almost full info. 
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		Integer skip = params.queryParameter("skip") == null ? null : params.queryParameter("skip").getInteger();
		Integer limit = params.queryParameter("limit") == null ? null : params.queryParameter("limit").getInteger();
		String query = params.queryParameter("searchString") == null ? null : params.queryParameter("searchString").getString();
		String type = params.queryParameter("type") == null ? null : params.queryParameter("type").getString();
		JsonObject body = new JsonObject();
		body.put("skip", skip).put("limit", limit).put("query", query).put("type", type);		
		checkAccess(context.user(),Roles.Admin, experimentAuth)
		.onSuccess(authed -> {								
				handleUserManagerCommand(context, "listUsers", body, MessageResponseHandler.createDefaultHandler(200));
		})
		.onFailure(err -> {
					checkAccess(context.user(), Roles.Researcher, experimentAuth)
					.onSuccess(authorized -> {
						body.put("namesOnly", true);
						handleUserManagerCommand(context, "listUsers", body, MessageResponseHandler.createDefaultHandler(200));
					})
					.onFailure(err2 -> handleError(err2, context));					
		});				
	}
	
	/**
	 * Register a user according to the API command
	 * @param context The {@link RoutingContext} to extract the USer info from
	 */
	public void registerUser(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();
		handleUserManagerCommand(context, "addUserWithEmail", body, MessageResponseHandler.createDefaultHandler(201));
	}

	/**
	 * Route for user deletion. 
	 * TODO: If deleteFiles is false/unassigned report a unique id that can be used to reassociate data and allow deletion of the data.  
	 * @param context the {@link RoutingContext} to extract the user from
	 */
	public void removeUser(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();		
		Boolean deleteFiles = body.getBoolean("deleteFiles") == null ? false : body.getBoolean("deleteFiles");
		String username = body.getString("username");
		// for roles, the Authorization provider doesn't make a difference.
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {
			if(deleteFiles)
			{
				eb.request("soile.umanager.getParticipantsForUser",new JsonObject().put("username", username))
				.onSuccess(response -> {
					JsonObject reply = (JsonObject) response.body();
					eb.request("soile.participant.delete", reply.getJsonArray("participantIDs"))
					.onSuccess(success -> {
							handleUserManagerCommand(context, "removeUser", body, MessageResponseHandler.createDefaultHandler(202));			
					})
					.onFailure(err -> handleError(err, context));	
				})
				.onFailure(err -> handleError(err, context));
				
				
			}
			else
			{
				handleUserManagerCommand(context, "removeUser", body, MessageResponseHandler.createDefaultHandler(202));
			}			
		})
		.onFailure(err -> handleError(err, context));

	}
	
	/**
	 * This allows setting the user information (except the password)
	 * @param context The {@link RoutingContext} to extract the user for which to set the given info
	 */
	public void setUserInfo(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();				
		String username = body.getString("username");
		JsonObject userData = new JsonObject();
		userData.put("username", username);
		userData.put("fullname", body.getString("fullname"));
		userData.put("email", body.getString("email"));
		if(body.containsKey("role") )
		{
			userData.put("role", body.getString("role"));
		}				
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {
			// we are setting a role.
			if(body.containsKey("role"))
			{
				checkAccess(context.user(), Roles.Admin, experimentAuth)
				.onSuccess(isAdmin -> {
					handleUserManagerCommand(context, "setUserInfo", userData, MessageResponseHandler.createDefaultHandler(200));
				})
				.onFailure(err -> handleError(err, context));			
			}
			else
			{
				handleUserManagerCommand(context, "setUserInfo", userData, MessageResponseHandler.createDefaultHandler(200));
			}
			
		})
		.onFailure(err -> handleError(err, context));
	}
	
	/**
	 * This allows setting the user information (except the password)
	 * @param context The {@link RoutingContext} to get the user for which to set the password
	 */
	public void setPassword(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();				
		String username = body.getString("username");
		JsonObject userData = new JsonObject();
		userData.put("username", username);
		userData.put("password", body.getString("password"));		
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {
			// we are setting a role.
			handleUserManagerCommand(context, "setPassword", userData, MessageResponseHandler.createDefaultHandler(200));			
		})
		.onFailure(err -> handleError(err, context));
	}
	
	/**
	 * Set the Role of a specified user (only Admins are allowed to do this)
	 * @param context The current {@link RoutingContext} (contains User and data to use)
	 */
	public void setRole(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();				
		String username = body.getString("username");
		JsonObject userData = new JsonObject();
		userData.put("username", username);
		userData.put("role", body.getString("role"));		
		checkAccess(context.user(), Roles.Admin, experimentAuth)
		.onSuccess(allowed -> {
			// we are setting a role.
			handleUserManagerCommand(context, "permissionOrRoleChange", userData, MessageResponseHandler.createDefaultHandler(200));			
		})
		.onFailure(err -> handleError(err, context));
	}
	
	/**
	 * Set the permissions of a specified user.
	 * @param context The current {@link RoutingContext} (contains User and data to use)
	 */
	public void permissionChange(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();				
		String username = body.getString("username");
		JsonObject permissionProps = body.getJsonObject("permissionsProperties");
		JsonObject userData = new JsonObject();
		userData.put("username", username);
		userData.put("command", body.getString("command"));
		userData.put("permissionsProperties", permissionProps);
		LOGGER.debug(context.user().principal().encodePrettily());
		eb.request("soile.permissions.checkTargets", permissionProps)
		.onSuccess(permissionsExist -> { 
			checkUserHasAllPermissions(context.user(),permissionProps.getJsonArray("permissionSettings"), getAuthForType(permissionProps.getString("elementType")),getHandlerForType(permissionProps.getString("elementType")))
			.onSuccess(allowed -> {			
				LOGGER.debug("User has all required permissions");
				handleUserManagerCommand(context, "permissionOrRoleChange", userData, MessageResponseHandler.createDefaultHandler(200));			
			})
			.onFailure(err -> handleError(err, context));
			/*.recover(err -> {
				LOGGER.error("Request errored, trying to recover with failed future");
				return Future.failedFuture(err);
			});*/
		})
		.onFailure(err -> handleError(err, context));
	}
	
	/**
	 * Set the permissions of a specified user.
	 * @param context The current {@link RoutingContext} (contains User and data to use)
	 */
	public void permissionOrRoleRequest(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);						
		String username = params.queryParameter("username").getString();		
		JsonObject userData = new JsonObject();
		userData.put("username", username);
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {			
			handleUserManagerCommand(context, "getAccessRequest", userData, MessageResponseHandler.createDefaultHandler(200));			
		})
		.onFailure(err -> handleError(err,context));
	
	}
	
	/**
	 * Get the use userinfo
	 * @param context The current {@link RoutingContext} (contains User and data to use)
	 */
	public void getUserInfo(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
					
		String username = params.queryParameter("username").getString();
		JsonObject userData = new JsonObject();
		userData.put("username", username);
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {
			// we are setting a role.
			handleUserManagerCommand(context, "getUserInfo", userData, MessageResponseHandler.createDefaultHandler(200));				
		})
		.onFailure(err -> handleError(err, context));
	}
	/**
	 * Get the Active projects for the user in the context
	 * @param context The current {@link RoutingContext} (contains User and data to use)
	 */
	public void getUserActiveProjects(RoutingContext context)
	{
		User currentUser = context.user();
		if(currentUser == null)
		{
			handleError(new HttpException(401, "No user authenticated"), context);
		}
		else
		{
			if(isTokenUser(currentUser))
			{				
				
				// token user, so lets look up the project
				String participantToken = currentUser.principal().getString("access_token");
				String projectID = participantToken.substring(participantToken.indexOf("$")+1);
				LOGGER.debug("Found a token user returning the one project: " + projectID);
				context.response()
					.setStatusCode(200)
					.putHeader("content-type","application/json")
					.end(new JsonArray().add(projectID).encode());				
			}
			else
			{
				handleUserManagerCommand(context, "getParticipantsForUser", context.user().principal(), new MessageResponseHandler() {
					
					@Override
					public void handle(JsonObject responseData, RoutingContext context) {
						// TODO Auto-generated method stub
						if(responseData.getString(SoileCommUtils.RESULTFIELD).equals(SoileCommUtils.SUCCESS))
						{
							LOGGER.debug(responseData.encodePrettily());
							context.response().setStatusCode(200);
							JsonArray response = new JsonArray();							
							JsonArray data = responseData.getJsonArray("participantIDs", new JsonArray());
							for(int i = 0; i < data.size(); ++i)
							{
								response.add(data.getJsonObject(i).getString("UUID"));
							}
								context.response()
								.putHeader("content-type","application/json")
								.end(response.encode());																		
						}
						else
						{
							handleError(new HttpException(500,"Something went wrong"), context);
						}
					}
				});
			}
			
		}
	}
	/**
	 * Create a new user
	 * @param context The current {@link RoutingContext} (contains User and data to use)
	 */
	public void createUser(RoutingContext context)
	{
		registerUser(context);
	}

	
	/**
	 * Handle a response from the User Manager. Does standard error handling etc.
	 * @param routingContext The current {@link RoutingContext} (contains User and data to use)
	 * @param command The command sent to the UserManager
	 * @param commandContent the Content of the command sent
	 * @param messageHandler the Response handler to use
	 */
	void handleUserManagerCommand(RoutingContext routingContext, String command, JsonObject commandContent, MessageResponseHandler messageHandler)
	{		
		LOGGER.debug("Command: " + command + " Request: \n " + commandContent.encodePrettily());		
		eb.request("soile.umanager." + command,commandContent)
		.onSuccess( response ->
		{
			LOGGER.debug("Got a reply");
			if(response.body() instanceof JsonObject)
			{
				messageHandler.handle(((JsonObject)response.body()), routingContext);
			}
		}).onFailure( failure -> {
			if(failure instanceof ReplyException)
			{
				ReplyException err = (ReplyException)failure;
				routingContext.response()
				.setStatusCode(err.failureCode())
				.setStatusMessage(err.getMessage())
				.end();
			}
			else
			{
				routingContext.response()
				.setStatusCode(500)
				.end();
				LOGGER.error("Something went wrong when trying to register a new user");					
				LOGGER.error(failure);
			}
		});

	}

	/**
	 * Convenience function to check Access for a user to given resources
	 * @param user The user to check
	 * @param id the target ID
	 * @param requiredRole the Required roles for access
	 * @param requiredPermission the required permission for the access
	 * @param adminAllowed whether admin access is allowed
	 * @param authProvider the {@link MongoAuthorization} to extract authorization 
	 * @param IDAccessHandler the Access handler 
	 * @return A {@link Future} that is successfull if the user has access and fails if not.
	 */
	protected Future<Void> checkAccess(User user, String id, Roles requiredRole, PermissionType requiredPermission,
			boolean adminAllowed, MongoAuthorization authProvider, SoileIDBasedAuthorizationHandler IDAccessHandler)
	{
		Promise<Void> accessPromise = Promise.<Void>promise();
		authProvider.getAuthorizations(user)
		.onSuccess(Void -> {
			roleHandler.authorize(user, requiredRole)
			.onSuccess(acceptRole -> {
				if( id != null)
				{
					IDAccessHandler.authorize(user, id, adminAllowed, requiredPermission)
					.onSuccess(acceptID -> {
						// both role and permission checks are successfull.
						accessPromise.complete();
					})
					.onFailure(err -> accessPromise.fail(err));
				}
				else
				{
					accessPromise.complete();
				}
			})
			.onFailure(err -> accessPromise.fail(err));			
		})
		.onFailure(err -> {
			accessPromise.fail(new HttpException(500,err.getMessage()));
		});

		return accessPromise.future();
	}


	/**
	 * Simplified wrapper for the more general checkAccess function
	 * @param user the User to check
	 * @param requiredRole the Role required
	 * @param authProvider the {@link MongoAuthorization} for auth checks
	 * @return A {@link Future} that is successfull if the user has access and fails if not.
	 */
	protected Future<Void> checkAccess(User user, Roles requiredRole, MongoAuthorization authProvider)
	{
		return checkAccess(user,null,requiredRole,null,true,authProvider,null);
	}

	/**
	 * Convenience function that checks whether this is either the user itself, or an admin calling this command.
	 * Used when user modifications are done (which is nly allowed by the user or an admin
	 * @param user The current user
	 * @param modifiedUser the username of the modified user
	 * @param authProvider the {@link MongoAuthorization} for auth
	 * @return A {@link Future} that is successfull if the user has access and fails if not.
	 */
	protected Future<Void> checkSameUserOrAdmin(User user, String modifiedUser, MongoAuthorization authProvider)
	{		
		Promise<Void> accessPromise = Promise.promise();
		LOGGER.debug(user.principal().getString("username") + " // " + modifiedUser);
		if(user.principal().getString("username").equals(modifiedUser))
		{
			accessPromise.complete();
			return accessPromise.future();
		}
		else
		{
			checkAccess(user,Roles.Admin,authProvider)
			.onSuccess(success ->
			{
				accessPromise.complete();
			})
			.onFailure(err -> accessPromise.fail(err));
		}
		return accessPromise.future();
	}
	
	/**
	 * Check if a user has all permissions required for the given permissions array (the permissions array has object with a field target and for all of those 
	 * the user needs to have full permissions.
	 * @param user The user to check
	 * @param permissions the permissions required
	 * @param authProvider the auth provider that provides the authorizations to the user
	 * @param IDAccessHandler the Access handler that can build the authorizations.
	 * @return a succeeded {@link Future} if authorizated or a failed future if not. 
	 */
	protected Future<Void> checkUserHasAllPermissions(User user, JsonArray permissions, MongoAuthorization authProvider, SoileIDBasedAuthorizationHandler IDAccessHandler )
	{
		Promise<Void> accessPromise = Promise.<Void>promise();
		authProvider.getAuthorizations(user)
		.onSuccess(Void -> {			
			List<String> IDsToCheck = new LinkedList<>();
			for(int i = 0; i < permissions.size(); i++)
			{
				IDsToCheck.add(permissions.getJsonObject(i).getString("target"));
			}
			if(IDAccessHandler.checkMultipleFullAuthorizations(user, IDsToCheck, true))
			{
				accessPromise.complete();
			}
			else
			{
				accessPromise.fail(new HttpException(403, "Missing Full access to at least some of the provided objects"));
			}
			})		
		.onFailure(err -> {
			accessPromise.fail(new HttpException(500,err.getMessage()));
		});

		return accessPromise.future();
	}
	

}
