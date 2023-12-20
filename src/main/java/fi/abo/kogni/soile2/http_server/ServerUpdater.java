package fi.abo.kogni.soile2.http_server;

import java.util.concurrent.ConcurrentLinkedQueue;

import fi.abo.kogni.soile2.migrations.TaskFieldAddition;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.mongo.MongoClient;

public class ServerUpdater extends SoileServerVerticle{
	
	MongoClient client;

	public Future<Void> updateServer(Void empty) {
		client = MongoClient.createShared(vertx, SoileConfigLoader.getMongoCfg());
		TaskFieldAddition adder = new TaskFieldAddition(client);
		return adder.run();
		
	}
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		soileRouter = new SoileRouteBuilding();
		deployedVerticles = new ConcurrentLinkedQueue<>();					
		setupConfig() // As the very first stepp we need to set up the config so that it is available for all later steps.
		.compose(this::setupFolders) // Set up all necessary folders		
		.compose(this::deployVerticles) // deploy all necessary verticles (including routing etc), but don't actually start the server...
		.compose(this::updateServer)
		.onSuccess(res ->
		{			
			LOGGER.info("Server updated successfully");
			startPromise.complete();
		})
		.onFailure(err ->
		{			
			LOGGER.info("Server update Failed");
			LOGGER.error(err, err);
			startPromise.fail(err);
		});
	}
	
	@Override
	public void stop(Promise<Void> stopPromise) throws Exception {
		LOGGER.debug("Stopping Server Verticle");					
		undeploy(stopPromise);
	}
}
