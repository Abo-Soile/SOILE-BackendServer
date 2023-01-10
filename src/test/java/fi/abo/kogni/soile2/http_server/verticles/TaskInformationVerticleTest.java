package fi.abo.kogni.soile2.http_server.verticles;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class TaskInformationVerticleTest extends SoileVerticleTest {

	@Test
	public void taskInfoTest(TestContext context)
	{		
		Async testAsync = context.async();
		ElementManager<Task> taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,new GitManager(eb));
		ObjectGenerator.buildAPITask(taskManager, "FirstTask", mongo_client)
		.onSuccess(currentTask -> {
			String taskID = currentTask.getUUID();
			eb.request(SoileConfigLoader.getVerticleProperty("getTaskInformationAddress"), new JsonObject().put("taskID",taskID))
			.onSuccess(response -> {
				JsonObject taskData = ((JsonObject)response.body()).getJsonObject(SoileCommUtils.DATAFIELD);
				context.assertEquals(taskID, taskData.getString("_id"));
				context.assertTrue(taskData.getBoolean("private"));
				context.assertEquals("elang", taskData.getString("codeType"));
				testAsync.complete();
			})
			.onFailure(err -> context.fail(err));						
		})
		.onFailure(err -> context.fail(err));	
	}
}
