package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ProjectRouterTest extends SoileWebTest {

	@Test
	public void testFilterProject(TestContext context)
	{
		System.out.println("--------------------  Running Filter Test  ----------------------");    

		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			JsonObject validRequest = new JsonObject().put("filter", "Element1 + Element2 > 4")
											   .put("parameters", new JsonObject().put("Element1", 1).put("Element2", 2));
			JsonObject inValidRequest1 = new JsonObject().put("filter", "Element1 + Element2 > 4")
					   .put("parameters", new JsonObject().put("Element1", 1));
			JsonObject inValidRequest2 = new JsonObject().put("filter", "Element1 + Element2 >= 4")
					   .put("parameters", new JsonObject().put("Element1", 1).put("Element2", 2));
			Async validAsync = context.async();
			POST(authedSession, "/project/testfilter", null, validRequest)
			.onSuccess(response -> {
				context.assertTrue(response.bodyAsJsonObject().getBoolean("valid"));
				validAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			Async invalidAsync1 = context.async();
			POST(authedSession, "/project/testfilter", null, inValidRequest1)
			.onSuccess(response -> {
				context.assertEquals("Unknown function or variable 'Element2' at pos 11 in expression 'Element1 + Element2 > 4'",response.bodyAsJsonObject().getString("error")); 
				context.assertFalse(response.bodyAsJsonObject().getBoolean("valid"));
				invalidAsync1.complete();
			})
			.onFailure(err -> context.fail(err));
			Async invalidAsync2 = context.async();
			POST(authedSession, "/project/testfilter", null, inValidRequest2)
			.onSuccess(response -> {
				System.out.println(response.bodyAsJsonObject().encodePrettily());

				context.assertFalse(response.bodyAsJsonObject().getBoolean("valid"));
				invalidAsync2.complete();
			})
			.onFailure(err -> context.fail(err));
			setupAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
}
