package fi.abo.kogni.soile2.http_server.routes;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

public class ProjectInstanceRouter  {

	ProjectInstanceHandler instanceHandler;
	public ProjectInstanceRouter(MongoClient client, EventBus bus) {		
		instanceHandler = new ProjectInstanceHandler(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder"), client, bus);
	}
	
	
	public void start(RoutingContext context)
	{
		String id = context.pathParam("id");
		String version = context.pathParam("version");
		JsonObject projectData = context.body().asJsonObject().put("uuid", id).put("version", version); 
		instanceHandler.createProjectInstance(projectData)
		.onSuccess(instance -> {
			
		})
		.onFailure(err -> context.fail(err));
	}

	
	
}
