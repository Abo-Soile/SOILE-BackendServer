package fi.abo.kogni.soile2.http_server.auth;

import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.security.SecureRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.utils.CookieStrategy;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

public class SoileCookieCreationHandler {
	
	private final SecureRandom random = new SecureRandom();
	static final Logger LOGGER = LogManager.getLogger(SoileCookieCreationHandler.class);
	private EventBus eb;
	public SoileCookieCreationHandler(EventBus bus) {
		eb = bus;
	}
	
	public Future<Void> updateCookie(RoutingContext ctx, User user)
	{		
		// if we get an explicit user we use that one, otherwise we try to retrieve it from the context.
		User currentuser = user == null ? user : ctx.user();
		Promise<Void> finishedCookie = Promise.<Void>promise();
				LOGGER.debug("Handling a request");
				if(user != null)
				{
					// there is a cookie that we need to invalidate, and a new one that we need to put in.
					LOGGER.debug("Found a user");
					if(user.principal().containsKey("refreshCookie") && user.principal().getBoolean("refreshCookie") )
					{
						LOGGER.debug("Got a request to refresh a cookie");
						invalidateSessionCookie(ctx);
						
						storeSessionCookie(ctx, user).onComplete(res ->
						{
							finishedCookie.complete();
						});
						user.principal().remove("refreshCookie");
					}
					else
					{
						if(user.principal().containsKey("storeCookie") && user.principal().getBoolean("storeCookie"))
						{
							LOGGER.debug("Got a request to store a cookie");
							storeSessionCookie(ctx, user).onComplete(res ->
							{
								finishedCookie.complete();
							});
							user.principal().remove("storeCookie");
						}
						else
						{
							// we checked, no cookie was requested. So we complete handling here.
							LOGGER.debug("No cookie to update found returning");
							finishedCookie.complete();
						}
					}				
				}
				else
				{
					LOGGER.debug("No user to update cookie for found returning");
					finishedCookie.complete();			
				}
		return finishedCookie.future();
	}
	public void invalidateSessionCookie(RoutingContext ctx)
	{
		Cookie sessionCookie = ctx.request().getCookie(SoileConfigLoader.getSessionProperty("sessionCookieID"));
		String token = CookieStrategy.getTokenFromCookieContent(sessionCookie.getValue());
		String username = CookieStrategy.getUserNameFromCookieContent(sessionCookie.getValue());		
		eb.send(SoileCommUtils.getUserEventBusCommand("invalidateUserSession")
			   ,new JsonObject().put("sessionID",token)
				 				.put("username",username));		
		
	}
	
	private Future<Void> storeSessionCookie(RoutingContext ctx, User user)
	{
		Promise<Void> cookieHandled = Promise.<Void>promise();
		LOGGER.debug("Adding Cookie");
		final byte[] rand = new byte[64];
	    random.nextBytes(rand);
	    String token = base64Encode(rand);
	    // we don't need any reply here.
	    JsonObject cuser = user.principal();
	    // pause this request until we are sure, that the session is stored. Otherwise we could return before the session is processed, which can lead to unauthorized requests.	    
	    eb.request(SoileCommUtils.getUserEventBusCommand("addSession")
				   ,new JsonObject().put("sessionID",token)
					 				.put("username",cuser.getString(SoileConfigLoader.getUserdbField("usernameField"))))
	    		.onComplete(reply ->
	    		{
	    			cookieHandled.complete();	    				    				
	    			if(reply.failed())
	    			{
	    				LOGGER.error(reply.cause());
	    			}
	    		});
		// now build the cookie to store on the remote system. 		
		String cookiecontent = CookieStrategy.buildCookieContent(cuser.getString("username"),token);
 
		Cookie cookie = Cookie.cookie(SoileConfigLoader.getSessionProperty("sessionCookieID"),cookiecontent)
							  .setDomain(SoileConfigLoader.getServerProperty(("domain")))
							  .setSecure(true)
							  .setPath(SoileConfigLoader.getSessionProperty("cookiePath"))
							  .setMaxAge(SoileConfigLoader.getSessionLongProperty("maxTime")/1000); //Maxtime in seconds
							 													 //TODO: Check whether SameSite needs to be set.
		LOGGER.debug("Adding Cookie: " + cookie.getName() + " / " +  cookie.getDomain() + " / " +  cookie.getValue() + " / " +  cookie.isSecure() );
		ctx.response().addCookie(cookie);
		return cookieHandled.future();
	}
		
}
