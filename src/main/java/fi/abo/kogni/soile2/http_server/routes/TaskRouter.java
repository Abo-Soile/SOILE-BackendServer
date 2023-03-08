package fi.abo.kogni.soile2.http_server.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.requestHandling.IDSpecificFileProvider;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

/**
 * The Task Router (of the element Router needs a couple of additional routes like Resource Posting/retrieval). 
 * @author Thomas Pfau
 *
 */
public class TaskRouter extends ElementRouter<Task> {

	private static final Logger LOGGER = LogManager.getLogger(ElementRouter.class);

	public TaskRouter(MongoClient client, IDSpecificFileProvider resManager, Vertx vertx, SoileAuthorization auth )
	{
		super(ElementManager.getTaskManager(client,vertx),auth, vertx.eventBus(), client);
	}		
	
	public void postResource(RoutingContext context)
	{				
		LOGGER.debug("Trying to post a resource");
		LOGGER.debug(context.pathParam("id") + "/" + context.pathParam("version") + "/" + context.pathParam("*") );				
		String elementID = context.pathParam("id");
		String version = context.pathParam("version");
		String filename = context.pathParam("*");
		if(filename.startsWith("lib/"))
		{
			handleError(new HttpException(400, "lib/ is a restricted path!"), context);
			return;
		}
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ_WRITE,true)
		.onSuccess(Void -> 
		{
			if(context.fileUploads().size() != 1)
			{
				handleError(new HttpException(400, "Missing or invalid file data, exactly one File expected"), context);
				return;
			}
			
			elementManager.handlePostFile(elementID,version,filename,context.fileUploads().get(0))
			.onSuccess(newversion -> {
				context.response().setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
				.end(new JsonObject().put("version", newversion).encode());
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));
	}
	
	public void getResource(RoutingContext context)
	{
		String elementID = context.pathParam("id");
		String version = context.pathParam("version");
		String filename = context.pathParam("*"); 	
		accessHandler.checkAccess(context.user(),elementID, Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			// Potentially update with fileProvider.
			elementManager.handleGetFile(elementID, version, filename )
			.onSuccess( datalakeFile -> {
				context.response().
				setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, datalakeFile.getFormat())
				.sendFile(datalakeFile.getAbsolutePath());
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));
	}
	
	//TODO: Implement Run methods.
	
	
	
	
	public void cleanup()
	{
		elementManager.cleanUp();
	}
}
