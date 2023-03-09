package fi.abo.kogni.soile2.http_server.routes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.DataLakeManagerTest;
import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;

public class TaskRouterTest extends SoileWebTest{

	@Test
	public void testGetResource(TestContext context)
	{		 
		System.out.println("--------------------  Testing Web get Task Resources  ----------------------");

		Async testAsync = context.async();
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {				 
				WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
				.onSuccess(taskData -> {
					Async testResource = context.async();
					String resourceAddress = "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/ImageData.jpg";
					GET(authedSession,resourceAddress,null,null )
					.onSuccess(result -> {
						String targetFileName = tmpDir + File.separator + "taskRouter.out"; 
						vertx.fileSystem().writeFile(targetFileName , result.bodyAsBuffer())
						.onSuccess(
								res -> 
								{
									try {
										context.assertTrue( 
												areFilesEqual(
														new File(targetFileName),
														new File(DataLakeManagerTest.class.getClassLoader().getResource("FileTestData/ImageData.jpg").getPath())
														)
												);
										testResource.complete();

									}
									catch(IOException e)
									{
										context.fail(e);
									}
								}
								// compare that the file is the same as the original one.
								)
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
					Async invalidAccessAsync = context.async();
					GET(wrongSession,resourceAddress,null,null )
					.onSuccess(err -> {
						context.fail("Should not be allowed");
					})
					.onFailure(err -> {
						context.assertEquals(403, ((HttpException)err).getStatusCode());
						invalidAccessAsync.complete();
					});

					Async invalidFileAsync = context.async();					 
					GET(authedSession,"/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/Something.jpg",null,null )
					.onSuccess(err -> {
						context.fail("Should not be possible");
					})
					.onFailure(err -> {
						context.assertEquals(404, ((HttpException)err).getStatusCode());
						invalidFileAsync.complete();
					});


					testAsync.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));

		})
		.onFailure(err -> context.fail(err));
	}


	@Test
	public void testRunResources(TestContext context)
	{

		Async testRunResAsync = context.async();
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {				 
				WebObjectCreator.createOrRetrieveTask(authedSession, "Initial_Questionaire")
				.onSuccess(taskData -> {									
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/ImageData.jpg", null, null)
					.onSuccess(failed -> {
						context.fail("The first Task does NOT have any resources!");
					})
					.onFailure(response -> {
						context.assertEquals(404, ((HttpException)response).getStatusCode());
						WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
						.onSuccess(taskWithImage -> {
							System.out.println(taskWithImage.encodePrettily());
							Async invalidAccess = context.async();						 
							GET(wrongSession, "/task/" + taskWithImage.getString("UUID") + "/" + taskWithImage.getString("version") + "/execute/ImageData.jpg", null, null)
							.onSuccess(failed -> {
								context.fail("The first Task does NOT have any resources!");
							})
							.onFailure(invalid -> {
								context.assertEquals(403, ((HttpException)invalid).getStatusCode());
								invalidAccess.complete();
							});
							GET(authedSession, "/task/" + taskWithImage.getString("UUID") + "/" + taskWithImage.getString("version") + "/execute/ImageData.jpg", null, null)
							.onSuccess(dataResponse ->  {
								String targetFileName = tmpDir + File.separator + "taskRouter.out"; 
								vertx.fileSystem().writeFile(targetFileName , dataResponse.bodyAsBuffer())
								.onSuccess(res -> {
									// compare that the file is the same as the original one.
									try {
										context.assertTrue( 
												areFilesEqual(
														new File(targetFileName),
														new File(DataLakeManagerTest.class.getClassLoader().getResource("FileTestData/ImageData.jpg").getPath())
														)
												);
										testRunResAsync.complete();

									}
									catch(IOException e)
									{
										context.fail(e);
									}
								})					 
								.onFailure(err -> context.fail(err));
							})
							.onFailure(err -> context.fail(err));

						})
						.onFailure(err -> context.fail(err));

					});
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void testRunLibs(TestContext context)
	{
		System.out.println("--------------------  Running Tests for /run/{id}/lib/*  ----------------------");    

		Async testRunResource = context.async();
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {				 
				WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
				.onSuccess(taskData -> {
				GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/lib/testlib.js", null, null)
				.onSuccess(response -> {
					context.assertTrue(response.bodyAsString().contains("console.log"));
					GET(wrongSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/lib/testlib.js", null, null)
					.onFailure(fail -> {
						context.assertEquals(403, ((HttpException)fail).getStatusCode());
						
						testRunResource.complete();
					})
					.onSuccess(err -> context.fail("Should not be allowed"));	
				})
				.onFailure(err -> context.fail(err));
				Async failedLibAsync = context.async();
				GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/lib/testlib2.js", null, null)
				.onSuccess(response -> {
					context.fail("Should not be possible");
				})
				.onFailure(err ->
				{
					if( err instanceof HttpException)
					{
						context.assertEquals(404, ((HttpException)err).getStatusCode());
						failedLibAsync.complete();
					}
					else
					{
						context.fail(err);
					}

				});
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void runTaskTest(TestContext context)
	{
		Async runTestAsync = context.async();	
		System.out.println("--------------------  Running Tests for /task/{id}/{version}/execute ----------------------");    
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {	
				Async initQuest = context.async();				
				WebObjectCreator.createOrRetrieveTask(authedSession, "Initial_Questionaire")
				.onSuccess(taskData -> {									
					
					Async correctSessionAsync  = context.async();
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> {
						context.assertEquals("application/json", response.headers().get("content-type"));					
						JsonObject compiledCode = response.bodyAsJsonObject();
						context.assertTrue(compiledCode.containsKey("elements"));
						context.assertEquals("html", compiledCode.getJsonArray("elements").getJsonArray(0).getJsonObject(0).getString("type"));
						context.assertEquals("",compiledCode.getString("title"));						
						correctSessionAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async incorrectSessionAsync  = context.async();
					GET(wrongSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> {
						context.assertEquals("application/json", response.headers().get("content-type"));					
						JsonObject compiledCode = response.bodyAsJsonObject();
						context.assertTrue(compiledCode.containsKey("elements"));
						context.assertEquals("html", compiledCode.getJsonArray("elements").getJsonArray(0).getJsonObject(0).getString("type"));
						context.assertEquals("",compiledCode.getString("title"));						
						incorrectSessionAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					initQuest.complete();
				})
				.onFailure(err -> context.fail(err));
				Async firstTask = context.async();				
				WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
				.onSuccess(taskData -> {									
					
					Async correctSessionAsync  = context.async();
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> {
						context.assertEquals("application/javascript", response.headers().get("content-type"));					
						String compiledCode = response.bodyAsString();
						context.assertTrue(compiledCode.contains("SOILE2"));
						context.assertTrue(compiledCode.contains("opcode"));
						correctSessionAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async incorrectSessionAsync  = context.async();
					GET(wrongSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> context.fail("User has no Access"))																
					.onFailure(invalid -> {
						context.assertEquals(403, ((HttpException)invalid).getStatusCode());
						incorrectSessionAsync.complete();
					});
					firstTask.complete();
				})
				.onFailure(err -> context.fail(err));		
				runTestAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
}
