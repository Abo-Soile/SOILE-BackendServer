package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.utils.WebObjectCreator;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ProjectinstanceRouterTest extends SoileWebTest {

	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testStartProject(TestContext context)
	{
		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					System.out.println("Retrieved json after creation: \n" + projectData.encodePrettily());
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					Async startAsync = context.async();

					POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )
					.onSuccess(response -> {
						Async listAsync = context.async();
						String id = response.bodyAsJsonObject().getString("projectID");
						GET(authedSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(1, listresponse.bodyAsJsonArray().size());
							context.assertEquals(id, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));
							listAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						Async emptyListAsync = context.async();
						
						GET(wrongSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(0, listresponse.bodyAsJsonArray().size());
							emptyListAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						startAsync.complete();
					})
					.onFailure(err -> context.fail(err));	
					
					setupAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	
	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testStopRestart(TestContext context)
	{
		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					System.out.println("Retrieved json after creation: \n" + projectData.encodePrettily());
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					Async startAsync = context.async();

					POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )
					.onSuccess(response -> {
						Async listAsync = context.async();
						String id = response.bodyAsJsonObject().getString("projectID");
						GET(authedSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(1, listresponse.bodyAsJsonArray().size());
							context.assertEquals(id, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));
							listAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						Async emptyListAsync = context.async();
						
						GET(wrongSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(0, listresponse.bodyAsJsonArray().size());
							emptyListAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						startAsync.complete();
					})
					.onFailure(err -> context.fail(err));	
					
					setupAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
		
}
