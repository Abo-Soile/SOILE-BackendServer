package fi.abo.kogni.soile2.http_server.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import io.vertx.core.eventbus.ReplyException;
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
		LOGGER.debug(err);
		if(err instanceof ElementNameExistException)
		{
			context.fail(409);
			return;
		}
		if(err instanceof ObjectDoesNotExist)
		{
			context.fail(410);
			return;
		}
		if(err instanceof HttpException)
		{
			HttpException e = (HttpException) err;
			context.fail(e.getStatusCode());
			return;
		}
		if(err.getCause() instanceof ReplyException)
		{
			ReplyException rerr = (ReplyException)err.getCause();
			context.fail(rerr.failureCode());
			return;
		}
		context.fail(400, err);
	}
}
