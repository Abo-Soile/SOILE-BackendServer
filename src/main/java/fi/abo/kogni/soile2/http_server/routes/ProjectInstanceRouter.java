package fi.abo.kogni.soile2.http_server.routes;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.auth.SoileIDBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileRoleBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.AccessProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

public class ProjectInstanceRouter  {

	ProjectInstanceHandler instanceHandler;
	SoileIDBasedAuthorizationHandler<AccessElement> instanceIDAccessHandler;
	SoileIDBasedAuthorizationHandler<AccessElement> projectIDAccessHandler;
	SoileRoleBasedAuthorizationHandler roleHandler;
	SoileAuthorization authorizationRertiever;
	TargetElementType instanceType = TargetElementType.INSTANCE;
	MongoAuthorization mongoAuth; 
	EventBus eb;

	public ProjectInstanceRouter(SoileAuthorization auth, EventBus eb, MongoClient client) {
		authorizationRertiever = auth;
		instanceHandler = new ProjectInstanceHandler(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder"), client, eb);
		mongoAuth = auth.getAuthorizationForOption(instanceType);
		roleHandler = new SoileRoleBasedAuthorizationHandler();
		instanceIDAccessHandler = new SoileIDBasedAuthorizationHandler<AccessElement>(AccessProjectInstance::new, client);
		projectIDAccessHandler = new SoileIDBasedAuthorizationHandler<AccessElement>(Project::new, client);
		this.eb = eb;
	}

	public void start(RoutingContext context)
	{
		String id = context.pathParam("id");
		String version = context.pathParam("version");		
		JsonObject projectData = context.body().asJsonObject().put("uuid", id).put("version", version).mergeIn(context.body().asJsonObject());
		// we need to check, whether the user has access to the actual project indicated.
		mongoAuth.getAuthorizations(context.user())
		.onSuccess(Void -> {
			projectIDAccessHandler.authorize(context.user(), id, false, PermissionType.READ)			
			.onSuccess(canCreate -> {
				instanceHandler.createProjectInstance(projectData)
				.onSuccess(instance -> {
					JsonObject permissionChange = new JsonObject().put("command", SoileConfigLoader.getCommunicationField("addCommand"))
																  .put("username", context.user().principal().getString("username"))
																  .put("permissions", new JsonObject().put("elementType", SoileConfigLoader.INSTANCE)
																		  							  .put("permissions", new JsonArray().add(new JsonObject().put("type", PermissionType.FULL.toString())
																		  									  												  .put("target", instance.getID()))));
					eb.request(SoileConfigLoader.getCommand(SoileConfigLoader.USERMGR_CFG,"permissionOrRoleChange"), permissionChange)
					.onSuccess(success -> {
						// instance was created, access was updated, everything worked fine. Now
						context.response().setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(new JsonObject().put("projectID",instance.getID()).encode());												
					})
					.onFailure(err -> context.fail(500, err));
				})
				.onFailure(err -> context.fail(500, err));
			})
			.onFailure(err -> context.fail(403, err));
		})
		.onFailure(err -> {
			context.fail(500,err);
		});		
	}
	
	public void getRunningProjectList(RoutingContext context)
	{				
		checkAccess(context.user(),null, Roles.Researcher,null,true)
		.onSuccess(Void -> 
		{
			authorizationRertiever.getGeneralPermissions(context.user(),instanceType)
			.onSuccess( permissions -> {
				instanceHandler.getProjectList(permissions)
				.onSuccess(elementList -> {	
					// this list needs to be filtered by access

					context.response()
					.setStatusCode(200)
					.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.end(elementList.encode());
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));	
		})
		.onFailure(err -> handleError(err, context));
	}
	
	
	private void handleError(Throwable err, RoutingContext context)
	{
		if(err instanceof ObjectDoesNotExist)
		{
			context.fail(410, err);
			return;
		}
		if(err instanceof HttpException)
		{
			HttpException e = (HttpException) err;
			context.fail(e.getStatusCode(),e);
			return;
		}

		context.fail(400, err);
	}

	
	protected Future<Void> checkAccess(User user, String id, Roles requiredRole, PermissionType requiredPermission, boolean adminAllowed)
	{
		Promise<Void> accessPromise = Promise.<Void>promise();
		mongoAuth.getAuthorizations(user)
		.onSuccess(Void -> {
			instanceIDAccessHandler.authorize(user, id, adminAllowed, requiredPermission)
			.onSuccess(acceptID -> {
				roleHandler.authorize(user, requiredRole)
				.onSuccess(acceptRole -> {
					// both role and permission checks are successfull.
					accessPromise.complete();
				})
				.onFailure(err -> accessPromise.fail(err));
			})
			.onFailure(err -> accessPromise.fail(err));
		})
		.onFailure(err -> {
			accessPromise.fail(new HttpException(500,err.getMessage()));
		});
		return accessPromise.future();
	}
	
	
}
