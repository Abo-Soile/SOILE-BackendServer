package fi.abo.kogni.soile2.http_server;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.http_server.verticles.SoileUserManagementVerticle;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
public class SoileServerVerticle extends AbstractVerticle {

	static final Logger LOGGER = LogManager.getLogger(SoileServerVerticle.class);
	private JsonObject soileConfig = new JsonObject();
	SoileRouteBuilding soileRouter;
	ConcurrentLinkedQueue<String> deployedVerticles;
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		soileRouter = new SoileRouteBuilding();
		deployedVerticles = new ConcurrentLinkedQueue<>();		
		setupConfig()
		.compose(this::setupFolders)		
		.compose(this::deployVerticles)		
		.compose(this::startHttpServer).onComplete(res ->
		{
			if(res.succeeded())
			{
				LOGGER.debug("Server started successfully and listening on port " + SoileConfigLoader.getServerIntProperty("port"));
				
				startPromise.complete();
			}
			else
			{
				LOGGER.debug("Error starting server " + res.cause().getMessage());
				res.cause().printStackTrace(System.out);
				startPromise.fail(res.cause().getMessage());
			}
		});
	}
	@Override
	public void stop(Promise<Void> stopPromise) throws Exception {
		LOGGER.debug("Trying to stop Server Verticle");
		List<Future> unDeploymentFutures = new LinkedList<Future>();		
		for(String deploymentID : deployedVerticles)
		{
			LOGGER.debug("Trying to undeploy : " + deploymentID);
			unDeploymentFutures.add(vertx.undeploy(deploymentID).onFailure(err -> {
				LOGGER.debug("Couldn't undeploy " + deploymentID);
			}).onSuccess(res -> {
				LOGGER.debug("Successfully undeployed " + deploymentID);
			}));
		}
		//deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle("js:templateManager.js", opts, promise)));
		CompositeFuture.all(unDeploymentFutures).mapEmpty()
		.onSuccess(v -> {			
			stopPromise.complete();			
		})
		.onFailure(err -> {
			LOGGER.debug("Could not stop all child verticles");
			stopPromise.complete();
		});
		
	}
	
	Future<String> addDeployedVerticle(Future<String> result)
	{
		result.onSuccess(deploymentID -> {
			LOGGER.debug("Deploying verticle with id:  " + deploymentID );
			deployedVerticles.add(deploymentID);
		});
		return result;
	}
	
	Future<Void> deployVerticles(Void unused)
	{
		DeploymentOptions opts = new DeploymentOptions().setConfig(soileConfig);
		soileRouter.setDeploymentOptions(opts);
		List<Future> deploymentFutures = new LinkedList<Future>();
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(new SoileUserManagementVerticle(), opts)));
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(soileRouter, opts)));
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(new gitProviderVerticle(SoileConfigLoader.getServerProperty("gitVerticleAddress"), SoileConfigLoader.getServerProperty("soileGitFolder")), opts )));
		return CompositeFuture.all(deploymentFutures).mapEmpty();
	}
	
	Future<Void> setupConfig()
	{
		Promise<Void> finishedSetupPromise = Promise.promise();		
		SoileConfigLoader.setupConfig(vertx)
		.onSuccess(finished -> {
			soileConfig.mergeIn(SoileConfigLoader.config());
			finishedSetupPromise.complete();
		})
		.onFailure(err -> finishedSetupPromise.fail(err));
		
		return finishedSetupPromise.future();		
	}
	

	Future<Void> setupFolders(Void unused)
	{
		return createFolder(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder"))
		.compose( unusedVoid -> {
		 return createFolder(SoileConfigLoader.getServerProperty("soileResultDirectory"));	
		});		
	}

	Future<Void> createFolder(String folderName)
	{
		Promise<Void> folderCreatedPromise = Promise.<Void>promise();
		vertx.fileSystem().exists(folderName).onSuccess(exists-> {
			if(!exists)
			{
				vertx.fileSystem().mkdirs(folderName)
				.onSuccess(created -> {
					folderCreatedPromise.complete();
				});				
			}
			else
			{
				folderCreatedPromise.complete();	
			}
		}).onFailure(fail ->{
			folderCreatedPromise.fail(fail.getMessage());
		});
		return folderCreatedPromise.future();
	}
	
	
	
	Future<Void> startHttpServer(Void unused)
	{
		LOGGER.info("Starting HTTP Server");
		 JksOptions keyOptions = new JksOptions()
		    .setPath("server-keystore.jks")
		    .setPassword("secret");		    

		    
		JsonObject http_config = soileConfig.getJsonObject("http_server");
		HttpServerOptions opts = new HttpServerOptions()
									 .setLogActivity(true) 
									 .setSsl(true)
									 .setKeyStoreOptions(keyOptions);
		HttpServer server = vertx.createHttpServer(opts).requestHandler(soileRouter.getRouter());
		int httpPort = http_config.getInteger("port", SoileConfigLoader.getServerIntProperty("port"));			
		return Future.<HttpServer>future(promise -> server.listen(httpPort,promise)).mapEmpty();	
	}
}
