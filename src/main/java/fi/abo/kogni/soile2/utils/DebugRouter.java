package fi.abo.kogni.soile2.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A Router for debugging puroposes Will log some information about received requests
 * @author Thomas Pfau
 *
 */
public class DebugRouter implements Handler<RoutingContext>{
	private static final Logger LOGGER = LogManager.getLogger(DebugRouter.class);

	@Override
	public void handle(RoutingContext event) {
		LOGGER.debug("Request URL: " + event.request().absoluteURI());
		LOGGER.debug("Request is ssl: " + event.request().isSSL());
		LOGGER.debug("Request Method is : " + event.request().method());
		if(event.body().available())
		{
			try {
				LOGGER.debug(event.body().asJsonObject(200));
			}
			catch(Exception e)
			{
				LOGGER.debug(event.body().asString().substring(0, Math.min(event.body().asString().length()-1,200)));
			}
		}
		event.next();
	}

}
