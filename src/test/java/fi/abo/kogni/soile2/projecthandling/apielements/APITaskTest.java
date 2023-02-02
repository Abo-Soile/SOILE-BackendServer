package fi.abo.kogni.soile2.projecthandling.apielements;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeResourceManager;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import fi.abo.kogni.soile2.projecthandling.utils.SimpleFileUpload;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class APITaskTest extends GitTest {

	@Test
	public void testAPIResourceManagement(TestContext context)
	{		
		System.out.println("--------------------  Testing Task Save/Load ----------------------");
		Async testAsync = context.async();
		ElementManager<Task> TaskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client, vertx);
		DataLakeResourceManager grm = new DataLakeResourceManager(vertx);
		ObjectGenerator.buildAPITask(TaskManager, "FirstTask", mongo_client)
		.onSuccess(apiTask -> {
			// create a new upload.
			String fileName = vertx.fileSystem().createTempFileBlocking("SomeFile", ".ending");			
			SimpleFileUpload upload = new SimpleFileUpload(fileName, "Fun.jpg");
			grm.writeUploadToGit(new GitFile("NewFile",TaskManager.getGitIDForUUID(apiTask.getUUID()),apiTask.getVersion()), upload)
			.onSuccess(newVersion -> {
				Async newVerAsync = context.async();
				// this version should now have NewFile, while the old Version should not.
				TaskManager.getAPIElementFromDB(apiTask.getUUID(), newVersion)
				.onSuccess(element -> {
					APITask task = (APITask) element;
					System.out.println("New Task: " + task.getJson().encodePrettily());
					context.assertEquals(2,task.getResources().size());
					newVerAsync.complete();

				}).onFailure(err -> context.fail(err));
				// this one should NOT have the new File
				Async oldVerAsync = context.async();
				TaskManager.getAPIElementFromDB(apiTask.getUUID(), apiTask.getVersion())
				.onSuccess(element -> {
					APITask task = (APITask) element;
					System.out.println("Old Task: " + task.getJson().encodePrettily());
					context.assertEquals(1,task.getResources().size());
					oldVerAsync.complete();
				}).onFailure(err -> context.fail(err));
				testAsync.complete();
			})
			.onFailure(err -> context.fail(err));						
		})
		.onFailure(err -> context.fail(err));
	}

}

