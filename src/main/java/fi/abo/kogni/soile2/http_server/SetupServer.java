package fi.abo.kogni.soile2.http_server;

import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.verticles.PermissionVerticle;
import fi.abo.kogni.soile2.http_server.verticles.SoileUserManagementVerticle;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;

public class SetupServer extends AbstractVerticle {

	static final Logger LOGGER = LogManager.getLogger(SetupServer.class);
	private JsonObject soileConfig = new JsonObject();	
	ConcurrentLinkedQueue<String> deployedVerticles;
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {		
		deployedVerticles = new ConcurrentLinkedQueue<>();		
		
		setupConfig() // As the very first step we need to set up the config so that it is available for all later steps.			
		.compose(this::deployVerticles) // deploy all necessary verticles for setup.
		.compose(this::setupServer)
		.onComplete(res ->
		{
			if(res.succeeded())
			{
				LOGGER.debug("Server successfully set up, you can now start it.");				
				startPromise.complete();
				vertx.close();
				// we are done.
			}
			else
			{
				res.cause().printStackTrace(System.out);
				LOGGER.debug("Error setting up the server " + res.cause().getMessage());
				startPromise.fail(res.cause().getMessage());
			}
		});
	}
	@Override
	public void stop(Promise<Void> stopPromise) throws Exception {
		@SuppressWarnings("rawtypes")
		List<Future> unDeploymentFutures = new LinkedList<Future>();		
		for(String deploymentID : deployedVerticles)
		{
			unDeploymentFutures.add(vertx.undeploy(deploymentID));
		}
		//deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle("js:templateManager.js", opts, promise)));
		CompositeFuture.all(unDeploymentFutures).mapEmpty()
		.onSuccess(v -> {			
			stopPromise.complete();			
		})
		.onFailure(err -> {
			stopPromise.complete();
		});
		
	}
	
	Future<String> addDeployedVerticle(Future<String> result)
	{
		result.onSuccess(deploymentID -> {
			deployedVerticles.add(deploymentID);
		});
		return result;
	}
	
	Future<Void> deployVerticles(Void unused)
	{
		DeploymentOptions opts = new DeploymentOptions().setConfig(soileConfig);
		@SuppressWarnings("rawtypes")
		List<Future> deploymentFutures = new LinkedList<Future>();
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(new SoileUserManagementVerticle(), opts)));
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(new PermissionVerticle(), opts )));
		return CompositeFuture.all(deploymentFutures).mapEmpty();
	}
	
	Future<Void> setupConfig()
	{
		Promise<Void> finishedSetupPromise = Promise.promise();
		LOGGER.info("Setting up Config");
		SoileConfigLoader.setupConfig(vertx)
		.onSuccess(finished -> {
			soileConfig.mergeIn(SoileConfigLoader.config());
			finishedSetupPromise.complete();
		})
		.onFailure(err -> finishedSetupPromise.fail(err));
		
		return finishedSetupPromise.future();		
	}
	

	Future<Void> setupServer(Void unused)
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

	
	

	
}
