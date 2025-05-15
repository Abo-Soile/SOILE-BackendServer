package fi.abo.kogni.soile2.utils;

import fi.abo.kogni.soile2.utils.impl.MessageResponseHandlerImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A Handler for default Replies to the context.
 * Assumes, that the response type is simple text or Json and will answer the context accordingly 
 * Does nothing if not successfull.
 * @author Thomas Pfau
 *
 */
public interface MessageResponseHandler {

	
	/**
	 * Reply to the context request with the given information in responseData. 
	 * Expects a certain field to have a specific value for a default message to be replied. 
	 * @param responseData The data to process
	 * @param context the context to reply to
	 */
	public void handle(JsonObject responseData, RoutingContext context);
	
	/**
	 * Create a new handler indicate the success status code.
	 * @param successReturnCode the return code upon success
	 * @return A {@link MessageResponseHandler} that uses the default response code
	 */
	public static MessageResponseHandler createDefaultHandler(int successReturnCode)
	{
		return new MessageResponseHandlerImpl(successReturnCode);
	}
}
