package fi.abo.kogni.soile2.http_server.authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.utils.CookieStrategy;
import fi.abo.kogni.soile2.http_server.authentication.utils.UserUtils;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.AuthenticationHandler;


public class SoileCookieRestoreHandler implements AuthenticationHandler {

	private final Vertx vertx;
	private MongoClient client;
	public static final Logger log = LogManager.getLogger(SoileCookieRestoreHandler.class); 
	
	public SoileCookieRestoreHandler(Vertx vertx, JsonObject config) {
		client = MongoClient.createShared(vertx, config.getJsonObject("db"));
		this.vertx = vertx;				  
	}

	public void authenticate(RoutingContext context) {
		log.debug("Checking session cookies");

		HttpServerRequest request = context.request();		  

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
											context.setUser(user);
											Session session = context.session();
											if (session != null) 
											{
												// the user has upgraded from unauthenticated to authenticated
												// session should be upgraded as recommended by owasp
												session.regenerateId();
											}
										}
										catch( Exception e)
										{
											log.warn("We found a valid session but could not create the user due to the following error: " + e.toString() );
										}
										context.next();
									} 

								});																				
							}
							else
							{
								//pass on, after we added the user (or not).
								context.next();
							}
						}
					});

				}
				catch(Exception e)
				{

				}
			}
			else
			{
				log.debug(" Cookie not found, resuming");
				context.next();
				return;
			}

		}
		else
		{
			log.debug("Context had user: \n" + context.user().principal().encodePrettily());
			context.next();
			return;
		}
	}


	@Override
	public void handle(RoutingContext ctx) {
		// TODO Auto-generated method stub
		log.debug("Received a request, checking if cookie is present");
		authenticate(ctx);
	}





}
