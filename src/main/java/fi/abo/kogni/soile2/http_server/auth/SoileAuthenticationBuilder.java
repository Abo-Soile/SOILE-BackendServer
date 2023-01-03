package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
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
public class SoileAuthenticationBuilder {
	
	private JWTAuth authProvider;
	private SimpleAuthenticationHandler cookieHandler;
	private SimpleAuthenticationHandler tokenHandler;
	public synchronized JWTAuth getJWTAuthProvider(Vertx vertx)
	{
		if(authProvider == null)
		{
			JWTAuthOptions config = new JWTAuthOptions()
										.setKeyStore(new KeyStoreOptions()
										.setPath("soile.jceks")
										.setPassword(SoileConfigLoader.getServerProperty("jwtStoreSecret")));
			authProvider = JWTAuth.create(vertx, config);
		}
		return authProvider;
	}
	
	public synchronized SimpleAuthenticationHandler getCookieAuthProvider(Vertx vertx, MongoClient client, SoileCookieCreationHandler cookieCreationHandler)
	{
		if(cookieHandler == null)
		{		
			cookieHandler = SimpleAuthenticationHandler.create();
			SoileCookieAuth soileHandler = new SoileCookieAuth(vertx, client, cookieCreationHandler);
			cookieHandler.authenticate(soileHandler::authenticate);		
		}
		return cookieHandler;
	}	
	
	public synchronized SimpleAuthenticationHandler getTokenAuthProvider(ParticipantHandler partHandler)
	{
		if(tokenHandler == null)
		{		
			tokenHandler = SimpleAuthenticationHandler.create();
			TokenAuthProvider tokenAuth= new TokenAuthProvider(partHandler);
			tokenHandler.authenticate(tokenAuth::authenticate);		
		}
		return tokenHandler;
	}	
	
	public ChainAuthHandler create(Vertx vertx, MongoClient client, SoileCookieCreationHandler cookieCreationHandler)
	{			
		JWTAuthHandler jwtAuth = JWTAuthHandler.create(getJWTAuthProvider(vertx));	
		ChainAuthHandler handler = ChainAuthHandler.any().add(getCookieAuthProvider(vertx, client, cookieCreationHandler)).add(jwtAuth);
		return handler;
	}
}
