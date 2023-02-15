package fi.abo.kogni.soile2.http_server.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;

public class SoileFormLoginHandler extends AuthenticationHandlerImpl<AuthenticationProvider> {
	static final Logger LOGGER = LogManager.getLogger(SoileFormLoginHandler.class);
	static final HttpException UNAUTHORIZED = new HttpException(401);
	static final HttpException BAD_REQUEST = new HttpException(400);
	static final HttpException BAD_METHOD = new HttpException(405);
	private SoileCookieCreationHandler cookieHandler;
	private JWTTokenCreator jwtCreator;
	private String usernameParam;
	private String passwordParam;

	public SoileFormLoginHandler(AuthenticationProvider authProvider, String usernameParam, String passwordParam,
			JWTTokenCreator jwtCreator, SoileCookieCreationHandler cookieHandler) {
		super(authProvider);
	    this.usernameParam = usernameParam;
	    this.passwordParam = passwordParam;
		this.cookieHandler = cookieHandler;
		this.jwtCreator = jwtCreator;
	}
		

	  @Override
	  public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {
	    HttpServerRequest req = context.request();
	    if (req.method() != HttpMethod.POST) {
	      handler.handle(Future.failedFuture(BAD_METHOD)); // Must be a POST
	    } else {
	      if (!context.body().available()) {
	        handler.handle(Future.failedFuture("BodyHandler is required to process POST requests"));
	      } else {
	        MultiMap params = req.formAttributes();
	        String username = params.get(usernameParam);
	        String password = params.get(passwordParam);
	        if (username == null || password == null) {
	          handler.handle(Future.failedFuture(BAD_REQUEST));
	        } else {
	          authProvider.authenticate(new UsernamePasswordCredentials(username, password), authn -> {
	            if (authn.failed()) {
	              handler.handle(Future.failedFuture(new HttpException(401, authn.cause())));
	            } else {
	              handler.handle(authn);
	            }
	          });
	        }
	      }
	    }
	  }
	
	 @Override
	  public void postAuthentication(RoutingContext ctx) {
		// get the parameters once more and check, whether to store a cookie.
		User user = ctx.user(); 		
		HttpServerRequest req = ctx.request();
		MultiMap params = req.formAttributes();
		Boolean remember = params.get("remember").equals("1");		
		user.principal().put("storeCookie", remember);
		cookieHandler.updateCookie(ctx, user).onComplete(cookieDone ->
		{
			// A response needs to wait on the cookieHandling to be done. Otherwise we can end up with the cookie not stored in the db and a new request failing.
			LOGGER.debug(user.principal().encodePrettily());
			// Finally set the token and send the reply		
			jwtCreator.getToken(ctx).onSuccess(token ->
			{
				ctx.response().setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
				.end(new JsonObject().put("token",token).encode());			
			}).onFailure(fail ->
			{
				if(fail.getCause() instanceof HttpException)
				{
					ctx.fail(((HttpException)fail.getCause()).getStatusCode());
				}
			});
		})
		.onFailure(err -> {
			ctx.fail(500,err);
		});
	  }

}
