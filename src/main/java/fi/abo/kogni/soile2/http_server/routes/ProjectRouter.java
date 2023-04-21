package fi.abo.kogni.soile2.http_server.routes;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class ProjectRouter extends ElementRouter<Project>{

	private static final Logger LOGGER = LogManager.getLogger(ProjectRouter.class);
	public ProjectRouter(ElementManager<Project> manager, SoileAuthorization auth, EventBus eb, MongoClient client) {
		super(manager, auth, eb, client);
		// TODO Auto-generated constructor stub
	}
	
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
