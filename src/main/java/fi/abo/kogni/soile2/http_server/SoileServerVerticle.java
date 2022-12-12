package fi.abo.kogni.soile2.http_server;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.projecthandling.projectElements.SoileRouteBuilding;
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
import io.vertx.ext.web.Router;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
public class SoileServerVerticle extends AbstractVerticle {

	static final Logger LOGGER = LogManager.getLogger(SoileExperimentPermissionVerticle.class);
	private JsonObject soileConfig = new JsonObject();
	SoileRouteBuilding soileRouter;
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		soileRouter = new SoileRouteBuilding();
		getConfig()
		.compose(this::storeConfig)
		.compose(this::setupFolders)
		.compose(this::deployVerticles)
		.compose(this::startHttpServer).onComplete(res ->
		{
			if(res.succeeded())
			{
				LOGGER.debug("Server started successfully");
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
	
	Future<Void> deployVerticles(Void unused)
	{
		DeploymentOptions opts = new DeploymentOptions().setConfig(soileConfig);
		List<Future> deploymentFutures = new LinkedList<Future>();
		deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle(new SoileUserManagementVerticle(), opts, promise)));
		deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle(new SoileExperimentPermissionVerticle(), opts, promise)));				
		deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle(new SoileAuthenticationVerticle(), opts, promise)));
		deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle(soileRouter, opts, promise)));
		deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle(new gitProviderVerticle(SoileConfigLoader.getServerProperty("gitVerticleAddress"), SoileConfigLoader.getServerProperty("soileGitFolder")), opts, promise )));
		//deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle("js:templateManager.js", opts, promise)));
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
		int httpPort = http_config.getInteger("port", 8080);
				
		return Future.<HttpServer>future(promise -> server.listen(httpPort,promise)).mapEmpty();	
	}
}
