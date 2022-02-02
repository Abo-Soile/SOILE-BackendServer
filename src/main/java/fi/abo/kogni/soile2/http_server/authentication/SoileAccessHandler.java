package fi.abo.kogni.soile2.http_server.authentication;

import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.security.SecureRandom;

import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.ext.web.impl.RoutingContextInternal;

public class SoileAccessHandler extends AuthenticationHandlerImpl<AuthenticationProvider> implements Handler<RoutingContext> {

	private final JsonObject userConfig;
	private final JsonObject sessionConfig;
	private final Vertx vertx;
	private final SecureRandom random = new SecureRandom();


	public SoileAccessHandler(Vertx vertx, SoileAuthentication mAuthen, JsonObject userConfig, JsonObject sessionConfig)
	{
		super(mAuthen);
		this.userConfig = userConfig;
		this.sessionConfig = sessionConfig;
		this.vertx = vertx;
	}


	@Override
	public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {

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
				String username = formAttribs.get("username");
				String password = formAttribs.get("password");
				boolean remember = formAttribs.get("remember") == null ? false : formAttribs.get("remember") == "1";			

				if (username == null || password == null) {
					handler.handle(Future.failedFuture(new HttpException(405)));
				} else {
					authProvider.authenticate(new UsernamePasswordCredentials(username, password), authn -> {
						if (authn.failed()) {
							handler.handle(Future.failedFuture(new HttpException(401, authn.cause())));
						} else {
							handler.handle(authn);
							context.session().put("remember", remember);
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

		if(ctx.session().<Boolean>remove("remember") && ctx.user() != null)
		{
			// store this session
			storeSessionCookie(ctx);			
		}
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
	
	
	public void storeSessionCookie(RoutingContext ctx)
	{
		final byte[] rand = new byte[64];
	    random.nextBytes(rand);
	    String token = base64Encode(rand);
	    // we don't need any reply here.
		vertx.eventBus()
			 .send(SoileConfigLoader.getEventBusCommand(userConfig, "addSession")
				   ,new JsonObject().put(sessionConfig.getString("sessionID"),token));
		// now build the cookie to store on the remote system. 		
		String cookiecontent = token + ":" 
							   + ctx.user().principal().getString(sessionConfig.getString("usernameField")) + ":" 
							   + ctx.user().principal().getString(sessionConfig.getString("userTypeField")); 
		Cookie cookie = Cookie.cookie(sessionConfig.getString("sessionCookieID"),cookiecontent);
		ctx.response().addCookie(cookie);
	}
	

}
