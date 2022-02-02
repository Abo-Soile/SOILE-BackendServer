package fi.abo.kogni.soile2.http_server;





import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.SoileAccessHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthentication;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthenticationOptions;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
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
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.SessionStore;

public class SoileAuthenticationVerticle extends SoileBaseVerticle{
	
	MongoClient client;
	Router router;
	SessionStore store;
	MongoAuthorization authZuser;
	MongoAuthorization authZpart;
	static final Logger LOGGER = LogManager.getLogger(SoileAuthenticationVerticle.class);
	static enum AccessType
	{
		FileAccess,
		DataAccess,
		ResultAccess
	}
	
	
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
		router.route().handler(SessionHandler.create(store));		
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
	
	
	void setUpAuthentication()
	{
		SoileAuthenticationOptions partConf = new SoileAuthenticationOptions(config().getJsonObject(SoileUserManagementVerticle.PARTICIPANT_CFG));
		SoileAuthenticationOptions userConf = new SoileAuthenticationOptions(config().getJsonObject(SoileUserManagementVerticle.USER_CFG));		
		JsonObject uManConf = config().getJsonObject(SoileConfigLoader.USERMGR_CFG);
		partConf.setUserType(uManConf.getString("participantType"));
		userConf.setUserType(uManConf.getString("researcherType"));
		SoileAccessHandler parthandler = new SoileAccessHandler(vertx, new SoileAuthentication(client, partConf, config()),uManConf,getSessionConfig());
		SoileAccessHandler userhandler = new SoileAccessHandler(vertx, new SoileAuthentication(client, userConf, config()),uManConf,getSessionConfig());
		
		// This will only handle the login request send to this address.
		router.post("/services/user/auth").handler(userhandler);
		router.post("/services/part/auth").handler(parthandler);		
	}
	
	void setupAuthorization()
	{
		// Create the authorization providers
		MongoAuthorizationOptions partConf = new MongoAuthorizationOptions(config().getJsonObject(SoileUserManagementVerticle.PARTICIPANT_CFG));
		MongoAuthorizationOptions userConf = new MongoAuthorizationOptions(config().getJsonObject(SoileUserManagementVerticle.USER_CFG));
	
		authZpart = new SoileAuthorization(client, partConf, "Participant_Auth");
		authZuser = new SoileAuthorization(client, userConf, "User_Auth");
		
		// define necessary messages 
		setupUserRoutes();	
	}
	
	void setupUserRoutes()
	{		
		router.route(HttpMethod.GET,"/experiment/:id/resources/*").handler( this::handleFileReadAccess);		
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
	
}
