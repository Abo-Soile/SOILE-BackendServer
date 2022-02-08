package fi.abo.kogni.soile2.http_server;





import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.SoileAccessHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthentication;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthenticationOptions;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.authentication.SoileSessionRestoreHandler;
import fi.abo.kogni.soile2.http_server.utils.DebugRouter;
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authorization.OrAuthorization;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthorizationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.SessionStore;

public class SoileAuthenticationVerticle extends SoileBaseVerticle{
	
	MongoClient client;
	Router router;
	SessionStore store;
	MongoAuthorization authZuser;
	MongoAuthorization authZpart;
	SoileAuthentication soileAuth;
	SoileAccessHandler accessHandler;
	SoileSessionRestoreHandler restoreHandler;
	
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
		System.out.println("Starting SoileAuthVerticle");
		try
		{
		setupConfig("experiments");
		client = MongoClient.createShared(vertx, config().getJsonObject("db"));
		setupBasicRoutes();
		setupHandlers();
		setUpAuthentication();		
		}
		catch(Exception e)
		{
			startPromise.fail(e.getMessage());
			e.printStackTrace(System.out);			
		}
		startPromise.complete();
		System.out.println("SoileAuthVerticle started successfully");
		
	}
	
	void setupHandlers()
	{
		SoileAuthenticationOptions  authOpts = new SoileAuthenticationOptions(config());			
		soileAuth = new SoileAuthentication(client, authOpts, config());
		accessHandler = new SoileAccessHandler(vertx, soileAuth, config());
		restoreHandler = new SoileSessionRestoreHandler(vertx, soileAuth, config());

	}
	void setupBasicRoutes()
	{
		router.route().handler(LoggerHandler.create());
		router.route().handler(SessionHandler.create(store));		
		router.route().handler(BodyHandler.create());
		//router.route().handler(CorsHandler.create("localhost"));
		//router.route().handler(CSRFHandler.create(vertx, config().getJsonObject(SoileConfigLoader.HTTP_SERVER_CFG).getString("serverSecret")));
		//router.route().handler(restoreHandler);
	}
	void setUpAuthentication()
	{
				
		// This will only handle the login request send to this address. handler(BodyHandler.create()).
		router.route().handler(new DebugRouter());
		router.post("/services/auth").handler(accessHandler);				
	}
	
	void setupAuthorization()
	{
		// Create the authorization providers
		MongoAuthorizationOptions partConf = new MongoAuthorizationOptions().setCollectionName(config().getJsonObject(SoileConfigLoader.USERCOLLECTIONS).getString("participant"));
		MongoAuthorizationOptions userConf = new MongoAuthorizationOptions().setCollectionName(config().getJsonObject(SoileConfigLoader.USERCOLLECTIONS).getString("user"));
		
		authZpart = new SoileAuthorization(client, partConf, "Participant_Auth");
		authZuser = new SoileAuthorization(client, userConf, "User_Auth");
		
		// define necessary messages 
		setupUserRoutes();	
	}
	
	void setupUserRoutes()
	{		
		//set up SessionRestoreHandler
		router.route(HttpMethod.GET,"/experiment/:id/resources/*").handler( this::handleFileReadAccess);
		router.route("/experiment/:id/resources/*").handler(RedirectAuthHandler.create(null));
		//router.route(HttpMethod.POST,"/experiment/:id/resources/*").handler( this::handleRestrictiveAccess);
	}
	
	
	void handleFileReadAccess(RoutingContext ctx)
	{
		handlePermissiveReadAccess(ctx, AccessType.FileAccess);
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
						.addAuthorizationProvider(authZpart)
						.addAuthorizationProvider(authZuser)
						.handle(ctx);							
						}
						else
						{
							if(response.getString("Reason").equals(getConfig("objectNotExistent")))
							{
								
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
