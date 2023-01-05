package fi.abo.kogni.soile2.http_server.routes;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.auth.SoileIDBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileRoleBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import fi.abo.kogni.soile2.projecthandling.projectElements.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.AccessProjectInstance;
import fi.abo.kogni.soile2.utils.MessageResponseHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
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
	SoileAuthorization authorizationRertiever;
	SoileRoleBasedAuthorizationHandler roleHandler;
	MongoAuthorization projectAuth;
	MongoAuthorization experimentAuth;
	MongoAuthorization taskAuth;
	MongoAuthorization instanceAuth;
	SoileIDBasedAuthorizationHandler taskIDAccessHandler;
	SoileIDBasedAuthorizationHandler projectIDAccessHandler;
	SoileIDBasedAuthorizationHandler experimentIDAccessHandler;
	SoileIDBasedAuthorizationHandler instanceIDAccessHandler;

	private static Logger LOGGER = LogManager.getLogger(UserRouter.class.getName()); 


	public UserRouter(SoileAuthorization auth, Vertx vertx, MongoClient client)
	{
		authorizationRertiever = auth;
		projectAuth = auth.getAuthorizationForOption(TargetElementType.PROJECT);
		experimentAuth = auth.getAuthorizationForOption(TargetElementType.EXPERIMENT);
		taskAuth = auth.getAuthorizationForOption(TargetElementType.TASK);
		instanceAuth = auth.getAuthorizationForOption(TargetElementType.INSTANCE);		
		taskIDAccessHandler = new SoileIDBasedAuthorizationHandler(new Task().getTargetCollection(), client);
		experimentIDAccessHandler = new SoileIDBasedAuthorizationHandler(new Experiment().getTargetCollection(), client);
		projectIDAccessHandler = new SoileIDBasedAuthorizationHandler(new Project().getTargetCollection(), client);
		instanceIDAccessHandler = new SoileIDBasedAuthorizationHandler(new AccessProjectInstance().getTargetCollection(), client);

		roleHandler = new SoileRoleBasedAuthorizationHandler();
		this.eb = vertx.eventBus();
	}

	public void listUsers(RoutingContext context)
	{
		// Admins get almost full info. 
		checkAccess(context.user(),Roles.Admin, experimentAuth)
		.onSuccess(authed -> {
				RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
				JsonObject body = params.body().getJsonObject();
				body.put("skip", params.queryParameter("skip")).put("limit", params.queryParameter("limit")).put("query", params.queryParameter("query"));
				handleUserManagerCommand(context, "listUsers", body, MessageResponseHandler.createDefaultHandler(200));
		})
		.onFailure(err -> {
					checkAccess(context.user(), Roles.Researcher, experimentAuth)
					.onSuccess(authorized -> {
						RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
						JsonObject body = params.body().getJsonObject();
						body.put("namesOnly", true).put("skip", params.queryParameter("skip")).put("limit", params.queryParameter("limit")).put("query", params.queryParameter("query"));
						handleUserManagerCommand(context, "listUsers", body, MessageResponseHandler.createDefaultHandler(200));
					})
					.onFailure(err2 -> handleError(err2, context));
					
		});
		
		
	}
	
	/**
	 * Register a user according to the API command
	 * @param context
	 */
	public void registerUser(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();
		handleUserManagerCommand(context, "addUser", body, MessageResponseHandler.createDefaultHandler(201));
	}

	/**
	 * Route for user deletion. 
	 * TODO: If deleteFiles is false/unassigned report a unique id that can be used to reassociate data and allow deletion of the data.  
	 * @param context
	 */
	public void removeUser(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();		
		Boolean deleteFiles = body.getBoolean("deleteFiles");
		String username = body.getString("username");
		// for roles, the Authorization provider doesn't make a difference.
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {
			if(deleteFiles)
			{
				eb.request(SoileConfigLoader.getCommand(SoileConfigLoader.USERMGR_CFG, "getParticipantsForUser"),new JsonObject().put("username", username))
				.onSuccess(response -> {
					JsonArray participants = (JsonArray) response.body();
					eb.request("soile.participant.delete", participants)
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
	 * @param context
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
		if(body.containsKey("userRole") )
		{
			userData.put("userRole", body.getString("userRole"));
		}				
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {
			// we are setting a role.
			if(body.containsKey("userRole"))
			{
				checkAccess(context.user(), Roles.Admin, experimentAuth)
				.onSuccess(isAdmin -> {
					handleUserManagerCommand(context, "setUserInfo", userData, MessageResponseHandler.createDefaultHandler(200));
				});			
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
	 * @param context
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
	 * @param context
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
	 * @param context
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
		userData.put("permissions", permissionProps);		
		checkUserHasAllPermissions(context.user(),permissionProps.getJsonArray("permissionsSettings"), getAuthForType(body.getString("ElementType")),getHandlerForType(body.getString("ElementType")))
		.onSuccess(allowed -> {			
			handleUserManagerCommand(context, "permissionOrRoleChange", userData, MessageResponseHandler.createDefaultHandler(200));			
		})
		.onFailure(err -> handleError(err, context));
	}
	
	/**
	 * Set the permissions of a specified user.
	 * @param context
	 */
	public void permissionOrRoleRequest(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();				
		String username = body.getString("username");		
		JsonObject userData = new JsonObject();
		userData.put("username", username);			
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {			
			handleUserManagerCommand(context, "getAccessRequest", userData, MessageResponseHandler.createDefaultHandler(200));			
		})
		.onFailure(err -> handleError(err, context));
	}
	
	/**
	 * Get the use userinfo
	 * @param context
	 */
	public void getUserInfo(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();				
		String username = body.getString("username");
		JsonObject userData = new JsonObject();
		userData.put("username", username);
		checkSameUserOrAdmin(context.user(), username, experimentAuth)
		.onSuccess(allowed -> {
			// we are setting a role.
			handleUserManagerCommand(context, "getUserInfo", userData, MessageResponseHandler.createDefaultHandler(200));				
		})
		.onFailure(err -> handleError(err, context));
	}
	
	
	public void createUser(RoutingContext context)
	{
		registerUser(context);
	}

	void handleUserManagerCommand(RoutingContext routingContext, String command, JsonObject commandContent, MessageResponseHandler messageHandler)
	{		
		eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, command),commandContent).onSuccess( response ->
		{
			if(response.body() instanceof JsonObject)
			{
				messageHandler.handle(((JsonObject)response.body()), routingContext);
				routingContext.response().end();
			}
		}).onFailure( failure -> {
			if(failure.getCause() instanceof ReplyException)
			{
				ReplyException err = (ReplyException)failure.getCause();
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


	protected Future<Void> checkAccess(User user, Roles requiredRole, MongoAuthorization authProvider)
	{
		return checkAccess(user,null,requiredRole,null,true,authProvider,null);
	}

	// Checks whether this is either the user itself, or an admin calling this command.
	protected Future<Void> checkSameUserOrAdmin(User user, String modifiedUser, MongoAuthorization authProvider)
	{		
		Promise<Void> accessPromise = Promise.promise();
		
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
			});
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
	 * @return a succeeded future if authorizated or a failed future if not. 
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
	
	SoileIDBasedAuthorizationHandler getHandlerForType(String type)
	{		
		switch(type)
		{
			case SoileConfigLoader.PROJECT: return projectIDAccessHandler;
			case SoileConfigLoader.EXPERIMENT: return experimentIDAccessHandler;
			case SoileConfigLoader.TASK: return taskIDAccessHandler;
			case SoileConfigLoader.INSTANCE: return instanceIDAccessHandler;
			default: return taskIDAccessHandler;
		}
	}
	MongoAuthorization getAuthForType(String type)
	{		
		switch(type)
		{
			case SoileConfigLoader.PROJECT: return projectAuth;
			case SoileConfigLoader.EXPERIMENT: return experimentAuth;
			case SoileConfigLoader.TASK: return taskAuth;
			case SoileConfigLoader.INSTANCE: return instanceAuth;
			default: return taskAuth;
		}
	}
}
