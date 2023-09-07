package fi.abo.kogni.soile2.projecthandling.participant;

import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.projecthandling.ProjectBaseTest;
import fi.abo.kogni.soile2.projecthandling.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
import fi.abo.kogni.soile2.projecthandling.utils.ProjectFactoryImplForTesting;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class DBParticipantTest extends GitTest{

	@Test
	public void testCreationAndExp(TestContext context) {
		System.out.println("--------------------  Testing Participant Creation and Progress ----------------------");
		StudyHandler projHandler = new StudyHandler(mongo_client, vertx);
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		JsonObject smokerOutput = new JsonObject()
				.put("name", "smoker")
				.put("value", 1);


		JsonObject wrongquestionaireOutput = new JsonObject().put("outputData", new JsonArray().add(smokerOutput)).put("taskID", "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2"); 
		JsonObject smokerQuestionaireOutput = new JsonObject().put("persistentData", new JsonArray().add(smokerOutput)).put("outputData", new JsonArray().add(smokerOutput)).put("taskID", "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1");
		Async participantAsync = context.async();
		try
		{
			ProjectFactoryImplForTesting.loadProject(ProjectBaseTest.getPos(0))
			.onSuccess(proj -> {
				partHandler.create(proj)
				.onSuccess(participant -> {
					Async projTestAsync = context.async();
					proj.startStudy(participant)
					.onSuccess(v1 -> {
						context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", participant.getStudyPosition());
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
							context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2", participant.getStudyPosition());
							proj.finishStep(participant, new JsonObject().put("taskID", id))
							.onSuccess(newID -> {								
								context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4", newID);
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
												context.assertTrue(participant.isFinished());
												context.assertTrue(json.getBoolean("finished"));
												context.assertEquals(6,json.getInteger("currentStep"));
												projTestAsync.complete();	
											});												
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

}
