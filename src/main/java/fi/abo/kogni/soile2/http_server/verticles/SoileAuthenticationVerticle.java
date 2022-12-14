package fi.abo.kogni.soile2.http_server.verticles;





import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.SoileAuthentication;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthorizationProvider;
import fi.abo.kogni.soile2.http_server.auth.SoileFormLoginHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileCookieCreationHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileCookieRestoreHandler;
import fi.abo.kogni.soile2.utils.DebugRouter;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authorization.OrAuthorization;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthorizationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.SessionStore;

public class SoileAuthenticationVerticle extends SoileBaseVerticle{
	
	MongoClient client;
	SoileAuthorizationProvider authZuser;
	SoileAuthentication soileAuth;
	SoileAuthHandler accessHandler;
	SoileCookieRestoreHandler restoreHandler;
	SoileCookieCreationHandler cookieCreationHandler;
	RedirectAuthHandler auth;
	static final Logger LOGGER = LogManager.getLogger(SoileAuthenticationVerticle.class);
	static enum AccessType
	{
		FileAccess,
		DataAccess,
		ResultAccess
	}
	
	/**
	 * Build a authentication vehicle which will add authentication mechanisms to the individual routes.
	 * Should be started before other routing handlers.
	 * @param router The {@link Router} that will be user
	 * @param store the {@link SessionStore} that will be used.
	 */
	public SoileAuthenticationVerticle()
	{
	}
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception
	{
		LOGGER.debug("Starting SoileAuthVerticle with id: " + deploymentID());
		try
		{
		setupConfig("experiments");
		client = MongoClient.createShared(vertx, SoileConfigLoader.getDbCfg());
		setupHandlers();
		setupRouteInit();
		setupAuthenticationHandlers();	
		setupRoutes();
		setUpAuthentication();
		setupPostAuthHandlers();
		}
		catch(Exception e)
		{
			startPromise.fail(e.getMessage());
			e.printStackTrace(System.out);			
		}
		startPromise.complete();
		LOGGER.debug("SoileAuthVerticle started successfully");
		
	}
	/**
	 * Set up initial Handlers used for routing
	 */
	void setupHandlers()
	{
//		SoileAuthenticationOptions  authOpts = new SoileAuthenticationOptions(config());			
		soileAuth = new SoileAuthentication(client);
		accessHandler = new SoileAuthHandler(vertx, soileAuth);
		restoreHandler = new SoileCookieRestoreHandler(vertx, config());
		auth = RedirectAuthHandler.create(soileAuth,"/static/login.html");
		cookieCreationHandler = new SoileCookieCreationHandler(vertx.eventBus());
	}
	
	
	/**
	 * These handlers should be present in all routes and handle things like Logging, Sessions or similar. 
	 */
	void setupRouteInit()
	{
	}
	
	/**
	 * These handlers handle authentication on restricted resources, i.e. they are set up for all restricted 
	 * areas of the Server. 
	 */
	void setupAuthenticationHandlers()
	{		
	}
	
	void setUpAuthentication()
	{
		LOGGER.debug("Adding handler to POST: /auth");		
		// This will only handle the login request send to this address. handler(BodyHandler.create()).
		//router.post("/auth").handler(BodyHandler.create());
		//router.post("/auth").handler(new SoileFormLoginHandler(soileAuth, "username", "password",
		//							 null, null));		
	}
	
	void setupAuthorization()
	{
		
		// Create the authorization providers
		authZuser = new SoileAuthorizationProvider();		
		// define necessary messages 
		setupRoutes();	
	}
	void setupPostAuthHandlers()
	{
	}
	
	void setupRoutes()
	{		
	}
	
	
	void handleFileReadAccess(RoutingContext ctx)
	{
		handlePermissiveReadAccess(ctx, AccessType.FileAccess);
	}
	
	void setupRestrictedRoute(String route)
	{
		
	}
	
	void handlePermissiveReadAccess(RoutingContext ctx, AccessType access)
	{
		String expID = ctx.pathParam("id");
		//TODO: Ensure, that this is actually correct...
		vertx.eventBus().request(getEventbusCommandString("requestReadPermissions"), new JsonObject().put(getConfig("IDField"), expID)).onComplete(
				res -> {
					if(res.succeeded())
					{						
						OrAuthorization orAuth = OrAuthorization.create();
						JsonObject response = (JsonObject) res.result().body();
						if(response.getString("Result").equals("Success"))
						{
						if(!response.getBoolean(getConfig("logonRequiredField")))
						{
							// This is a public resource, just forward it. 
							ctx.next();
							return;
						}
						JsonArray roles = response.getJsonArray(getConfig("readRolesField"));							
						for(Object obj : roles)
						{
							String currentRole = (String) obj;
							orAuth.addAuthorization(RoleBasedAuthorization.create(currentRole));
						}
						for(Object obj : roles)
						{
							String currentRole = (String) obj;
							orAuth.addAuthorization(RoleBasedAuthorization.create(currentRole));
						}
						orAuth.addAuthorization(PermissionBasedAuthorization.create(expID));
						AuthorizationHandler.create(orAuth)																		
						// where to lookup the authorizations for the user
						.addAuthorizationProvider(authZuser)
						.handle(ctx);							
						}
						else
						{
							if(response.getString("Reason").equals(getConfig("objectNotExistent")))
							{
								ctx.fail(404, new HttpException(404));
							}
						}
					}
					else
					{
						// we could not get permissions. So we refuse this request.
						LOGGER.debug("Unable to retrieve permissions for requested ressource: " + expID);
						ctx.fail(403, new HttpException(403));
					}
								
				});	
	}

	private class RedirectUnauthorizedHandler implements Handler<RoutingContext>
	{

		@Override
		public void handle(RoutingContext ctx) {
			if(ctx.failed())
			{
				if(HttpResponseStatus.valueOf(ctx.statusCode()) == HttpResponseStatus.UNAUTHORIZED)
				{
					//we came here because the context was unauthorized.
					//redirect it to the login page
					ctx.redirect("/login");
				}
			}
			
			
		}
		
	}
}
