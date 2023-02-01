package fi.abo.kogni.soile2.http_server.routes;

import java.util.List;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileIDBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileRoleBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.participant.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
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
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

public class ElementRouter<T extends ElementBase> extends SoileRouter{

	ElementManager<T> elementManager;
	EventBus eb;
	SoileIDBasedAuthorizationHandler IDAccessHandler;
	SoileRoleBasedAuthorizationHandler roleHandler;
	SoileAuthorization authorizationRertiever;
	MongoAuthorization mongoAuth; 
	public ElementRouter(ElementManager<T> manager, SoileAuthorization auth, EventBus eb, MongoClient client)
	{		
		authorizationRertiever = auth;
		T tempElement = manager.getElementSupplier().get();
		mongoAuth = auth.getAuthorizationForOption(tempElement.getElementType());
		roleHandler = new SoileRoleBasedAuthorizationHandler();		
		IDAccessHandler = new SoileIDBasedAuthorizationHandler(tempElement.getTargetCollection(), client);
		elementManager = manager;
		this.eb = eb;
	}


	public void getElement(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String elementVersion = params.pathParameter("version").getString();
		checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			elementManager.getAPIElementFromDB(elementID, elementVersion)
			.onSuccess(apielement -> {
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(apielement.getJson().encode());
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));

	}

	public void writeElement(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();		
		checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ_WRITE,true)
		.onSuccess(Void -> 
		{
			
			elementManager.getAPIElementFromJson(params.body().getJsonObject())
			.onSuccess(apiElement -> {
				elementManager.updateElement(apiElement)
				.onSuccess(version -> {
					context.response()
					.setStatusCode(200)
					.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.end(new JsonObject().put("version",version).encode());	
				})
				.onFailure(err -> handleError(err, context));								
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));

	}

	public void getElementList(RoutingContext context)
	{				
		//TODO: Add skip + limit + query here.		
		checkAccess(context.user(),null, Roles.Researcher,null,true)
		.onSuccess(Void -> 
		{
			authorizationRertiever.getGeneralPermissions(context.user(),elementManager.getElementSupplier().get().getElementType())
			.onSuccess( permissions -> {
				elementManager.getElementList(permissions)
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

	public void getVersionList(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();	
		checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			elementManager.getVersionListForElement(elementID)
			.onSuccess(versionList -> {			
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(versionList.encode());	
			})
			.onFailure(err -> handleError(err, context));	
		})
		.onFailure(err -> handleError(err, context));

	}

	public void getTagList(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();	
		checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			elementManager.getTagListForElement(elementID)
			.onSuccess(tagList -> {			
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(tagList.encode());	
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));

	}

	public void create(RoutingContext context)
	{		
		checkAccess(context.user(),null, Roles.Researcher,null,true)
		.onSuccess(Void -> 
		{
			List<String> nameParams = context.queryParam("name");
			List<String> typeParams = context.queryParam("codeType");
			String type;
			if(typeParams.size() == 0) {
				type = null; 
				}
			else{
				type = typeParams.get(0);
				if(!Task.allowedTypes.contains(type))
				{
					context.fail(400, new HttpException(400, "Invalid codeType Parameter"));
					return;
				}
			}
			if(nameParams.size() != 1)
			{
				context.fail(400, new HttpException(400, "Invalid name  query parameter"));
				return;
			}		
			elementManager.createElement(nameParams.get(0), type)
			.onSuccess(element -> {			
				JsonObject permissionChangeRequest = new JsonObject()
						.put("username", context.user().principal().getString("username"))
						.put("command", SoileConfigLoader.getStringProperty(SoileConfigLoader.COMMUNICATION_CFG, "addCommand"))
						.put("permissions", new JsonObject().put("type", getTypeID(element.getTypeID())))
						.put("target", new JsonArray().add(element.getUUID()));
				eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "permissionOrRoleChange"), permissionChangeRequest)
				.onSuccess( reply -> {
					elementManager.getAPIElementFromDB(element.getUUID(), element.getCurrentVersion())
					.onSuccess(apiElement -> {
						context.response()
						.setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(apiElement.getJson().encode());			
					}).onFailure(err -> handleError(err, context));	
				})
				.onFailure(err -> handleError(err, context));						
			}).onFailure(err -> handleError(err, context));		
		})
		.onFailure(err -> handleError(err, context));

	}

	private String getTypeID(String typeID)
	{
		switch(typeID)
		{
		case "E": return SoileConfigLoader.EXPERIMENT;
		case "P": return SoileConfigLoader.PROJECT;
		case "T": return SoileConfigLoader.TASK;
		default: return SoileConfigLoader.INSTANCE;			
		}
	}


	protected Future<Void> checkAccess(User user, String id, Roles requiredRole, PermissionType requiredPermission, boolean adminAllowed)
	{
		Promise<Void> accessPromise = Promise.<Void>promise();
		mongoAuth.getAuthorizations(user)
		.onSuccess(Void -> {
			IDAccessHandler.authorize(user, id, adminAllowed, requiredPermission)
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
