package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;

public class ElementRouterTest extends SoileWebTest {


	@Test
	public void testBuildWebTask(TestContext context)
	{	
		System.out.println("--------------------  Testing Task generation ----------------------");

		Async setupAsync = context.async();
		WebClientSession currentSession = createSession();
		createUser(vertx, "TestUser", "testPassword", Roles.Admin)
		.onSuccess(userCreated -> {
			authenticateSession(currentSession, "TestUser", "testPassword")
			.onSuccess(authed -> {
				WebObjectCreator.createTask(currentSession, "Test2")
				.onSuccess(taskData -> {
					Async taskInfoAsync = context.async();
					String taskID = taskData.getString("UUID");
					String taskVersion = taskData.getString("version");
					getElement(currentSession, "task", taskID, taskVersion)
					.onSuccess(newData -> {
						context.assertEquals(taskID, newData.getString("UUID"));
						context.assertEquals(taskVersion, newData.getString("version"));
						context.assertEquals(taskData.getString("code"), newData.getString("code"));
						context.assertTrue(taskData.getString("code").contains("intermezzo"));
						taskInfoAsync.complete();
					})
					.onFailure(err -> context.fail(err));

					Async taskListAsync = context.async();

					getElementList(currentSession, "task")
					.onSuccess(taskList-> {
						context.assertEquals(1, taskList.size());
						context.assertEquals(taskID, taskList.getJsonObject(0).getValue("uuid"));
						context.assertEquals(taskData.getString("name"), taskList.getJsonObject(0).getValue("name"));
						taskListAsync.complete();
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

	@Test
	public void testTaskExists(TestContext context)
	{	
		System.out.println("--------------------  Testing Task exists  ----------------------");

		Async setupAsync = context.async();
		WebClientSession currentSession = createSession();
		createUser(vertx, "TestUser", "testPassword", Roles.Researcher)
		.onSuccess(userCreated -> {
			authenticateSession(currentSession, "TestUser", "testPassword")
			.onSuccess(authed -> {
				WebObjectCreator.createTask(currentSession, "Test2")
				.onSuccess(taskData -> {
					Async invalidReacreateAsync = context.async();
					WebObjectCreator.createTask(currentSession, "Test2")
					.onSuccess(res -> {
						context.fail("This should have errored with a does exist exception");
					})
					.onFailure(err -> {
						context.assertEquals(HttpException.class, err.getClass());
						context.assertEquals(409, ((HttpException)err).getStatusCode());
						invalidReacreateAsync.complete();
					});
					Async validReacreateAsync = context.async();
					WebObjectCreator.createOrRetrieveTask(currentSession, "Test2")
					.onSuccess(res -> {
						context.assertEquals(taskData.getValue("UUID"), res.getValue("UUID"));
						context.assertEquals(taskData.getValue("code"), res.getValue("code"));
						context.assertEquals(taskData.getValue("codeType"), res.getValue("codeType"));
						validReacreateAsync.complete();
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

	@Test
	public void testExperimentCreation(TestContext context)
	{	
		System.out.println("--------------------  Testing Experiment generation ----------------------");

		Async setupAsync = context.async();
		createUserAndAuthedSession("TestAdmin", "testPassword", Roles.Researcher)
		.onSuccess(currentSession -> {
			WebObjectCreator.createExperiment(currentSession, "TestExperiment1")
			.onSuccess(experimentObject -> {
				context.assertEquals(2, experimentObject.getJsonArray("elements",new JsonArray()).size());
				JsonArray items = experimentObject.getJsonArray("elements");
				for(int i = 0; i < items.size(); ++i)
				{
					JsonObject currentItem = items.getJsonObject(i).getJsonObject("data");
					String itemName = currentItem.getString("name"); 
					switch(itemName)
					{
					case "Test1" : 
						context.assertEquals("tabcdefg2", currentItem.getString("instanceID"));
						context.assertEquals(1, currentItem.getJsonArray("outputs").size());
						break;
					case "Test2" : 
						context.assertEquals("tabcdefg3", currentItem.getString("instanceID"));
						context.assertEquals("tabcdefg2.smoker = 0", currentItem.getString("filter"));
						break;
					}
				}
				setupAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));

	}


	@Test
	public void testBuildProject(TestContext context)
	{		
		System.out.println("--------------------  Testing Project Generation ----------------------");
		Async projAsync = context.async();
		createUserAndAuthedSession("TestUser", "testPassword", Roles.Researcher)
		.onSuccess(session -> {
			Async plistAsync = context.async();
			Async elistAsync = context.async();
			Async tlistAsync = context.async();
			Async plistAsync2 = context.async();
			Async elistAsync2 = context.async();
			Async tlistAsync2 = context.async();
			WebObjectCreator.createProject(session, "Testproject")
			.onSuccess( projectData -> {
				createUserAndAuthedSession("AnotherUser", "testPassword", Roles.Researcher)
				.onSuccess(otherUserSession -> {
					getElementList(otherUserSession, "experiment")
					.onSuccess(expList -> {					
						// check, that the created Elements actually exist.
						context.assertEquals(1, expList.size()); // one private experiment
						elistAsync2.complete(); 
					})
					.onFailure(err -> context.fail(err));

					getElementList(otherUserSession, "project")
					.onSuccess(list -> {
						context.assertEquals(1,list.size()); // no private data
						plistAsync2.complete();
					})
					.onFailure(err -> context.fail(err));

					getElementList(otherUserSession, "task")
					.onSuccess(list -> {
						context.assertEquals(5,list.size()); // one private task
						tlistAsync2.complete();
					})
					.onFailure(err -> context.fail(err));
					
				})
				.onFailure(err -> context.fail(err));				

				getElementList(session, "experiment")
				.onSuccess(expList -> {					
					// check, that the created Elements actually exist.
					context.assertEquals(2, expList.size()); // one private experiment
					elistAsync.complete(); 
				})
				.onFailure(err -> context.fail(err));

				getElementList(session, "project")
				.onSuccess(list -> {
					context.assertEquals(1,list.size()); // no private data
					plistAsync.complete();
				})
				.onFailure(err -> context.fail(err));

				getElementList(session, "task")
				.onSuccess(list -> {
					context.assertEquals(6,list.size()); // one private task
					tlistAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				projAsync.complete();				
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
}
