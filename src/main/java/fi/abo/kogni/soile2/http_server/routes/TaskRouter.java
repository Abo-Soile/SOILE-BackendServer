package fi.abo.kogni.soile2.http_server.routes;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.datamanagement.git.ResourceManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.TaskDataHandler;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

public class TaskRouter extends ElementRouter<Task> {

	TaskDataHandler dataHandler;
	
	public TaskRouter(GitManager gitManager, MongoClient client, ResourceManager resManager)
	{
		super(ElementManager.getTaskManager(client,gitManager));
		dataHandler = new TaskDataHandler(resManager);
	}
	
	
	public void postResource(RoutingContext context)
	{
		dataHandler.handlePostFile(context);
	}
	
	public void getResource(RoutingContext context)
	{
		dataHandler.handleGetFile(context);
	}
}
