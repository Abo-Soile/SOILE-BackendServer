package fi.abo.kogni.soile2.http_server.verticles;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * Verticle to handle task information requests. 
 * @author Thomas Pfau
 *
 */
public class TaskInformationverticle extends AbstractVerticle{

	
	ElementManager<Task> taskManager;	
	public static final Logger LOGGER = LogManager.getLogger(TaskInformationverticle.class);

	
	@Override
	public void start()
	{
		taskManager = new ElementManager<Task>(Task::new, APITask::new, MongoClient.createShared(vertx, config().getJsonObject("db")), vertx);
		LOGGER.debug("Deploying TaskInformation with id : " + deploymentID());
		vertx.eventBus().consumer(SoileConfigLoader.getVerticleProperty("getTaskInformationAddress"), this::getTaskInfo);
	}
					

	
	
	@Override
	public void stop(Promise<Void> stopPromise)
	{
		List<Future> undeploymentFutures = new LinkedList<Future>();
		undeploymentFutures.add(vertx.eventBus().consumer(SoileConfigLoader.getVerticleProperty("getTaskInformationAddress"), this::getTaskInfo).unregister());		
		CompositeFuture.all(undeploymentFutures).mapEmpty().
		onSuccess(v -> {
			LOGGER.debug("Successfully undeployed TaskInformationVerticle with id: " + deploymentID());
			stopPromise.complete();
		})
		.onFailure(err -> stopPromise.fail(err));			
	}

	/**
	 * Get information on the requested Task.
	 * @param request
	 */
	public void getTaskInfo(Message<JsonObject> request)
	{
		String taskID = request.body().getString("taskID");
		taskManager.getElement(taskID)
		.onSuccess(task -> {
			request.reply(SoileCommUtils.successObject().put(SoileCommUtils.DATAFIELD, task.toJson()));
		})
		.onFailure(err -> {			
			request.fail(500, err.getMessage());
		});
	}
}
