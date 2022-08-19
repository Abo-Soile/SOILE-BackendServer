package fi.abo.kogni.soile2.utils;

import fi.abo.kogni.soile2.utils.impl.MessageResponseHandlerImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface MessageResponseHandler {
	
	public void handle(JsonObject responseData, RoutingContext context);
	
	public static MessageResponseHandler createDefaultHandler(int successReturnCode)
	{
		return new MessageResponseHandlerImpl(successReturnCode);
	}
}
