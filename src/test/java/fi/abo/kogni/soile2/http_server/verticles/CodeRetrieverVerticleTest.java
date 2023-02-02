package fi.abo.kogni.soile2.http_server.verticles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class CodeRetrieverVerticleTest extends SoileVerticleTest {

	@Test
	public void elangTest(TestContext context)
	{		
		System.out.println("--------------------  Testing Elang Verticle ----------------------");
		try
		{			
			String originalCode2 = Files.readString(Paths.get(CodeRetrieverVerticleTest.class.getClassLoader().getResource("CodeTestData/FirstTask.elang").getPath()));
			Async compilation2Async = context.async();
			JsonObject CompileRequest = new JsonObject().put("code", originalCode2).put("type", CodeRetrieverVerticle.ELANG);			
			vertx.eventBus().request(SoileConfigLoader.getVerticleProperty("compilationAddress"), CompileRequest)
			.onSuccess(reply-> {
				JsonObject response = (JsonObject) reply.body();
				// this could be made more explicit, testing actual contents.
				context.assertNotNull(response.getString("code"));
				context.assertTrue(response.getString("code").contains("SOILE2"));
				compilation2Async.complete();				
			})
			.onFailure(err -> context.fail(err));
			String failingCode = Files.readString(Paths.get(CodeRetrieverVerticleTest.class.getClassLoader().getResource("CodeTestData/FirstTask_Error.elang").getPath()));			
			Async compilationAsync = context.async();
			JsonObject CompileRequest2 = new JsonObject().put("code", failingCode).put("type", CodeRetrieverVerticle.ELANG);			
			vertx.eventBus().request(SoileConfigLoader.getVerticleProperty("compilationAddress"), CompileRequest2)
			.onSuccess(reply-> {
				context.fail("Should have failed since code does not compile");								
			})
			.onFailure(err -> {
				compilationAsync.complete();
			});

		}
		catch(IOException e)
		{
			context.fail(e);
		}

	}
	
	@Test
	public void qmarkupTest(TestContext context)
	{
		System.out.println("--------------------  Testing Questionair Verticle ----------------------");
		try 
		{
			String originalCode2 = Files.readString(Paths.get(CodeRetrieverVerticleTest.class.getClassLoader().getResource("CodeTestData/pilotform.qmarkup").getPath()));
			Async compilation2Async = context.async();
			JsonObject CompileRequest = new JsonObject().put("code", originalCode2).put("type", CodeRetrieverVerticle.QMARKUP);			
			vertx.eventBus().request(SoileConfigLoader.getVerticleProperty("compilationAddress"), CompileRequest)
			.onSuccess(reply-> {
				JsonObject response = (JsonObject) reply.body();
				// this could be made more explicit, testing actual contents.
				context.assertNotNull(response.getString("code"));
				JsonObject codeObject = new JsonObject(response.getString("code"));
				context.assertTrue(codeObject.containsKey("elements"));
				compilation2Async.complete();				
			})
			.onFailure(err -> context.fail(err));
			String failingCode = Files.readString(Paths.get(CodeRetrieverVerticleTest.class.getClassLoader().getResource("CodeTestData/pilotform2_error.qmarkup").getPath()));			
			Async compilationAsync = context.async();
			JsonObject CompileRequest2 = new JsonObject().put("code", failingCode).put("type", CodeRetrieverVerticle.QMARKUP);			
			vertx.eventBus().request(SoileConfigLoader.getVerticleProperty("compilationAddress"), CompileRequest2)
			.onSuccess(reply-> {
				context.fail("Should have failed since code does not compile");								
			})
			.onFailure(err -> {
				context.assertTrue(err.getMessage().contains("numberfiel"));
				compilationAsync.complete();			
			});

		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}		

}
