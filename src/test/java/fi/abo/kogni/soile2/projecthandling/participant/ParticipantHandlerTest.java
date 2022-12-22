package fi.abo.kogni.soile2.projecthandling.participant;

import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.projecthandling.ProjectBaseTest;
import fi.abo.kogni.soile2.projecthandling.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import fi.abo.kogni.soile2.projecthandling.utils.ProjectFactoryImplForTesting;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ParticipantHandlerTest extends GitTest{

	@Test
	public void testGetParticipantStatus(TestContext context) {
		ProjectInstanceHandler projHandler = new ProjectInstanceHandler(gitDataLakeDir, mongo_client, vertx.eventBus());
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		Async testAsync = context.async();
		ObjectGenerator.buildAPIProject(ElementManager.getProjectManager(mongo_client, gitManager), ElementManager.getExperimentManager(mongo_client, gitManager), ElementManager.getTaskManager(mongo_client, gitManager), mongo_client, "Testproject")
		.onSuccess(apiProject-> {
			projHandler.createProjectInstance(apiProject.getJson())
			.onSuccess(projectInstance -> {			
				LinkedList<Future> participantFutures = new LinkedList<Future>();
				participantFutures.add(partHandler.createParticipant(projectInstance.getID()));
				participantFutures.add(partHandler.createParticipant(projectInstance.getID()));
				participantFutures.add(partHandler.createParticipant(projectInstance.getID()));
				CompositeFuture.all(participantFutures).mapEmpty()
				.onSuccess(res -> {
					partHandler.getParticipantStatusForProject(projectInstance)
					.onSuccess(participantInfo -> 
					{
						context.assertEquals(3, participantInfo.size());
						for(int i = 0; i < participantInfo.size(); ++i)
						{
							context.assertFalse(participantInfo.getJsonObject(i).getBoolean("finished"));
						}
						testAsync.complete();
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));	
			})
			.onFailure(err -> context.fail(err));

		})
		.onFailure(err -> context.fail(err));

	}


	@Test
	public void testParticipantStatus(TestContext context) {
		ProjectInstanceHandler projHandler = new ProjectInstanceHandler(gitDataLakeDir, mongo_client, vertx.eventBus());
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		JsonObject smokerOutput = new JsonObject()
				.put("name", "smoker")
				.put("value", 1);

		JsonObject nonSmokerOutput = new JsonObject()
				.put("name", "smoker")
				.put("value", 0);

		JsonObject wrongquestionaireOutput = new JsonObject().put("outputdata", new JsonArray().add(smokerOutput)).put("taskID", "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2"); 
		JsonObject nonSmokerQuestionaireOutput = new JsonObject().put("outputdata", new JsonArray().add(nonSmokerOutput)).put("taskID", "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1");
		JsonObject smokerQuestionaireOutput = new JsonObject().put("outputdata", new JsonArray().add(smokerOutput)).put("taskID", "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1");
		Async participantAsync = context.async();
		try
		{
			ProjectFactoryImplForTesting.loadProject(ProjectBaseTest.getPos(0))
			.onSuccess(proj -> {
				partHandler.create(proj)
				.onSuccess(participant -> {
					Async projTestAsync = context.async();
					proj.startProject(participant)
					.onSuccess(v1 -> {
						context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", participant.getProjectPosition());
						Async invalidAsync = context.async();
						proj.finishStep(participant, wrongquestionaireOutput).
						onSuccess(r -> {
							context.fail("This should be unreachable as the previouscommand should throw an exception");	
						})
						.onFailure(err -> {
							context.assertEquals(InvalidPositionException.class, err.getClass());
							invalidAsync.complete();
						});

						proj.finishStep(participant, smokerQuestionaireOutput)
						.onSuccess(id -> {
							context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2", participant.getProjectPosition());
							proj.finishStep(participant, new JsonObject().put("taskID", id))
							.onSuccess(newID -> {								
								context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4", newID);
								partHandler.getParticipantStatusForProject(proj)
								.onSuccess(resultArray -> {
									context.assertEquals(1, resultArray.size());
									context.assertFalse(resultArray.getJsonObject(0).getBoolean("finished"));
									context.assertEquals(participant.getID(), resultArray.getJsonObject(0).getString("participantID"));
									proj.finishStep(participant, new JsonObject().put("taskID", newID))
									.onSuccess(newID2 -> {
										// the next step from the first experiment is skipped because it filters for smoker = 0, so we are in the next experiment.
										LinkedList<String> resultOptions = new LinkedList<String>();
										resultOptions.add("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b7");
										resultOptions.add("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b8");
										context.assertTrue(resultOptions.contains(newID2));
										proj.finishStep(participant, new JsonObject().put("taskID", newID2))
										.onSuccess(newID3 -> {										
											context.assertTrue(resultOptions.contains(newID2));
											context.assertNotEquals(newID2, newID3);
											proj.finishStep(participant, new JsonObject().put("taskID", newID3))
											.onSuccess(newID4 -> {
												context.assertTrue(newID4 == null);	
												mongo_client.findOne(SoileConfigLoader.getCollectionName("participantCollection"),new JsonObject().put("_id", participant.getID()),null)
												.onSuccess( json -> {
													System.out.println(json.encodePrettily());
													context.assertTrue(participant.isFinished());
													context.assertTrue(json.getBoolean("finished"));
													context.assertEquals(6,json.getInteger("currentStep"));
													partHandler.getParticipantStatusForProject(proj)
													.onSuccess(resultArray2 -> {
														context.assertEquals(1, resultArray2.size());
														context.assertTrue(resultArray2.getJsonObject(0).getBoolean("finished"));
														context.assertEquals(participant.getID(), resultArray2.getJsonObject(0).getString("participantID"));
														projTestAsync.complete();	

													})																								
													.onFailure(err -> context.fail(err));		
												})
												.onFailure(err -> context.fail(err));
											})
											.onFailure(err -> context.fail(err));											
										})
										.onFailure(err -> context.fail(err));

									})
									.onFailure(err -> context.fail(err));
								})
								.onFailure(err -> context.fail(err));
							})
							.onFailure(err -> context.fail(err));
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
					participantAsync.complete();
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
	public void testCreateAndGet(TestContext context) {
		ProjectInstanceHandler projHandler = new ProjectInstanceHandler(gitDataLakeDir, mongo_client, vertx.eventBus());
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		Async testAsync = context.async();
		ElementManager<Project> projectManager = ElementManager.getProjectManager(mongo_client, gitManager);
		ObjectGenerator.buildAPIProject(projectManager, ElementManager.getExperimentManager(mongo_client, gitManager), ElementManager.getTaskManager(mongo_client, gitManager), mongo_client, "Testproject2")
		.onSuccess(apiProject-> {
			System.out.println("Api Project is: " + apiProject.getJson().encodePrettily());
			projHandler.createProjectInstance(apiProject.getJson())
			.onSuccess(projectInstance -> {				
				System.out.println("The Project Instance is : " + projectInstance.toString());
				System.out.println("The Api Project UUID is : " + apiProject.getUUID());
				System.out.println("The Api Project is : " + apiProject.getJson().encodePrettily());
				projectManager.getGitJson(apiProject.getUUID(), apiProject.getVersion())
				.onSuccess(gitJson -> {
					System.out.println("The Json associated with this project is:\n " + gitJson.encodePrettily());					
					partHandler.createParticipant(projectInstance.getID())				
					.onSuccess( participant -> 
					{				
						System.out.println("Starting Project");
						projectInstance.startProject(participant)
						.onSuccess(position -> {
							partHandler.getParticpant(participant.getID())
							.onSuccess(participant2 -> 
							{
								context.assertEquals(participant, participant2);
								context.assertFalse(participant == participant2);
								partHandler.getParticpant(participant.getID())
								.onSuccess(participant3 -> 
								{
									context.assertEquals(participant, participant3);
									// those should be the very same object as retrieved by the participantHandler.
									context.assertTrue(participant2 == participant3);
									context.assertFalse(participant == participant3);
									//
									System.out.println("Finishing current step");
									projectInstance.finishStep(participant, new JsonObject().put("taskID", position))
									.onSuccess(res -> {
										partHandler.getParticpant(participant.getID())
										.onSuccess(participant4 ->
										{
											//this should be a new object, as the participant should have been made "dirty"
											context.assertEquals(participant, participant4);
											// those should be the very same object as retrieved by the participantHandler.									
											context.assertFalse(participant == participant4);
											context.assertFalse(participant2 == participant4);
											testAsync.complete();			
										})
										.onFailure(err -> context.fail(err));
									})
									.onFailure(err -> context.fail(err));		
								})
								.onFailure(err -> context.fail(err));														
							})
							.onFailure(err -> context.fail(err));					
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));	
			})
			.onFailure(err -> context.fail(err));

		})
		.onFailure(err -> context.fail(err));

	}
}
