package fi.abo.kogni.soile2.http_server.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.utils.CookieStrategy;
import fi.abo.kogni.soile2.http_server.authentication.utils.UserUtils;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerInternal;

public class SoileCookieAuth {
	
	private final Vertx vertx;
	private MongoClient client;
	public static final Logger log = LogManager.getLogger(SoileCookieAuth.class);
	
	public SoileCookieAuth(Vertx vertx, MongoClient client)
	{
		this.client = client;
		this.vertx = vertx;
	}
		
	public Future<User> authenticate(RoutingContext context) {
		// TODO Auto-generated method stub
		log.debug("Checking session cookies");

		HttpServerRequest request = context.request();		  
		Promise<User> userPromise = Promise.promise();
		if(context.user() == null)
		{ 
			log.debug(" No user found Checking Cookie");
			Cookie sessionCookie = context.request().getCookie(SoileConfigLoader.getSessionProperty("sessionCookieID"));
			log.debug(context.request().cookieCount());
			for(Cookie c: context.request().cookies())
			{
				log.debug(c.toString());
			}
			if(sessionCookie != null)
			{
				// start async operations with a paused request.
				final boolean parseEnded = request.isEnded();
				if (!parseEnded) {
					request.pause();
				}
				try
				{
					String token = CookieStrategy.getTokenFromCookieContent(sessionCookie.getValue());
					String username = CookieStrategy.getUserNameFromCookieContent(sessionCookie.getValue());
					JsonObject command = new JsonObject().put(SoileCommUtils.getCommunicationField("usernameField"), username)
							.put(SoileCommUtils.getCommunicationField("sessionID"), token);

					log.debug("Trying to validate token:\n" + command.encodePrettily());
					vertx.eventBus()
					.request(SoileCommUtils.getUserEventBusCommand("checkUserSessionValid"),command,
							res ->
					{									
						if(res.succeeded())
						{
							// User token verified, create User and add it to session.
							JsonObject result = (JsonObject)res.result().body();
							if(SoileCommUtils.isResultSuccessFull(result)) 
							{
								JsonObject query = new JsonObject()
										.put(SoileConfigLoader.getdbField("usernameField"), username);
								client.find(SoileConfigLoader.getdbProperty("userCollection"), query, dbRes -> 
								{
									//only resume the request, if we have finished loading everything here.							
									request.resume();
									if (dbRes.succeeded()) 
									{	
										//retrieve the user from the database entry.
										try
										{
											User user = UserUtils.buildUserFromDBResult(dbRes.result(), username);
											user.principal().put("refreshCookie", true);
											userPromise.complete(user);
										}
										catch( Exception e)
										{
											log.error("We found a valid session but could not create the user due to the following error: " + e.toString() );
											userPromise.fail(new HttpException(401));
										}
									}
									else
									{
										log.error("We found a valid session but could not create the user due to the following error");
										userPromise.fail(new HttpException(401));									}

								});																				
							}
							else
							{
								//Fail if we could not add the user
								log.error("Session no longer valid , : " + res.cause().getMessage() );
								userPromise.fail(new HttpException(401));							}
						}
						else
						{
							//This would fail since it could not be authenticated.
							log.error("Session no longer valid , : " + res.cause().getMessage() );
							userPromise.fail(new HttpException(401));						}
					});

				}
				catch(Exception e)
				{
					log.error("Problem processing cookies: " + e.getMessage() );
					userPromise.fail(new HttpException(401));				}
			}
			else
			{
				log.debug(" Cookie not found, not authenticated!");
				userPromise.fail(new HttpException(401));
			}

		}
		else
		{
			userPromise.complete(context.user());
		}	
		return userPromise.future();
	}
	
}