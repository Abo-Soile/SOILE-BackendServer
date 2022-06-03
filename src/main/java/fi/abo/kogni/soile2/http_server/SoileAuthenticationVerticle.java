package fi.abo.kogni.soile2.http_server;





import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.SoileAuthentication;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthorizationProvider;
import fi.abo.kogni.soile2.http_server.authentication.SoileCookieAuthHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileCookieCreationHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileCookieRestoreHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileFormLoginHandler;
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
	Router router;
	SessionStore store;
	SoileAuthorizationProvider authZuser;
	SoileAuthentication soileAuth;
	SoileCookieAuthHandler accessHandler;
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
	public SoileAuthenticationVerticle(Router router, SessionStore store)
	{
		this.store = store;
		this.router = router;
	}
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception
	{
		LOGGER.debug("Starting SoileAuthVerticle");
		try
		{
		setupConfig("experiments");
		client = MongoClient.createShared(vertx, SoileConfigLoader.getDbCfg());
		setupHandlers();
		setupRouteInit();
		setupAuthenticationHandlers();	
		//setupBasicRoutes();
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
		accessHandler = new SoileCookieAuthHandler(vertx, soileAuth);
		restoreHandler = new SoileCookieRestoreHandler(vertx, config());
		auth = RedirectAuthHandler.create(soileAuth,"/static/login.html");
		cookieCreationHandler = new SoileCookieCreationHandler(vertx.eventBus());
	}
	/**
	 * These handlers should be present in all routes and handle things like Logging, Sessions or similar. 
	 */
	void setupRouteInit()
	{
		router.route().handler(new DebugRouter());
		router.route().handler(LoggerHandler.create());
		router.route().handler(SessionHandler.create(store));		
		router.route().handler(restoreHandler);		
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
		router.post("/auth").handler(BodyHandler.create());
		router.post("/auth").handler(new SoileFormLoginHandler(soileAuth, "username", "password",
									 RedirectAuthHandler.DEFAULT_RETURN_URL_PARAM, "/", cookieCreationHandler));		
	}
	
	void setupAuthorization()
	{
		
		// Create the authorization providers
		MongoAuthorizationOptions userConf = SoileConfigLoader.getMongoAuthZOptions();	
		authZuser = new SoileAuthorizationProvider(client);		
		// define necessary messages 
		setupUserRoutes();	
	}
	void setupPostAuthHandlers()
	{
		router.route().handler(cookieCreationHandler);
	}
	void setupUserRoutes()
	{		
		
		router.route(HttpMethod.GET,"/experiment/:id/resources/*").handler( this::handleFileReadAccess);
		router.route("/experiment/:id/resources/*").handler(RedirectAuthHandler.create(null));
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
