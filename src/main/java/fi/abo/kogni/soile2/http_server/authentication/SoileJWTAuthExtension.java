package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.http_server.authentication.utils.UserUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;

/**
 * This class is an extension of the JWT auth handler that needs to be merged via  ChainAuthHandler.
 * @author Thomas Pfau
 *
 */
public class SoileJWTAuthExtension
{

	MongoClient client;
			
	// This requires A user to actually already be in the context.
	public Future<User> authenticate(RoutingContext context ) {
		String username = context.user().principal().getString("username");
		JsonObject query = new JsonObject()
				.put(SoileConfigLoader.getdbField("usernameField"), username);
		HttpServerRequest request = context.request();		  
		Promise<User> userPromise = Promise.promise();
		if(context.user() == null)
		{
			userPromise.fail("No User could be derived from JWT Token");
		}
				// start async operations with a paused request.
		final boolean parseEnded = request.isEnded();
		if (!parseEnded) {
			request.pause();
		}		
		client.find(SoileConfigLoader.getdbProperty("userCollection"), query, dbRes -> 
		{
			//only resume the request, if we have finished loading everything here.							
			
			if (dbRes.succeeded()) 
			{	
				//retrieve the user from the database entry.
				try
				{
					User user = UserUtils.buildUserFromDBResult(dbRes.result(), username);					
					userPromise.complete(user);
					request.resume();
				}
				catch(Exception e)
				{
					// resume if handling failed
					request.resume();					
					userPromise.fail(new HttpException(401));
				}
			}
			else
			{
				// resume if the db Request failed.
				request.resume();				dbRes.cause().printStackTrace(System.out);
				userPromise.fail(new HttpException(401));									}

		});		
		return userPromise.future();
	}			
	
	public SimpleAuthenticationHandler getHandler()
	{
		SimpleAuthenticationHandler handler = SimpleAuthenticationHandler.create();
		handler.authenticate(this::authenticate);
		return handler;
	}
}
