package fi.abo.kogni.soile2.http_server.routes;

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
	SoileIDBasedAuthorizationHandler<AccessElement> taskIDAccessHandler;
	SoileIDBasedAuthorizationHandler<AccessElement> projectIDAccessHandler;
	SoileIDBasedAuthorizationHandler<AccessElement> experimentIDAccessHandler;
	SoileIDBasedAuthorizationHandler<AccessElement> instanceIDAccessHandler;

	private static Logger LOGGER = LogManager.getLogger(UserRouter.class.getName()); 


	public UserRouter(SoileAuthorization auth, Vertx vertx, MongoClient client)
	{
		authorizationRertiever = auth;
		projectAuth = auth.getAuthorizationForOption(TargetElementType.PROJECT);
		experimentAuth = auth.getAuthorizationForOption(TargetElementType.EXPERIMENT);
		taskAuth = auth.getAuthorizationForOption(TargetElementType.TASK);
		instanceAuth = auth.getAuthorizationForOption(TargetElementType.INSTANCE);		
		taskIDAccessHandler = new SoileIDBasedAuthorizationHandler<AccessElement>(Task::new, client);
		experimentIDAccessHandler = new SoileIDBasedAuthorizationHandler<AccessElement>(Experiment::new, client);
		projectIDAccessHandler = new SoileIDBasedAuthorizationHandler<AccessElement>(Project::new, client);
		instanceIDAccessHandler = new SoileIDBasedAuthorizationHandler<AccessElement>(AccessProjectInstance::new, client);

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
	public void registerUser(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();
		handleUserManagerCommand(context, "addUser", body, MessageResponseHandler.createDefaultHandler(201));
	}

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
				LOGGER.error(failure.getCause());
			}
		});

	}

	protected Future<Void> checkAccess(User user, String id, Roles requiredRole, PermissionType requiredPermission,
			boolean adminAllowed, MongoAuthorization authProvider, SoileIDBasedAuthorizationHandler<AccessElement> IDAccessHandler)
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
	
}
