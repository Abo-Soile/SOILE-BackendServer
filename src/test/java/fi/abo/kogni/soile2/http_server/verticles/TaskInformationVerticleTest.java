package fi.abo.kogni.soile2.http_server.verticles;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class TaskInformationVerticleTest extends SoileVerticleTest {

	@Test
	public void taskInfoTest(TestContext context)
	{		
		System.out.println("--------------------  Testing Task Information retrieval ----------------------");

		Async testAsync = context.async();
		ElementManager<Task> taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,vertx);
		ObjectGenerator.buildAPITask(taskManager, "FirstTask", mongo_client)
		.onSuccess(currentTask -> {
			String taskID = currentTask.getUUID();
			Async versionAsync = context.async();
			eb.request("soile.task.getVersionInfo", new JsonObject().put("taskID",taskID).put("version", currentTask.getVersion()))
			.onSuccess(response -> {
				JsonObject taskData = ((JsonObject)response.body()).getJsonObject(SoileCommUtils.DATAFIELD);
				context.assertEquals("elang", taskData.getJsonObject("codeType").getString("language"));
				versionAsync.complete();
			})
			.onFailure(err -> context.fail(err));				
			Async infoAsync = context.async();
			eb.request("soile.task.getDBInfo", new JsonObject().put("taskID",taskID))
			.onSuccess(response -> {
				JsonObject taskData = ((JsonObject)response.body()).getJsonObject(SoileCommUtils.DATAFIELD);				
				context.assertEquals(taskID, taskData.getString("_id"));
				context.assertTrue(taskData.getBoolean("private"));
				infoAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			
			testAsync.complete();
		})
		.onFailure(err -> context.fail(err));	
	}
}
