package fi.abo.kogni.soile2.http_server.routes;

import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

public class SoileRouter {

	
	/**
	 * Default handling of errors. 
	 * @param err
	 * @param context
	 */
	void handleError(Throwable err, RoutingContext context)
	{
		if(err instanceof ObjectDoesNotExist)
		{
			context.fail(410, err);
			return;
		}
		if(err instanceof HttpException)
		{
			HttpException e = (HttpException) err;
			context.fail(e.getStatusCode(),e);
			return;
		}
		if(err.getCause() instanceof ReplyException)
		{
			ReplyException rerr = (ReplyException)err.getCause();
			context.fail(rerr.failureCode(),rerr);
			return;
		}
		context.fail(400, err);
	}
}
