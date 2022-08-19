package fi.abo.kogni.soile2.http_server.authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.ext.web.impl.RoutingContextInternal;
/**
 * This AuthenticationHandler will handle Authentication based on a form, that contains username, password and a "remember" field. 
 * If remember is ticked, it will put the remember flag on in the session, This flag is checked, post authentication, to set the cookie, if requested. 
 * The cookie can be used for later reauthentication based on a previous CookieHandler. 
 * @author Thomas Pfau
 *
 */
public class SoileAuthHandler extends AuthenticationHandlerImpl<AuthenticationProvider> implements Handler<RoutingContext>{

	static final Logger LOGGER = LogManager.getLogger(SoileAuthHandler.class);


	public SoileAuthHandler(Vertx vertx, SoileAuthentication mAuthen)
	{		
		super(mAuthen);
	}


	@Override
	public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {

		LOGGER.debug(handler.getClass().toString());
		HttpServerRequest req = context.request();
		
		//first, we will look into whether this user is already logged in. 
		if (req.method() != HttpMethod.POST) {
			handler.handle(Future.failedFuture(new HttpException(405))); // Must be a POST
		} else {
			if (!((RoutingContextInternal) context).seenHandler(RoutingContextInternal.BODY_HANDLER)) {
				handler.handle(Future.failedFuture("BodyHandler is required to process POST requests"));
			} else {
				MultiMap formAttribs = req.formAttributes();
				if(formAttribs == null)
				{
					//Again we expect a form for authentication.
					handler.handle(Future.failedFuture(new HttpException(405)));
				}
				//TODO: Make these settings, also in the dustjs/js code! 
				String username = formAttribs.get(SoileCommUtils.getCommunicationField("usernameField"));
				String password = formAttribs.get(SoileCommUtils.getCommunicationField("passwordField"));
				LOGGER.debug("Request to store cookie: " + formAttribs.get("remember").equals("1"));
				boolean remember = ( formAttribs.get("remember") == null ? false : formAttribs.get("remember").equals("1"));
				
				context.session().put("remember", remember);
				if (username == null || password == null) {
					handler.handle(Future.failedFuture(new HttpException(405)));
				} else {					
					LOGGER.debug("Trying to auth user " + username +" with password " + password);
					authProvider.authenticate(new UsernamePasswordCredentials(username, password), authn -> {
						if (authn.failed()) {
							LOGGER.debug("Handling invalid auth: ");
							//authn.cause().printStackTrace(System.out);
							LOGGER.debug(handler.getClass());
							handler.handle(Future.failedFuture(new HttpException(401, authn.cause())));
							LOGGER.debug("Handling finished");
						} else {
							if(context.user() != null)
							{
								// this is a new login!, we clear any old one.
								context.clearUser();			
							}
							handler.handle(authn);
							
						}	            
					});
				}
			}
		}
	}

	/**
	 * Here the cookie to keep the login active is being set, along with forwarding to the url requested
	 */
	@Override
	public void postAuthentication(RoutingContext ctx) {
		// if this has now an assigned user, we will store this user.
		LOGGER.debug("Handling Post-Authentication");	
		HttpServerRequest req = ctx.request();
		Session session = ctx.session();
		if (session != null) {
			String returnURL = session.remove("return_url");
			if (returnURL != null) {
				// Now redirect back to the original url
				ctx.redirect(returnURL);
				return;
			}
		}   
		// Just show a basic page
		req.response()
		.putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
		.end("<html><body><h1>Login successful</h1></body></html>");

	}
	
	
	@Override
	  protected void processException(RoutingContext ctx, Throwable exception) {
	    if (exception != null) {
	      if (exception instanceof HttpException) {
	        final int statusCode = ((HttpException) exception).getStatusCode();
	        final String payload = ((HttpException) exception).getPayload();

	        switch (statusCode) {
	          case 302:
	            ctx.response()
	              .putHeader(HttpHeaders.LOCATION, payload)
	              .setStatusCode(302)
	              .end("Redirecting to " + payload + ".");
	            return;
	          case 401:
	        	// we will reroute the request to the login page. 
	            if (!"XMLHttpRequest".equals(ctx.request().getHeader("X-Requested-With"))) {
	            	setAuthenticateHeader(ctx);
	            }
	            ctx.redirect("/login");	            
	            return;
	          default:
	            ctx.fail(statusCode, exception);
	            return;
	        }
	      }
	    }

	    // fallback 500
	    ctx.fail(exception);
	  }	

}
