package fi.abo.kogni.soile2.http_server.routes;

import java.util.HashMap;

import fi.aalto.scicomp.mathparser.MathHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

/**
 * The Router for Project routes
 * @author Thomas Pfau
 *
 */
public class ProjectRouter extends ElementRouter<Project>{

	/**
	 * Defaul constructor
	 * @param manager The Project Manager to obtain projects
	 * @param auth the {@link SoileAuthorization} used to chck Auth
	 * @param eb the {@link EventBus} for communication
	 * @param client the {@link MongoClient} for DB access
	 */
	public ProjectRouter(ElementManager<Project> manager, SoileAuthorization auth, EventBus eb, MongoClient client) {
		super(manager, auth, eb, client);
		// TODO Auto-generated constructor stub
	}
	/**
	 * Check whether a filter applies to the given Context
	 * Context needs to contain the values and filter to check.  
	 * @param context The {@link RoutingContext} to check
	 */
	public void isFilterValid(RoutingContext context)
	{								
		accessHandler.checkAccess(context.user(),null, Roles.Researcher,null,true)
		.onSuccess(Void -> 
		{
			try
			{
				String expression = context.body().asJsonObject().getString("filter");
				JsonObject elements = context.body().asJsonObject().getJsonObject("parameters");
				HashMap<String,Double> parammap = new HashMap<>();
				for(String key: elements.fieldNames())
				{
					parammap.put(key, elements.getDouble(key));
				}
				Double Res = MathHandler.evaluate(expression, parammap);
				context.response().setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
				.end(new JsonObject().put("valid", true).put("value",  Res).encode());
			}
			catch(Exception e)
			{
				context.response().setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
				.end(new JsonObject().put("valid", false).put("error",  e.getMessage()).encode());
			}
		})
		.onFailure(err -> handleError(err, context));

	}

}
