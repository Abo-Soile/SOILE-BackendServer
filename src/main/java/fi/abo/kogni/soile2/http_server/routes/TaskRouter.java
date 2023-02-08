package fi.abo.kogni.soile2.http_server.routes;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeResourceManager;
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
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

/**
 * The Task Router (of the element Router needs a couple of additional routes like Resource Posting/retrieval). 
 * @author Thomas Pfau
 *
 */
public class TaskRouter extends ElementRouter<Task> {

	private IDSpecificFileProvider fileProvider;
	public TaskRouter(MongoClient client, IDSpecificFileProvider resManager, Vertx vertx, SoileAuthorization auth )
	{
		super(ElementManager.getTaskManager(client,vertx),auth, vertx.eventBus(), client);
		fileProvider = resManager;
	}		
	
	public void postResource(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String version = params.pathParameter("version").getString();
		String filename = params.pathParameter("*").getString(); 		
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
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String elementID = params.pathParameter("id").getString();
		String version = params.pathParameter("version").getString();		
		String filename = params.pathParameter("*").getString(); 		
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
