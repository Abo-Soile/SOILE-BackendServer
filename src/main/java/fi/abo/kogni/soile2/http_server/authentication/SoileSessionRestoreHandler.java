package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.http_server.utils.SoileCommUtils;
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;


public class SoileSessionRestoreHandler extends AuthenticationHandlerImpl<AuthenticationProvider>  implements RedirectAuthHandler {

	  private final String loginRedirectURL;
	  private final String returnURLParam;
	  private final Vertx vertx;
	  JsonObject sessionConfig;
	  JsonObject userConfig;
	  JsonObject communicationConfig;
	  
	  public SoileSessionRestoreHandler(Vertx vertx, AuthenticationProvider authProvider, JsonObject config) {
		  super (authProvider);
		  userConfig = config.getJsonObject(SoileConfigLoader.USERMAGR_CFG);
		  sessionConfig = config.getJsonObject(SoileConfigLoader.SESSION_CFG);
		  communicationConfig = config.getJsonObject(SoileConfigLoader.SESSION_CFG);
		  loginRedirectURL = sessionConfig.getString("loginURL");
		  returnURLParam = sessionConfig.getString("returnURLParam");
		  this.vertx = vertx;				  
	  }

	  @Override
	  public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {		  
		  Cookie sessionCookie = context.request().getCookie(sessionConfig.getString("sessionCookieID"));
			if(sessionCookie != null)
			{
				String[] data = sessionCookie.getValue().split(":", 2);
				String token = data[0];
				String username = data[1];
				String userType = data[2];
				vertx.eventBus()
				.request(SoileConfigLoader.getEventBusCommand(userConfig, "checkUserSessionValid"),
						  new JsonObject().put(SoileCommUtils.getCommunicationField(communicationConfig,"usernameField"), username)
						  				  .put(SoileCommUtils.getCommunicationField(communicationConfig,"userTypeField"), userType)
										  .put(sessionConfig.getString("sessionID"), token),
						  res ->
								{
									if(res.succeeded())
									{
										JsonObject result = (JsonObject)res.result();
										if(SoileCommUtils.isResultSuccessFull(result)) {
											User user = User.fromName(username);
									    	user.principal().put(sessionConfig.getString("userTypeField"), userType);
										}
										
									}
								});
			}
		  Session session = context.session();
		  if (session != null) {
			  // Now redirect to the login url - we'll get redirected back here after successful login
			  session.put(returnURLParam, context.request().uri());
			  handler.handle(Future.failedFuture(new HttpException(302, loginRedirectURL)));
		  } else {
			  handler.handle(Future.failedFuture("No session - did you forget to include a SessionHandler?"));
		  }
	  }

	  @Override
	  public boolean performsRedirect() {
		  return true;
	  }


}
