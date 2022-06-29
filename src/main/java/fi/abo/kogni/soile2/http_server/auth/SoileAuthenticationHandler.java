package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.ChainAuthHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;

/**
 * This class provides the Soile Authentication handlers which 
 * @author Thomas Pfau
 *
 */
public class SoileAuthenticationHandler {
	
	private static JWTAuth authProvider;
	private static SimpleAuthenticationHandler cookieHandler;
	
	public synchronized static JWTAuth getAuthProvider(Vertx vertx)
	{
		if(authProvider == null)
		{
			JWTAuthOptions config = new JWTAuthOptions()
										.setKeyStore(new KeyStoreOptions()
										.setPath("jwtstore.jceks")
										.setPassword(SoileConfigLoader.getServerProperty("jwtStoreSecret")));
			authProvider = JWTAuth.create(vertx, config);
		}
		return authProvider;
	}
	
	public synchronized static SimpleAuthenticationHandler getCookieAuthProvider(Vertx vertx, MongoClient client)
	{
		if(cookieHandler == null)
		{		
			cookieHandler = SimpleAuthenticationHandler.create();
			SoileCookieAuth soileHandler = new SoileCookieAuth(vertx, client);
			cookieHandler.authenticate(soileHandler::authenticate);		
		}
		return cookieHandler;
	}	
	
	public static ChainAuthHandler create(Vertx vertx, MongoClient client)
	{			
		JWTAuthHandler jwtAuth = JWTAuthHandler.create(getAuthProvider(vertx));	
		ChainAuthHandler handler = ChainAuthHandler.any().add(getCookieAuthProvider(vertx, client)).add(jwtAuth);
		return handler;
	}
}
