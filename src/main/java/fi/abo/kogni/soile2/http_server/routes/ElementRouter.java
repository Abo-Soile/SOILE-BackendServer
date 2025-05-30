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
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
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
 * @param <T> The base for this Element Router (i.e. the {@link ElementBase} the router is targeting)
 */
public class ElementRouter<T extends ElementBase> extends SoileRouter{

	ElementManager<T> elementManager;
	EventBus eb;
	AccessHandler accessHandler;
	private static final Logger LOGGER = LogManager.getLogger(ElementRouter.class);
	
	/**
	 * Basic constructor
	 * @param manager The {@link ElementManager} for the target resource
	 * @param auth the used {@link SoileAuthorization} mechanism 
	 * @param eb {@link Vertx} {@link EventBus}
	 * @param client the {@link MongoClient} used by the router
	 */
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
	
	/**
	 * Get the element defined in the context (path parameter ids and version)
	 * @param context The {@link RoutingContext} to look up the elements IDs
	 */
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
	/**
	 * Delete the element specified in the context (id path parameter
	 * @param context The {@link RoutingContext} to extract the id from
	 */
	public void deleteElement(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		LOGGER.debug("Got Delete Request for Element: " + elementID );
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.FULL,false)
		.onSuccess(Void -> 
		{
			elementManager.deleteElement(elementID)
			.onSuccess(success -> {
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end();
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));

	}
	/**
	 * Write an element as specified in the context (data, id and version)
	 * @param context The {@link RoutingContext} to extract the data from
	 */
	public void writeElement(RoutingContext context)
	{
		LOGGER.debug("Trying to update an element");
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String elementVersion = params.pathParameter("version").getString();
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ_WRITE,true)
		.compose(allowed -> { return checkVersionAndID(elementID, elementVersion); })
		.onSuccess(Void -> 
		{

			elementManager.getAPIElementFromDB(elementID,elementVersion)
			.onSuccess(apiElement -> {
				LOGGER.debug(apiElement.getAPIJson().encodePrettily());
				JsonObject requestElement = params.body().getJsonObject();
				LOGGER.debug(requestElement.encodePrettily());
				String tag = null;
				if(requestElement.containsKey("tag"))
				{
					tag = requestElement.getString("tag");
					// this should not be saved,
					requestElement.remove("tag");
				}
				apiElement.updateFromJson(params.body().getJsonObject());
				LOGGER.debug(apiElement.getAPIJson().encodePrettily());				
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
	/**
	 * Get a List of Elements of the type represented by this Router
	 * The "full" parameter from the queryParameters indicates, whether to return everything obtainable
	 * @param context The {@link RoutingContext} to get the data. 
	 */
	public void getElementList(RoutingContext context)
	{				
		//TODO: Add skip + limit + query here.		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		Boolean full = params.queryParameter("full") == null ? false : params.queryParameter("full").getBoolean();
		if(full)
		{
			accessHandler.checkAccess(context.user(), null, Roles.Researcher, null, true)
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
	/**
	 * Get the Tag for the specified version of the Element with the specified ID
	 * @param context The {@link RoutingContext} to obtain the data from
	 */
	public void getTagForVersion(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String elementVersion = params.pathParameter("version").getString();
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
		.compose(allowed -> { return checkVersionAndID(elementID, elementVersion); })
		.onSuccess(Void -> 
		{
			elementManager.getTagForElementVersion(elementID, elementVersion)
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
	/**
	 * Get a list of version for a specified Element 
	 * @param context The {@link RoutingContext} specifying the Element to get versions for
	 */
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
	
	/**
	 * Remove a tag from an element specified in the given Context
	 * @param context The {@link RoutingContext} to extract the data from
	 */
	public void removeTagsFromElement(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();	
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.FULL,false)
		.onSuccess(Void -> 
		{			
			LOGGER.debug("Handling tag removal");
			elementManager.removeTagsFromElement(elementID, context.body().asJsonArray())
			.onSuccess(removed -> {			
				context.response()
				.setStatusCode(200)				
				.end();	
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));

	}
	/**
	 * Add a Tag to a specific version of the element as specified in the given {@link RoutingContext} 
	 * @param context The {@link RoutingContext} to extract data from
	 */
	public void addTagToVersion(RoutingContext context)
	{		
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String elementVersion = params.pathParameter("version").getString();		
		String newTag = context.body().asJsonObject().getString("name");
		if(newTag == null)
		{
			handleError(new HttpException(400, "Invalid Tag value"),context);
			return;
		}
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ_WRITE,true)
		.compose(allowed -> { return checkVersionAndID(elementID, elementVersion); })
		.onSuccess(Void -> 
		{			
			LOGGER.debug("Handling tag addition");
			elementManager.addTagToVersion(elementID, elementVersion, newTag)
			.onSuccess(added -> {			
				context.response()
				.setStatusCode(200)				
				.end();	
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));

	}
	
	/**
	 * Create an element of the type represented by this Router
	 * @param context The {@link RoutingContext}
	 */
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
				eb.request("soile.umanager.permissionOrRoleChange", permissionChangeRequest)
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
	
	Future<Void> checkVersionAndID(String id, String version)
	{
		Promise<Void> existPromise = Promise.promise();
		elementManager.doesRepoAtVersionExist(id, version)
		.onSuccess(exists -> {
			if(exists)
			{
				LOGGER.debug("Version existed");
				existPromise.complete();
			}
			else
			{
				LOGGER.debug("Version did not exist");
				existPromise.fail(new HttpException(404));
			}
		})
		.onFailure(err -> existPromise.fail(err));
		
		return existPromise.future();
	}

}
