package fi.abo.kogni.soile2.http_server.routes;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.HttpException;

//TODO: Test Project deletion and Project Stop.
public class ProjectinstanceRouterTest extends SoileWebTest {

	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testStartProject(TestContext context)
	{
		System.out.println("--------------------  Running Start Project test  ----------------------");    

		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
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
	public void testlistProjects(TestContext context)
	{
		System.out.println("--------------------  Running Start Project test  ----------------------");    

		JsonObject projectExec1 = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut");
		JsonObject projectExec2 = new JsonObject().put("private", false).put("name", "New Project2").put("shortcut","newShortcut2");
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebClient unAuthedSession = createSession();
				
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					Async startAsync = context.async();

					POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec1 )
					.onSuccess(response1 -> {
						String id1 = response1.bodyAsJsonObject().getString("projectID");

						POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec2 )
						.onSuccess(response2 -> {
						Async listAsync = context.async();
						String id2 = response2.bodyAsJsonObject().getString("projectID");
						GET(authedSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(2, listresponse.bodyAsJsonArray().size());
							List<String> uuids = new LinkedList<>();
							for(int i = 0 ; i < listresponse.bodyAsJsonArray().size(); i++)
							{
								uuids.add(listresponse.bodyAsJsonArray().getJsonObject(i).getString("uuid"));								
							}
							context.assertTrue(uuids.contains(id1));
							context.assertTrue(uuids.contains(id2));
							listAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						Async emptyListAsync = context.async();
						
						GET(wrongSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(1, listresponse.bodyAsJsonArray().size());
							context.assertEquals(id2, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));

							emptyListAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						
						Async unAuthAsync = context.async();
						
						GET(unAuthedSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(1, listresponse.bodyAsJsonArray().size());
							context.assertEquals(id2, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));

							unAuthAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						
						startAsync.complete();
					})
					.onFailure(err -> context.fail(err));	
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
		System.out.println("--------------------  Testing Start/Stop project  ----------------------");    

		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					Async startAsync = context.async();

					POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )
					.onSuccess(response -> {
						Async listAsync = context.async();
						Async emptyListAsync = context.async();
						String id = response.bodyAsJsonObject().getString("projectID");
						GET(authedSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(1, listresponse.bodyAsJsonArray().size());
							context.assertEquals(id, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));
							POST(authedSession, "/projectexec/" + id + "/stop", null,null )
							.onSuccess( stopped -> {
								POST(authedSession, "/projectexec/" + id + "/signup", null,null)
								.onSuccess(res -> context.fail("This should fail because the project is inactive"))
								.onFailure(rejected -> {
									context.assertEquals(410, ((HttpException)rejected).getStatusCode());
									POST(authedSession, "/projectexec/" + id + "/restart", null,null )
									.onSuccess( restarted -> {
										POST(authedSession, "/projectexec/" + id + "/signup", null,null)
										.onSuccess(res -> {											
											listAsync.complete();																		
										})
										.onFailure(err -> context.fail(err));
									})
									.onFailure(err -> context.fail(err));


								});
							})
							.onFailure(err -> context.fail(err));

						})
						.onFailure(err -> context.fail(err));
						
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
