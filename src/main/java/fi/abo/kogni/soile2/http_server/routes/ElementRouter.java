package fi.abo.kogni.soile2.http_server.routes;

import java.util.List;

import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

public class ElementRouter<T extends ElementBase> {
	
	ElementManager<T> elementManager;
	EventBus eb;
	public ElementRouter(ElementManager<T> manager, EventBus eb)
	{
		elementManager = manager;
		this.eb = eb;
	}
	
	public void getElement(RoutingContext context)
	{
		elementManager.getAPIElementFromDB(context.pathParam("id"), context.pathParam("version"))
		.onSuccess(element -> {
			context.response()
			.setStatusCode(200)
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.end(element.getJson().encode());
		})
		.onFailure(err -> handleError(err, context));				
	}
	
	public void writeElement(RoutingContext context)
	{
		elementManager.getAPIElementFromJson(context.body().asJsonObject())
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
	}
	
	public void getElementList(RoutingContext context)
	{				
		elementManager.getElementList()
		.onSuccess(elementList -> {	
			// this list needs to be filtered by access
			
			context.response()
			.setStatusCode(200)
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.end(elementList.encode());	
		})
		.onFailure(err -> handleError(err, context));								
	}
	
	public void getVersionList(RoutingContext context)
	{		
		elementManager.getVersionListForElement(context.pathParam("id"))
		.onSuccess(versionList -> {			
			context.response()
			.setStatusCode(200)
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.end(versionList.encode());	
		})
		.onFailure(err -> handleError(err, context));								
	}
	
	public void getTagList(RoutingContext context)
	{		
		elementManager.getTagListForElement(context.pathParam("id"))
		.onSuccess(tagList -> {			
			context.response()
			.setStatusCode(200)
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.end(tagList.encode());	
		})
		.onFailure(err -> handleError(err, context));								
	}
	
	public void create(RoutingContext context)
	{		
		List<String> nameParams = context.queryParam("name");
		if(nameParams.size() != 1)
		{
			context.fail(400, new HttpException(400, "Invalid name  query parameter"));
			return;
		}		
		elementManager.createElement(nameParams.get(0))
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
	}
	
	
	private void handleError(Throwable err, RoutingContext context)
	{
		if(err instanceof ObjectDoesNotExist)
		{
			context.fail(410, err);
		}
		else
		{
			context.fail(400, err);
		}
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
	
}
