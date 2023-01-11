package fi.abo.kogni.soile2.utils.impl;

import fi.abo.kogni.soile2.utils.MessageResponseHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;


/**
 * A SOILE specific {@link MessageResponseHandler}. 
 * Replies to responses that has {@link SoileCommUtils.RESULTFIELD} being set to {@link SoileCommUtils.SUCCESS}
 * checks, whether the "data" field is a Json or String, and if, resonds to the context with this information,
 * indicating the correct content-type.
 * @author Thomas Pfau
 *
 */
public class MessageResponseHandlerImpl implements MessageResponseHandler{

	private int successStatusCode;
	public MessageResponseHandlerImpl(int successStatusCode)
	{
		this.successStatusCode = successStatusCode;
	}
	@Override
	public void handle(JsonObject responseData, RoutingContext context) {
		if(responseData.getString(SoileCommUtils.RESULTFIELD).equals(SoileCommUtils.SUCCESS))
		{
			context.response().setStatusCode(successStatusCode);
			Object data = responseData.getValue(SoileCommUtils.DATAFIELD, null);
			if(data != null)
			{
				if(data instanceof JsonObject)
				{
					context.response().putHeader("content-type","application/json");
					context.response().write(responseData.getJsonObject(SoileCommUtils.DATAFIELD).encode());
				}
				else if ( data instanceof String)
				{
					context.response().putHeader("content-type","text/plain");
					context.response().write(responseData.getString(SoileCommUtils.DATAFIELD));				
				}
				else if ( data instanceof JsonArray)
				{
					context.response().putHeader("content-type","application/json");
					context.response().write(responseData.getJsonArray(SoileCommUtils.DATAFIELD).encode());				
				}
			}

		}
		
		
	}

}
