package fi.abo.kogni.soile2.http_server.routes;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.AccessHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

/**
 * Generic router for all different types of Elements. 
 * Descriptions for the individual calls can be obtained from the API document ( method names are 1:1 reflected by operation Names)
 * @author Thomas Pfau
 *
 * @param <T>
 */
public class ElementRouter<T extends ElementBase> extends SoileRouter{

	ElementManager<T> elementManager;
	EventBus eb;
	AccessHandler accessHandler;
	private static final Logger LOGGER = LogManager.getLogger(ElementRouter.class);

	public ElementRouter(ElementManager<T> manager, SoileAuthorization auth, EventBus eb, MongoClient client)
	{		
		super(auth,client);
		T tempElement = manager.getElementSupplier().get();		
		elementManager = manager;
		accessHandler = new AccessHandler(getAuthForType(tempElement.getElementType()),
										  getHandlerForType(tempElement.getElementType()),
										  roleHandler);
		this.eb = eb;
	}

	public void getElement(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String elementVersion = params.pathParameter("version").getString();
		LOGGER.debug("Got request for Element: " + elementID + "@" + elementVersion );
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			elementManager.getAPIElementFromDB(elementID, elementVersion)
			.onSuccess(apielement -> {
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(apielement.getAPIJson().encode());
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));

	}

	public void writeElement(RoutingContext context)
	{
		LOGGER.debug("Trying to update an element");
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String elementVersion = params.pathParameter("version").getString();
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ_WRITE,true)
		.onSuccess(Void -> 
		{
			
			elementManager.getAPIElementFromDB(elementID,elementVersion)
			.onSuccess(apiElement -> {
				JsonObject requestElement = params.body().getJsonObject();
				String tag = null;
				if(requestElement.containsKey("tag"))
				{
					tag = requestElement.getString("tag");
					// this should not be saved,
					requestElement.remove("tag");
				}
				apiElement.updateFromJson(params.body().getJsonObject());
				LOGGER.debug(apiElement.getGitJson().encodePrettily());				
				elementManager.updateElement(apiElement, tag)
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
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		Boolean full = params.queryParameter("full") == null ? false : params.queryParameter("full").getBoolean();
		if(full)
		{
			accessHandler.checkAccess(context.user(), null, Roles.Admin, null, true)
			.onSuccess(allowed -> {
				elementManager.getElementList(null)
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
		}
		else
		{
			accessHandler.checkAccess(context.user(),null, Roles.Researcher,null,true)
			.onSuccess(Void -> 
			{
				authorizationRertiever.getGeneralPermissions(context.user(),elementManager.getElementSupplier().get().getElementType())
				.onSuccess( permissions -> {
					LOGGER.debug(permissions.encodePrettily());
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
	}

	public void getTagForVersion(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String elementversion = params.pathParameter("version").getString();
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			elementManager.getTagForElementVersion(elementID, elementversion)
			.onSuccess(tag -> {			
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(new JsonObject().put("tag", tag).encode());	
			})
			.onFailure(err -> handleError(err, context));	
		})
		.onFailure(err -> handleError(err, context));

	}
	
	public void getVersionList(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();	
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
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
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
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
		LOGGER.debug("Received a request for creation");
		accessHandler.checkAccess(context.user(),null, Roles.Researcher,null,true)
		.onSuccess(Void -> 
		{
			LOGGER.debug("Request Access granted");
			List<String> nameParam = context.queryParam("name");
			List<String> typeParam = context.queryParam("codeType");
			List<String> versionParam = context.queryParam("codeVersion");
			String type = null;
			String version = null;
			if(nameParam.size() != 1) {
				LOGGER.debug("Invalid name parameter");
				handleError(new HttpException(400, "Must have exactly one codetype, codeVersion and name parameter"), context);
				return;
				}
			if( elementManager.getElementSupplier().get().getElementType() == TargetElementType.TASK  && (typeParam.size() != 1 || versionParam.size() != 1))
			{
				LOGGER.debug("Invalid CodeType/Version");
				handleError(new HttpException(400, "Must have exactly one codetype and codeVersion parameter"), context);
			}						
			else{
				
				if(elementManager.getElementSupplier().get().getElementType() == TargetElementType.TASK )
				{
					LOGGER.debug("Setting up Task data");
					type = typeParam.get(0);
					version = versionParam.get(0);
					if(!SoileConfigLoader.isValidTaskType(type, version))
					{
						LOGGER.debug("Innvalid Task Version/Code Type");
						// this could be a bit more explicit
						handleError(new HttpException(400, "Invalid codeType/version Parameter"), context);
						return;
					}
				}
			}
			LOGGER.debug("Trying to generate Element");
			elementManager.createElement(nameParam.get(0), type, version)
			.onSuccess(element -> {	
				LOGGER.debug("Element Created");
				JsonObject permissionChangeRequest = new JsonObject()
						.put("username", context.user().principal().getString("username"))
						.put("command", "add")
						.put("permissionsProperties", new JsonObject().put("elementType", element.getElementType().toString())
																	  .put("permissionSettings",new JsonArray().add(new JsonObject().put("target", element.getUUID())
																			  														.put("type", PermissionType.FULL.toString()))
																		  )
							);	
				LOGGER.debug("Requesting permission change");
				eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "permissionOrRoleChange"), permissionChangeRequest)
				.onSuccess( reply -> {
					LOGGER.debug("Permissions added to user for "+ nameParam + "/" + element.getUUID());
					elementManager.getAPIElementFromDB(element.getUUID(), element.getCurrentVersion())
					.onSuccess(apiElement -> {
						LOGGER.debug("Api element created");
						context.response()
						.setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(apiElement.getAPIJson().encode());			
					}).onFailure(err -> handleError(err, context));	
				})
				.onFailure(err -> handleError(err, context));						
			}).onFailure(err -> handleError(err, context));		
		})
		.onFailure(err -> handleError(err, context));

	}

}
