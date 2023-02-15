package fi.abo.kogni.soile2.http_server.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

/**
 * Base class which provides Error Handling for Soile Routers handling some common errors.
 * @author Thomas Pfau
 *
 */
public class SoileRouter {

	private static final Logger LOGGER = LogManager.getLogger(ElementRouter.class);

	/**
	 * Default handling of errors. 
	 * @param err
	 * @param context
	 */
	void handleError(Throwable err, RoutingContext context)
	{
		LOGGER.info(err);
		err.printStackTrace(System.out);
		if(err instanceof ElementNameExistException)
		{	
			sendError(context, 409, err.getMessage());			
			return;
		}
		if(err instanceof ObjectDoesNotExist)
		{
			sendError(context, 410, err.getMessage());			
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
		
		sendError(context, 400, err.getMessage());
	}
	
	void sendError(RoutingContext context, int code, String message)
	{		
		context.response()
		.setStatusCode(code)
		.end(message);
	}
	
}
