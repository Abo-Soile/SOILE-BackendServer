package fi.abo.kogni.soile2.http_server.routes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.Test;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class RouterConsistencyTest extends ParticipationRouterTest {
	@Test	
	public void signUpTest(TestContext context)
	{
		System.out.println("--------------------  Testing SignUp  ----------------------");
		String TestDataFolder = RouterConsistencyTest.class.getClassLoader().getResource("FileTestData").getPath();

		Async creationAsync = context.async();
		createResearcher(vertx, "TestUserForSignup", "password")
		.compose(created -> {
			return createAuthedSession("TestUserForSignup", "password");
		})
		.onSuccess(authedSession -> {
			createAndStartStudy(false, "shortcut", "Testproject3")
			.onSuccess(instanceID -> {										
				signUpToProject(authedSession, instanceID)
				.compose(signedUp -> {
					// We have a signed up user, and a Task. Lets get the File we want to obtain
					return POST(authedSession, "/study/" + instanceID + "/getcurrenttaskinfo", null, null);																					
				})
				.onSuccess(inforesponse -> {
					// Now, let's see, if we can access the Circle 1.jpg file.
					String targetFileName = tmpDir + File.separator + "Circle 1.png";
					String targetFileName2 = tmpDir + File.separator + "Circle 2.png";
					String targetFileName3 = tmpDir + File.separator + "Circle 3.png";
					Async testRunResource = context.async();
					GET(authedSession, "/run/" + instanceID + "/" + inforesponse.bodyAsJsonObject().getString("id") + "/other/Circle 1.png"  , null, null)
					.compose(file -> { 					
						return vertx.fileSystem().writeFile(targetFileName , file.bodyAsBuffer());
					})
					.onSuccess(res -> {
						// compare that the file is the same as the original one.
						try {
							context.assertTrue( 
									areFilesEqual(
											new File(targetFileName),
											new File(Path.of(TestDataFolder, "other/Circle 1.png").toString())
											)
									);
							System.out.println("Execution Files are OK");
							testRunResource.complete();

						}
						catch(IOException e)
						{
							context.fail(e);
						}						
					})					 
					.onFailure(err -> context.fail(err));
					Async taskExecutionAccessAsync = context.async();
					JsonObject taskData = new JsonObject();
					POST(authedSession,"/task/list", null, null)
					.compose(taskList -> {
						taskData.put("UUID", taskList.bodyAsJsonArray().getJsonObject(0).getString("UUID"));
						return POST(authedSession,"/task/" + taskData.getString("UUID") + "/list", null, null);
					})
					.compose(taskVersionList -> {
						JsonArray versions = taskVersionList.bodyAsJsonArray();
						for(int i = 0; i < versions.size(); ++i)
						{
							if(versions.getJsonObject(i).containsKey("tag"))
							{
								taskData.put("version", versions.getJsonObject(i).getString("version"));
							}
						}
						return GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/other/Circle 1.png", null, null);
					})
					.compose(file -> { 					
						return vertx.fileSystem().writeFile(targetFileName2 , file.bodyAsBuffer());
					})
					.compose(res -> {
						// compare that the file is the same as the original one.
						try {
							context.assertTrue( 
									areFilesEqual(
											new File(targetFileName2),
											new File(Path.of(TestDataFolder, "other/Circle 1.png").toString())
											)
									);
							System.out.println("Test execution Files are OK");
							return Future.succeededFuture();

						}
						catch(IOException e)
						{							
							context.fail(e);
							return Future.failedFuture(e);
						}						
					})					 					
					.compose(done -> {
						return GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/other/Circle 1.png", null, null);
					})
					.compose(file -> { 					
						return vertx.fileSystem().writeFile(targetFileName3 , file.bodyAsBuffer());
					})
					.compose(res -> {
						// compare that the file is the same as the original one.
						try {
							context.assertTrue( 
									areFilesEqual(
											new File(targetFileName3),
											new File(Path.of(TestDataFolder, "other/Circle 1.png").toString())
											)
									);
							System.out.println("Resource Files are OK");
							return Future.succeededFuture();

						}
						catch(IOException e)
						{							
							context.fail(e);
							return Future.failedFuture(e);
						}						
					})
					.onSuccess(done -> {						
						taskExecutionAccessAsync.complete();
					})
					.onFailure(err -> context.fail(err));														
					creationAsync.complete();
				})
				.onFailure(err -> context.fail(err));				
			})
			.onFailure(err -> context.fail(err));			
		})
		.onFailure(err -> context.fail(err));
	}
}
