package fi.abo.kogni.soile2.http_server;

import java.net.HttpURLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class SetupServer extends SoileServerVerticle {

	static final Logger LOGGER = LogManager.getLogger(SetupServer.class);
	private String dataFolder;			
	
	public SetupServer(String dataFolder) {
		super();
		this.dataFolder = dataFolder;
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {		
		soileRouter = new SoileRouteBuilding();
		deployedVerticles = new ConcurrentLinkedQueue<>();	
		
		setupConfig() // As the very first step we need to set up the config so that it is available for all later steps.			
		.compose(this::setupFolders)
		.compose(this::deployVerticles) // deploy all necessary verticles for setup.
		.compose(this::createAdminUser)
		.compose(this::makeExampleProject)
		.onComplete(res ->
		{
			if(res.succeeded())
			{
				LOGGER.info("Server successfully set up, you can now start it.");
				startPromise.complete();				
			}
			else
			{
				res.cause().printStackTrace(System.out);
				LOGGER.error("Error setting up the server " + res.cause().getMessage());
				startPromise.fail(res.cause().getMessage());
			}
		});
	}	
	
	Future<Void> createAdminUser(Void unused)
	{
		Promise<Void> setupPromise = Promise.promise();
		vertx.fileSystem().readFile(SetupServer.class.getClassLoader().getResource("setup.json").getPath())
		.onSuccess(fileContents -> {
			JsonObject def = new JsonObject(fileContents);
			createAdminUser(def)
			.compose(this::makeUserAdmin)
			.onSuccess( res -> {
				setupPromise.complete();	
			})
			.onFailure(err -> setupPromise.fail(err));
		})
		.onFailure(err -> setupPromise.fail(err));
		
		return setupPromise.future();
	}
		
	Future<JsonObject> createAdminUser(JsonObject config)
	{
		
		Promise<JsonObject> accountCreatedPromise = Promise.promise();
		JsonObject AdduserCommand = new JsonObject().put("username", config.getString("adminuser"))
													.put("password",config.getString("adminpassword"));
		LOGGER.info("Setting up user");
		vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"addUser"), AdduserCommand )
		.onSuccess(done -> {
			accountCreatedPromise.complete(config);
		})
		.onFailure(err -> {
			if(err instanceof ReplyException)
			{
				if(((ReplyException)err).failureCode() == HttpURLConnection.HTTP_CONFLICT)
				{
					LOGGER.info("User existed. Resetting password to selected password.");
					vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"setPassword"), AdduserCommand )
					.onSuccess(done -> {
							accountCreatedPromise.complete(config);
					})		
					.onFailure(err2 -> accountCreatedPromise.fail(err2));
				}
				else
				{
					accountCreatedPromise.fail(err);
				}
			}
			else
			{
				accountCreatedPromise.fail(err);
			}
			
		});
		
		return accountCreatedPromise.future();
	}
	
	Future<JsonObject> makeUserAdmin(JsonObject config)
	{
		JsonObject makeUserAdminCommand = new JsonObject().put("username", config.getString("adminuser"))
													.put("role",Roles.Admin.toString())
													.put("command", "setCommand");
		
		LOGGER.info("Making user Admin");
		return vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"permissionOrRoleChange"), makeUserAdminCommand )
						.map(config);
	}

	
	Future<Void> makeExampleProject(Void unused)
	{		
		return ObjectGenerator.createProject(MongoClient.create(vertx, SoileConfigLoader.getMongoCfg()), vertx, "ExampleProject", dataFolder).mapEmpty();				
	}
	
	
	public static void main(String[] args)
	{
		Vertx instance = Vertx.vertx();
		String dataFolder;
		if(args.length > 0)
		{
			dataFolder = args[0];
		}
		else
		{
			dataFolder = null;
		}
		
		instance.deployVerticle(new SetupServer(dataFolder))
		.onSuccess(res -> {			
			instance.close()
			.onSuccess(closed -> {				
			})
			.onFailure(err -> {
				LOGGER.debug("Error while shutting down vertx: ");
				LOGGER.debug(err, err);
			});
		})
		.onFailure(err -> {
			LOGGER.debug("Error while setting up server: ");
			LOGGER.debug(err, err);
		});
	}
	

	
}
