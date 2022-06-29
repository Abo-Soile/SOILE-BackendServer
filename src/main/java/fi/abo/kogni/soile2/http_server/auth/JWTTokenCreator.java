package fi.abo.kogni.soile2.http_server.auth;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

public class JWTTokenCreator implements Handler<RoutingContext>{

	Vertx vertx;
	@Override
	public void handle(RoutingContext context) {
		if(context.user() == null)
		{
			// we have to have a user at this point, or we fail miserably...
			context.fail(new HttpException(401));
			return;
		}
		else
		{
			JWTAuth jwt =  SoileAuthenticationHandler.getAuthProvider(vertx);
			String jwtToken = jwt.generateToken(new JsonObject().put("username", context.user().principal().getString("username")));
			context.json(new JsonObject().put("token", jwtToken));
		}		
		
	}

}
