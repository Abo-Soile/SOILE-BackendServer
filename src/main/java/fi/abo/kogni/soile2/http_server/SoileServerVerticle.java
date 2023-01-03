package fi.abo.kogni.soile2.http_server;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.http_server.verticles.ExperimentLanguageVerticle;
import fi.abo.kogni.soile2.http_server.verticles.SoileUserManagementVerticle;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.config.ConfigRetriever;
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
		getConfig()
		.compose(this::storeConfig)
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
		DeploymentOptions opts = new DeploymentOptions().setConfig(soileConfig);
		List<Future> unDeploymentFutures = new LinkedList<Future>();		
		for(String deploymentID : deployedVerticles)
		{
			LOGGER.debug("Trying to undeploy : " + deploymentID);
			unDeploymentFutures.add(vertx.undeploy(deploymentID));
		}
		//deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle("js:templateManager.js", opts, promise)));
		CompositeFuture.all(unDeploymentFutures).mapEmpty()
		.onSuccess(v -> stopPromise.complete())
		.onFailure(err -> stopPromise.fail(err));
		
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
		List<Future> deploymentFutures = new LinkedList<Future>();
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(new SoileUserManagementVerticle(), opts)));
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(soileRouter, opts)));
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(new gitProviderVerticle(SoileConfigLoader.getServerProperty("gitVerticleAddress"), SoileConfigLoader.getServerProperty("soileGitFolder")), opts )));
		deploymentFutures.add(addDeployedVerticle(vertx.deployVerticle(new ExperimentLanguageVerticle(), opts)));
		return CompositeFuture.all(deploymentFutures).mapEmpty();
	}
	
	Future<Void> storeConfig(JsonObject configLoadResult)
	{
		soileConfig.mergeIn(configLoadResult);
		return SoileConfigLoader.setConfigs(configLoadResult);		
	}
	
	Future<JsonObject> getConfig()
	{
		ConfigRetriever cfgRetriever = SoileConfigLoader.getRetriever(vertx);		
		return Future.future( promise -> cfgRetriever.getConfig(promise));
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
