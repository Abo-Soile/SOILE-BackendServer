package fi.abo.kogni.soile2.projecthandling.utils;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.SoileBaseTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.openapi.OpenAPIHolder;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.json.schema.openapi3.OpenAPI3SchemaParser;

public class HelpTest extends SoileBaseTest{

	@Override
	public void runBeforeTests(TestContext context) {
		// TODO Auto-generated method stub		
	}

	@Test
	public void testJsonSchema(TestContext context)
	{
		try {
			Async testAsync = context.async();
			String JsonToValidate = Files.readString(Paths.get(HelpTest.class.getClassLoader().getResource("testData.txt").getPath()));
			RouterBuilder.create(vertx, HelpTest.class.getClassLoader().getResource("api.yml").getFile())
			.onSuccess(routerBuilder -> {
				OpenAPIHolder holder = routerBuilder.getOpenAPI();
				JsonObject task =  holder.getOpenAPI().getJsonObject("components").getJsonObject("schemas").getJsonObject("TaskInstance");
				JsonArray contents = task.getJsonArray("allOf");
				for(Object o : contents)
				{
					System.out.println(routerBuilder.getOpenAPI().solveIfNeeded((JsonObject)o).encodePrettily());
				}
				testAsync.complete();
			});
			
		}
		catch(IOException e )
		{
			context.fail(e);
		}
	}
	
}
