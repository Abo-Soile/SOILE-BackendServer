package fi.abo.kogni.soile2.utils.impl;

import fi.abo.kogni.soile2.utils.MessageResponseHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class MessageResponseHandlerImpl implements MessageResponseHandler{

	private int successStatusCode;
	public MessageResponseHandlerImpl(int successStatusCode)
	{
		this.successStatusCode = successStatusCode;
	}
	@Override
	public void handle(JsonObject responseData, RoutingContext context) {
		// TODO Auto-generated method stub
		if(responseData.getString(SoileCommUtils.RESULTFIELD).equals(SoileCommUtils.SUCCESS))
		{
			context.response().setStatusCode(successStatusCode);
			Object data = responseData.getValue("data", null);
			if(data != null)
			{
				if(data instanceof JsonObject)
				{
					context.response().putHeader("content-type","application/json");
					context.response().write(responseData.getJsonObject("data").encode());
				}
				else if ( data instanceof String)
				{
					context.response().putHeader("content-type","text/plain");
					context.response().write(responseData.getString("data"));				
				}
			}
		}
		
		
	}

}
