package fi.abo.kogni.soile2.http_server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class SoileSetupServerTest extends GitTest{
	
	
	@Test
	public void testSetup(TestContext context){
		Async serverSetupAsync = context.async();
		try {
			Vertx setupVertx = Vertx.vertx();
		JsonObject setupConf = new JsonObject(Files.readString(Paths.get(SoileSetupServerTest.class.getClassLoader().getResource("setup.json").getPath())));
		// for testing there is no specific data Folder
		setupVertx.deployVerticle(new SetupServer(null), new DeploymentOptions())
		.onSuccess(Void -> 
		{			
			System.out.println("Server set up, retrieving information");
			mongo_client.find(SoileConfigLoader.getDbCfg().getString("userCollection"), new JsonObject())
			.onSuccess(res -> {
				if(res.size() != 1)
				{
					context.fail("Expected exactly one user to have been set up");
				}
				else
				{
					context.assertEquals(setupConf.getString("adminuser"), res.get(0).getString("username"));
					mongo_client.find(SoileConfigLoader.getDbCfg().getString("projectInstanceCollection"), new JsonObject())
					.onSuccess(projectRes -> {
						context.assertEquals(2,projectRes.size());	
						System.out.println(projectRes);
						serverSetupAsync.complete();	
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
}
