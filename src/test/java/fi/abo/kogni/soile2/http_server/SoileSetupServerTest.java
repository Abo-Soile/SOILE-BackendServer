package fi.abo.kogni.soile2.http_server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class SoileSetupServerTest extends MongoTest{


	@Test
	public void testSetup(TestContext context){
		Async serverSetupAsync = context.async();
		try {
			Vertx setupVertx = Vertx.vertx();
			JsonObject setupConf = new JsonObject(Files.readString(Paths.get(SoileSetupServerTest.class.getClassLoader().getResource("setup.json").getPath())));
			// for testing there is no specific data Folder
			setupVertx.deployVerticle(new SetupServer(null), new DeploymentOptions())
			.onSuccess(serverVerticleID -> 
			{			
				mongo_client.find(SoileConfigLoader.getDbCfg().getString("userCollection"), new JsonObject())
				.onSuccess(res -> {
					if(res.size() != 1)
					{
						context.fail("Expected exactly one user to have been set up");
					}
					else
					{
						context.assertEquals(setupConf.getString("adminuser"), res.get(0).getString("username"));
						mongo_client.find(SoileConfigLoader.getDbCfg().getString("studyCollection"), new JsonObject())
						.onSuccess(projectRes -> {
							context.assertEquals(2,projectRes.size());
							// now lets just check the content of one of those Projects.
							JsonObject p1 = projectRes.get(0);
							String version = p1.getString("version");
							String gitID = "P" + p1.getString("sourceUUID");
							GitFile targetFile = new GitFile("Object.json", gitID, version);
							setupVertx.eventBus().request("soile.git.getGitFileContentsAsJson", targetFile.toJson())
							.onSuccess(reply -> {

								JsonObject projectObject = (JsonObject)reply.body();
								JsonArray tasks = projectObject.getJsonArray("tasks");
								boolean checkedType = false;
								for(int i = 0; i < tasks.size(); ++i)
								{
									JsonObject cTask = tasks.getJsonObject(i);
									if(cTask.getString("name").equals("QuestionnaireExample"))
									{
										context.assertEquals("qmarkup", cTask.getJsonObject("codeType").getString("language"));
										checkedType = true; 
									}
								}
								
								context.assertTrue(checkedType);
								setupVertx.undeploy(serverVerticleID).
								onSuccess(undeployed -> {
									serverSetupAsync.complete();	
								})
								.onFailure(err -> context.fail(err));

							})
							.onFailure(err -> context.fail(err));
						})					
						.onFailure(err -> context.fail(err));															
					}
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}

	
	@Test
	public void testSetupContent(TestContext context){
		Async serverSetupAsync = context.async();
		try {
			Vertx setupVertx = Vertx.vertx();
			JsonObject setupConf = new JsonObject(Files.readString(Paths.get(SoileSetupServerTest.class.getClassLoader().getResource("setup.json").getPath())));
			// for testing there is no specific data Folder
			setupVertx.deployVerticle(new SetupServer(null), new DeploymentOptions())
			.onSuccess(serverVerticleID -> 
			{			
				mongo_client.find(SoileConfigLoader.getDbCfg().getString("projectCollection"), new JsonObject())
				.onSuccess(projectList -> {
					context.assertEquals(1, projectList.size());
					String projectID = projectList.get(0).getString("_id");
					String projectVersion = projectList.get(0).getJsonArray("tags").getJsonObject(0).getString("version"); // this is the initial version. Can't be anything else.
					GitFile f = new GitFile("Object.json", "P" + projectID, projectVersion);
					setupVertx.eventBus().request("soile.git.getGitFileContentsAsJson", f.toJson())
					.onSuccess(response -> {
						JsonObject projectContent = (JsonObject)response.body();
						context.assertEquals(2, projectContent.getJsonArray("tasks").size());
						context.assertEquals(1, projectContent.getJsonArray("experiments").size());
						context.assertEquals(1, projectContent.getJsonArray("filters").size());
						setupVertx.undeploy(serverVerticleID).
						onSuccess(undeployed -> {
							serverSetupAsync.complete();	
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}

	@Test
	public void testReSetup(TestContext context){
		Async serverSetupAsync = context.async();
		try {
			Vertx setupVertx = Vertx.vertx();
			JsonObject setupConf = new JsonObject(Files.readString(Paths.get(SoileSetupServerTest.class.getClassLoader().getResource("setup.json").getPath())));
			// for testing there is no specific data Folder
			setupVertx.deployVerticle(new SetupServer(null), new DeploymentOptions())
			.onSuccess( initID -> 
			{			
				// undeploy the server setup (and thus the server)
				System.out.println("Undeploying setup verticle");
				setupVertx.undeploy(initID).map(initID)
				.onSuccess(undeployed -> {
						
					Vertx newVertx = Vertx.vertx();
					newVertx.deployVerticle(new SetupServer(null), new DeploymentOptions())
					.onSuccess(serverReStarted -> {
						mongo_client.find(SoileConfigLoader.getDbCfg().getString("userCollection"), new JsonObject())
						.onSuccess(res -> {
							if(res.size() != 1)
							{
								context.fail("Expected exactly one user to have been set up");
							}
							else
							{
								context.assertEquals(setupConf.getString("adminuser"), res.get(0).getString("username"));
								mongo_client.find(SoileConfigLoader.getDbCfg().getString("studyCollection"), new JsonObject())
								.onSuccess(projectRes -> {
									context.assertEquals(2,projectRes.size());								
									serverSetupAsync.complete();	
								})					
								.onFailure(err -> context.fail(err));															
							}
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}
}
