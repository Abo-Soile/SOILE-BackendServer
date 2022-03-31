package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.FormLoginHandlerImpl;

public class SoileFormLoginHandler extends FormLoginHandlerImpl{

	public SoileFormLoginHandler(AuthenticationProvider authProvider, String usernameParam, String passwordParam,
			String returnURLParam, String directLoggedInOKURL) {
		super(authProvider, usernameParam, passwordParam, returnURLParam, directLoggedInOKURL);
		// TODO Auto-generated constructor stub
	}
	
	 @Override
	  public void postAuthentication(RoutingContext ctx) {
		// get the parameters once more and check, whether to store a cookie.
		User user = ctx.user(); 		
		HttpServerRequest req = ctx.request();
		MultiMap params = req.formAttributes();
		Boolean remember = params.get(SoileConfigLoader.getCommunicationField("rememberLoginField")) == "1";
		user.principal().put("storeCookie", remember);
		//then run the stuff from auth.
		super.postAuthentication(ctx); 
	   
	  }

}
