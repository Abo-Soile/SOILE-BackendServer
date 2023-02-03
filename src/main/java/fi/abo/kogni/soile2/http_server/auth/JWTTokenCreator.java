package fi.abo.kogni.soile2.http_server.auth;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

/**
 * A Simple Token generator for JWT.
 * @author Thomas Pfau
 *
 */
public class JWTTokenCreator{

	Vertx vertx;	
	SoileAuthenticationBuilder handler;
	
	public JWTTokenCreator(SoileAuthenticationBuilder handler, Vertx vertx)
	{
		this.vertx = vertx;
		this.handler = handler;
	}
	/**
	 * Create a Token if the context has an authenticated user. 
	 * @param context the Routing context for which to create a token.
	 * @return a Future of the token. This fails if there is no user in the Context.
	 */
	public Future<String> getToken(RoutingContext context) {
		Promise<String> tokenPromise = Promise.<String>promise();
		if(context.user() == null)
		{
			// we have to have a user at this point, or we fail miserably...
			tokenPromise.fail(new HttpException(401,"No user authenticated"));			
		}
		else
		{			
			JWTAuth jwt =  handler.getJWTAuthProvider(vertx);
			String jwtToken = jwt.generateToken(new JsonObject().put("username", context.user().principal().getString("username")));			
			tokenPromise.complete(jwtToken);
		}		
		return tokenPromise.future();
	}

}
