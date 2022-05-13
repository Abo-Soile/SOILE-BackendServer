package fi.abo.kogni.soile2.http_server.authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.FormLoginHandlerImpl;

public class SoileFormLoginHandler extends FormLoginHandlerImpl{
	static final Logger LOGGER = LogManager.getLogger(SoileFormLoginHandler.class);

	private SoileCookieCreationHandler cookieHandler;
	public SoileFormLoginHandler(AuthenticationProvider authProvider, String usernameParam, String passwordParam,
			String returnURLParam, String directLoggedInOKURL, SoileCookieCreationHandler cHandler) {
		super(authProvider, usernameParam, passwordParam, returnURLParam, directLoggedInOKURL);
		// TODO Auto-generated constructor stub
		cookieHandler = cHandler;
	}
	
	 @Override
	  public void postAuthentication(RoutingContext ctx) {
		// get the parameters once more and check, whether to store a cookie.
		User user = ctx.user(); 		
		HttpServerRequest req = ctx.request();
		MultiMap params = req.formAttributes();
		LOGGER.debug(params);
		LOGGER.debug(SoileConfigLoader.getCommunicationField("rememberLoginField"));
		Boolean remember = params.get(SoileConfigLoader.getCommunicationField("rememberLoginField")).equals("1");		
		user.principal().put("storeCookie", remember);
		LOGGER.debug(user.principal().encodePrettily());
		cookieHandler.handle(ctx);
		//then run the stuff from auth.
		super.postAuthentication(ctx); 
	   
	  }

}
