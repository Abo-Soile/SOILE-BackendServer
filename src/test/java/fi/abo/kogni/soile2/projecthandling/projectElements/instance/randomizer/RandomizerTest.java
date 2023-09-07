package fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizer;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClientSession;

//TODO: Test Project deletion and Project Stop.
public class RandomizerTest extends SoileWebTest {

	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testBlockRandom(TestContext context)
	{
		System.out.println("--------------------  Running Block Randomization Test  ----------------------");    
		Async testAsync = context.async();
		createAndStartStudy(true, "newOne", "BlockExample")
		.onSuccess(instanceID -> {
			WebClientSession p1 = createSession();
			WebClientSession p2 = createSession();
			WebClientSession p3 = createSession();
			WebClientSession p4 = createSession();
			WebClientSession p5 = createSession();
			WebClientSession p6 = createSession();
			mongo_client.findOne(SoileConfigLoader.getDbCfg().getString("studyCollection"), new JsonObject(), null)
			.compose(res -> {
				System.out.println(res.encodePrettily());
				context.assertEquals(0, res.getJsonObject("randomizerPasses").getInteger("r1"));
				System.out.println("Testing Participant 1");
				return signUpAndTest(p1, 1, "tabcdefg0", context, instanceID);
			})		
			.compose(checked -> {
				System.out.println("Testing Participant 2");
				return signUpAndTest(p2, 2, "tabcdefg3", context, instanceID);
			})
			.compose(checked -> {
				System.out.println("Testing Participant 3");
				return signUpAndTest(p3, 3, "tabcdefg3", context, instanceID);
			})
			.compose(checked -> {
				System.out.println("Testing Participant 4");
				return signUpAndTest(p4, 4, "tabcdefg0", context, instanceID);
			})
			.compose(checked -> {
				System.out.println("Testing Participant 5");
				return signUpAndTest(p5, 5, "tabcdefg0", context, instanceID);
			})
			.compose(checked -> {
				System.out.println("Testing Participant 6");
				return signUpAndTest(p6, 6, "tabcdefg3", context, instanceID);
			})
			.onSuccess(authToken -> {				
				// lets just see if it gets created... 				
				testAsync.complete();
				
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	private Future<Void> signUpAndTest(WebClientSession session, int expectedSignUps, String expectedTask, TestContext context, String instanceID)
	{
		return createTokenAndSignupUser(generatorSession, instanceID)
				.compose(authToken -> {
					session.addHeader("Authorization", authToken);
					return mongo_client.findOne(SoileConfigLoader.getDbCfg().getString("studyCollection"), new JsonObject(), null); 
				}).compose(res -> {
					context.assertEquals(expectedSignUps, res.getJsonObject("randomizerPasses").getInteger("r1"));
					return POST(session, "/study/" + instanceID + "/getcurrenttaskinfo", null, null);
				}).map(response -> {
					JsonObject taskData = response.bodyAsJsonObject();
					context.assertEquals(expectedTask, taskData.getValue("id"));
					return null;
				});
	}
	
	
	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testBasicRandom(TestContext context)
	{
		System.out.println("--------------------  Running Block Randomization Test  ----------------------");    
		Async testAsync = context.async();
		JsonArray OutputData = new JsonArray().add(new JsonObject().put("name", "smoker")
				.put("value", 1)
				.put("timestamp", System.currentTimeMillis()));
		JsonObject resultData = new JsonObject().put("jsonData",new JsonArray().add(new JsonObject().put("name", "smoker")
																									.put("value", 1)
																									.put("timestamp", System.currentTimeMillis())))							
				.put("fileData", new JsonArray());
		JsonObject result = new JsonObject().put("outputData", OutputData).put("resultData", resultData);
		
		createAndStartStudy(true, "newOne", "RandomExampleOnce")
		.onSuccess(instanceID -> {
			WebClientSession p1 = createSession();
			WebClientSession p2 = createSession();
			WebClientSession p3 = createSession();
			WebClientSession p4 = createSession();			
			JsonArray possibleTasks = new JsonArray().add("tabcdefg0").add("tabcdefg1"); 
			mongo_client.findOne(SoileConfigLoader.getDbCfg().getString("studyCollection"), new JsonObject(), null)
			.compose(res -> {
				System.out.println(res.encodePrettily());
				context.assertEquals(0, res.getJsonObject("randomizerPasses").getInteger("r1"));
				System.out.println("Testing Participant 1");
				return signUpAndTestRandom(p1, 1, possibleTasks, context, instanceID, result);
			})		
			.compose(checked -> {
				System.out.println("Testing Participant 2");
				return signUpAndTestRandom(p2, 2, possibleTasks, context, instanceID, result);
			})
			.compose(checked -> {
				System.out.println("Testing Participant 3");
				return signUpAndTestRandom(p3, 3, possibleTasks, context, instanceID, result);
			})
			.compose(checked -> {
				System.out.println("Testing Participant 4");
				return signUpAndTestRandom(p4, 4, possibleTasks, context, instanceID, result);
			})			
			.onSuccess(authToken -> {				
				// lets just see if it gets created... 				
				testAsync.complete();
				
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	
	private Future<Void> signUpAndTestRandom(WebClientSession session, int expectedSignUps, JsonArray expectedTasks, TestContext context, String instanceID, JsonObject resultData)
	{
		JsonObject assigned = new JsonObject();
		return createTokenAndSignupUser(generatorSession, instanceID)
				.compose(authToken -> {
					session.addHeader("Authorization", authToken);
					return mongo_client.findOne(SoileConfigLoader.getDbCfg().getString("studyCollection"), new JsonObject(), null); 
				}).compose(res -> {
					context.assertEquals(expectedSignUps, res.getJsonObject("randomizerPasses").getInteger("r1"));
					return POST(session, "/study/" + instanceID + "/getcurrenttaskinfo", null, null);
				}).compose(response -> {
					JsonObject taskData = response.bodyAsJsonObject();
					context.assertTrue(expectedTasks.contains(taskData.getValue("id")));
					assigned.put("value", taskData.getValue("id"));
					return submitResult(session, resultData, instanceID);
				}).compose(submitted -> {
					return POST(session, "/study/" + instanceID + "/getcurrenttaskinfo", null, null);
				})
				.map(response -> {
					JsonObject taskData = response.bodyAsJsonObject();
					context.assertEquals(assigned.getValue("value"), (taskData.getValue("id")));					
					return null;
				});
	}
}
