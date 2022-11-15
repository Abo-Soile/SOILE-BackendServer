package fi.abo.kogni.soile2.experiment.task;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class TaskBuilder {
	EventBus eb;
	String taskDirectory;
	
	
	/**
	 * Create the Json configuration of a task. 
	 * @param taskToBuild
	 * @param TaskHandler
	 */
	public void buildTask(JsonObject taskToBuild, Handler<AsyncResult<Task>> TaskHandler)
	{
		Promise<Task> promise = Promise.promise();
/*		JsonObject command = new JsonObject().put(gitProviderVerticle.COMMANDFIELD, gitProviderVerticle.GET_VERSION_COMMAND)
											 .put(gitProviderVerticle.VERSIONFIELD,taskToBuild.getString(SoileConfigLoader.getTaskProperty("version")))
											 .put(gitProviderVerticle.REPOFIELD,taskToBuild.getString(SoileConfigLoader.getTaskProperty("id")));
		eb.request(SoileConfigLoader.getTaskProperty("repoTaskAddress"), command, msg ->
		{
			if(msg.succeeded())
			{
				JsonObject result = (JsonObject)msg.result();
				Task newTask = new Task(result.getString(SoileConfigLoader.getTaskProperty("id")),result.getString(SoileConfigLoader.getTaskProperty("version")));
				JsonObject metaData = new JsonObject(result.getJsonObject(gitProviderVerticle.DATAFIELD).getString("metaData")); 
			}
		});*/
	}
}
