package fi.abo.kogni.soile2.http_server.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.auth.SoileIDBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileRoleBasedAuthorizationHandler;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.AccessProjectInstance;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

/**
 * Base class which provides Error Handling for Soile Routers handling some common errors.
 * @author Thomas Pfau
 *
 */
public class SoileRouter {

	private static final Logger LOGGER = LogManager.getLogger(ElementRouter.class);

	protected MongoAuthorization projectAuth;
	protected MongoAuthorization experimentAuth;
	protected MongoAuthorization taskAuth;
	protected MongoAuthorization instanceAuth;
	protected SoileIDBasedAuthorizationHandler taskIDAccessHandler;
	protected SoileIDBasedAuthorizationHandler projectIDAccessHandler;
	protected SoileIDBasedAuthorizationHandler experimentIDAccessHandler;
	protected SoileIDBasedAuthorizationHandler instanceIDAccessHandler;
	protected SoileAuthorization authorizationRertiever;
	protected SoileRoleBasedAuthorizationHandler roleHandler;
	
	
	public SoileRouter(SoileAuthorization auth, MongoClient client)
	{
		authorizationRertiever = auth;
		projectAuth = auth.getAuthorizationForOption(TargetElementType.PROJECT);
		experimentAuth = auth.getAuthorizationForOption(TargetElementType.EXPERIMENT);
		taskAuth = auth.getAuthorizationForOption(TargetElementType.TASK);
		instanceAuth = auth.getAuthorizationForOption(TargetElementType.INSTANCE);		
		taskIDAccessHandler = new SoileIDBasedAuthorizationHandler(new Task().getTargetCollection(), client);
		experimentIDAccessHandler = new SoileIDBasedAuthorizationHandler(new Experiment().getTargetCollection(), client);
		projectIDAccessHandler = new SoileIDBasedAuthorizationHandler(new Project().getTargetCollection(), client);
		instanceIDAccessHandler = new SoileIDBasedAuthorizationHandler(new AccessProjectInstance().getTargetCollection(), client);

		roleHandler = new SoileRoleBasedAuthorizationHandler();
	}
	
	/**
	 * Default handling of errors. 
	 * @param err
	 * @param context
	 */
	void handleError(Throwable err, RoutingContext context)
	{
		LOGGER.info(err);
		err.printStackTrace(System.out);;
		if(err instanceof ElementNameExistException)
		{	
			sendError(context, 409, err.getMessage());			
			return;
		}
		if(err instanceof ObjectDoesNotExist)
		{
			sendError(context, 404, err.getMessage());			
			return;
		}
		if(err instanceof HttpException)
		{
			HttpException e = (HttpException) err;
			sendError(context, e.getStatusCode(), err.getMessage());						
			return;
		}
		if(err.getCause() instanceof ReplyException)
		{
			ReplyException rerr = (ReplyException)err.getCause();
			sendError(context, rerr.failureCode(), rerr.getMessage());			
			return;
		}
		if(err instanceof ReplyException)
		{	
			ReplyException rerr = (ReplyException)err;
			sendError(context, rerr.failureCode(), rerr.getMessage());			
			return;
		}
		sendError(context, 400, err.getMessage());
	}
	
	void sendError(RoutingContext context, int code, String message)
	{		
		LOGGER.error("Request errored. Returning code" + code + " with message " + message);
		context.response()
		.setStatusCode(code)
		.end(message);
	}
	
	protected SoileIDBasedAuthorizationHandler getHandlerForType(TargetElementType type)
	{
		switch(type)
		{
			case PROJECT: return projectIDAccessHandler;
			case EXPERIMENT: return experimentIDAccessHandler;
			case TASK: return taskIDAccessHandler;
			case INSTANCE: return instanceIDAccessHandler;
			default: return taskIDAccessHandler;
		}
		
	}
	
	protected SoileIDBasedAuthorizationHandler getHandlerForType(String type)
	{
		return getHandlerForType(TargetElementType.valueOf(type));		
	}
	
	protected MongoAuthorization getAuthForType(TargetElementType type)
	{		
		switch(type)
		{
			case PROJECT: return projectAuth;
			case EXPERIMENT: return experimentAuth;
			case TASK: return taskAuth;
			case INSTANCE: return instanceAuth;
			default: return taskAuth;
		}
	}
	
	protected MongoAuthorization getAuthForType(String type)
	{		
		return getAuthForType(TargetElementType.valueOf(type));
	}
	
}
