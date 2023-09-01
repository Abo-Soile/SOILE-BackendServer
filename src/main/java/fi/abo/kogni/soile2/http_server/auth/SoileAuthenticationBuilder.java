package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.AuthenticationHandler;
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
	/**
	 * Get a JWTAuth Provider for the Soile platform
	 * @param vertx
	 * @return A {@link JWTAuth} provider with a soile specific config (generated if it did not previously exist)
	 */
	public synchronized JWTAuth getJWTAuthProvider(Vertx vertx)
	{
		if(authProvider == null)
		{
			JWTAuthOptions config = new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions()
				    .setAlgorithm("HS256")
				    .setBuffer(SoileConfigLoader.getServerProperty("jwtStoreSecret")));
			authProvider = JWTAuth.create(vertx, config);			
		}
		return authProvider;
	}
	
	/**
	 * A {@link AuthenticationHandler} that authenticates using cookies. 
	 * @param vertx
	 * @return a Auth Provider using Cookies for auth
	 */
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
	
	/**
	 * A {@link AuthenticationHandler} that authenticates using a token. Should only be used for Project execution specific 
	 * end-points 
	 * @param vertx
	 * @return a Auth Provider using Cookies for auth
	 */
	public synchronized SimpleAuthenticationHandler getTokenAuthProvider(ParticipantHandler partHandler, MongoClient client)
	{
		if(tokenHandler == null)
		{		
			tokenHandler = SimpleAuthenticationHandler.create();
			TokenAuthProvider tokenAuth= new TokenAuthProvider(partHandler, client);
			tokenHandler.authenticate(tokenAuth::authenticate);		
		}
		return tokenHandler;
	}	
}
