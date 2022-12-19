package fi.abo.kogni.soile2.http_server.routes;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.datamanagement.git.GitResourceManager;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.TaskDataHandler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

public class TaskRouter extends ElementRouter<Task> {

	TaskDataHandler dataHandler;
	
	public TaskRouter(GitManager gitManager, MongoClient client, GitResourceManager resManager, EventBus eb, SoileAuthorization auth )
	{
		super(ElementManager.getTaskManager(client,gitManager),auth, eb, client);
		dataHandler = new TaskDataHandler(resManager);
	}
	
	
	public void postResource(RoutingContext context)
	{
		checkAccess(context.user(),context.pathParam("id"), Roles.Researcher,PermissionType.READ_WRITE,true)
		.onSuccess(Void -> 
		{
			dataHandler.handlePostFile(context);
		})
		.onFailure(err -> handleError(err, context));
	}
	
	public void getResource(RoutingContext context)
	{
		checkAccess(context.user(),context.pathParam("id"), Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			dataHandler.handleGetFile(context);
		})
		.onFailure(err -> handleError(err, context));
	}
}
