package fi.abo.kogni.soile2.http_server;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class UpdaterTest extends MongoTest{


	@Test
	public void testTaskUpdate(TestContext context){
		Async serverUpdateAsync = context.async();
		JsonObject oldTask = new JsonObject().put("name", "Test Task")
											 .put("versions", new JsonArray().add(new JsonObject().put("version", "12345")
													 											  .put("timestamp", 2L))
													 						 .add(new JsonObject().put("version", "123456")
													 											  .put("timestamp", 1L))
													 						.add(new JsonObject().put("version", "1234567")
										 											  .put("timestamp", 7L))
													 						 )
													 						 
											 .put("tags", new JsonArray().add(new JsonObject().put("version", "12345")
		 											  										  .put("tag", "Latest"))
													 )
											 .put("private",false)
											 .put("dependencies", new JsonObject())
											 .put("visible", true);			
		System.out.println("Testing task update");
		mongo_client.insert(SoileConfigLoader.getCollectionName("taskCollection"), oldTask)
		.compose(res -> {
			System.out.println(vertx.deploymentIDs());
			return vertx.deployVerticle(new ServerUpdater());					
		})
		.compose(deployed -> {			
			return mongo_client.findOne(SoileConfigLoader.getCollectionName("taskCollection"), new JsonObject().put("name", "Test Task"), null);
			
		})
		.onSuccess(updatedTask -> {
			System.out.println(updatedTask.encodePrettily());
			context.assertTrue(updatedTask.containsKey("author"));
			context.assertEquals(1L, updatedTask.getLong("created"));
			serverUpdateAsync.complete();
		})
		.onFailure(err -> context.fail(err));

	}

	
}

