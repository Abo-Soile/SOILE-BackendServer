package fi.abo.kogni.soile2.http_server.authentication;

import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.security.SecureRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class SoileCookieCreationHandler implements Handler<RoutingContext> {
	
	private final SecureRandom random = new SecureRandom();
	static final Logger LOGGER = LogManager.getLogger(SoileCookieCreationHandler.class);
	private EventBus eb;
	public SoileCookieCreationHandler(EventBus bus) {
		eb = bus;
	}
	@Override
	public void handle(RoutingContext ctx) {
		// TODO Auto-generated method stub
		LOGGER.debug("Handling a request");
		if(ctx.user() != null)
		{
			// there is a cookie that we need to invalidate, and a new one that we need to put in.
			
			if(ctx.user().principal().containsKey("refreshCookie") && ctx.user().principal().getBoolean("refreshCookie") )
			{
				LOGGER.debug("Got a request to refresh a cookie");
				invalidateSessionCookie(ctx);
				storeSessionCookie(ctx);
				ctx.user().principal().remove("refreshCookie");
			}
			else
			{
				if(ctx.user().principal().containsKey("storeCookie") && ctx.user().principal().getBoolean("storeCookie"))
				{
					LOGGER.debug("Got a request to store a cookie");
					storeSessionCookie(ctx);
					ctx.user().principal().remove("storeCookie");
				}			
			}			
		}
		
	}

	public void invalidateSessionCookie(RoutingContext ctx)
	{
		Cookie sessionCookie = ctx.request().getCookie(SoileConfigLoader.getSessionProperty("sessionCookieID"));
		String token = CookieStrategy.getTokenFromCookieContent(sessionCookie.getValue());
		String username = CookieStrategy.getUserNameFromCookieContent(sessionCookie.getValue());		
		eb.send(SoileCommUtils.getUserEventBusCommand("invalidateUserSession")
			   ,new JsonObject().put(SoileCommUtils.getCommunicationField("sessionID"),token)
				 				.put(SoileCommUtils.getCommunicationField("usernameField"),username));		
		
	}
	
	private void storeSessionCookie(RoutingContext ctx)
	{
		LOGGER.debug("Adding Cookie");
		final byte[] rand = new byte[64];
	    random.nextBytes(rand);
	    String token = base64Encode(rand);
	    // we don't need any reply here.
	    JsonObject cuser = ctx.user().principal();		
	    eb.send(SoileCommUtils.getUserEventBusCommand("addSession")
				   ,new JsonObject().put(SoileCommUtils.getCommunicationField("sessionID"),token)
					 				.put(SoileCommUtils.getCommunicationField("usernameField"),cuser.getString(SoileConfigLoader.getdbField("usernameField"))));
		// now build the cookie to store on the remote system. 		
		String cookiecontent = CookieStrategy.buildCookieContent(cuser.getString(SoileCommUtils.getCommunicationField("usernameField")),token);
 
		Cookie cookie = Cookie.cookie(SoileConfigLoader.getSessionProperty("sessionCookieID"),cookiecontent)
							  .setDomain(SoileConfigLoader.getServerProperty(("domain")))
							  .setSecure(true)
							  .setPath(SoileConfigLoader.getSessionProperty("cookiePath"))
							  .setMaxAge(SoileConfigLoader.getSessionLongProperty("maxTime")/1000); //Maxtime in seconds
							 													 //TODO: Check whether SameSite needs to be set.
		LOGGER.debug("Adding Cookie: " + cookie.getName() + " / " +  cookie.getDomain() + " / " +  cookie.getValue() + " / " +  cookie.isSecure() );
		ctx.response().addCookie(cookie);		
	}
		
}
