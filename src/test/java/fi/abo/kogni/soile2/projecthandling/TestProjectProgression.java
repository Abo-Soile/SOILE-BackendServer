package fi.abo.kogni.soile2.projecthandling;

import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.projecthandling.utils.ParticipantImplForTesting;
import fi.abo.kogni.soile2.projecthandling.utils.ProjectFactoryImplForTesting;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class TestProjectProgression extends ProjectBaseTest{

	@Test
	public void testProgression(TestContext context) {		
		try {
			Async projAsync = context.async();
			ProjectFactoryImplForTesting.loadProject(getPos(0))
			.onSuccess(project -> {
				Async p1Async = context.async();
				ParticipantImplForTesting.getTestParticipant(context,0,getPos(0))
				.onSuccess(participant1 -> {

					Async startAsync = context.async();
					project.startProject(participant1)
					.onSuccess(position -> {
						context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", participant1.getProjectPosition());
						context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", position);
						Async finishAsync = context.async();
						project.finishStep(participant1,taskData.getJsonObject(0))
						.onSuccess(v -> 
						{
							context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2", participant1.getProjectPosition());							
							finishAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						startAsync.complete();
					})
					.onFailure(err -> context.fail(err));					
					p1Async.complete();
				}).onFailure(fail -> {
					context.fail(fail);					
				});		
				Async p2Async = context.async();
				ParticipantImplForTesting.getTestParticipant(context,3,getPos(0))
				.onSuccess(participant2 -> {
					context.assertEquals(Double.valueOf(1.0),participant2.getOutputs().get("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1.smoker"));
					context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4",participant2.getProjectPosition());
					Async finishAsync = context.async();
					project.finishStep(participant2,taskData.getJsonObject(1))
					.onSuccess(v -> 
					{
						// The first task in the second experiment, since we skipped the second task in the first (participant doesn't pass filter)
						context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b7",participant2.getProjectPosition());
						finishAsync.complete();
					})
					.onFailure(err -> context.fail(err));					
					p2Async.complete();
				}).onFailure(fail -> {
					context.fail(fail);
				});
				Async p3Async = context.async();
				ParticipantImplForTesting.getTestParticipant(context,1,getPos(0))
				.onSuccess(participant3 -> {
					context.assertEquals(Double.valueOf(0.0),participant3.getOutputs().get("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1.smoker"));
					context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2",participant3.getProjectPosition());				
					Async finishAsync = context.async();
					project.finishStep(participant3,taskData.getJsonObject(2))
					.onSuccess(v -> {
						// Can be now either at the first or the second task.
						LinkedList<String> resultOptions = new LinkedList<String>();
						resultOptions.add("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4");
						resultOptions.add("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b5");
						context.assertTrue(resultOptions.contains(participant3.getProjectPosition()));
						resultOptions.remove(participant3.getProjectPosition());
						context.assertEquals(1,resultOptions.size());
						//this depends on which step we are at now...							
						if(resultOptions.contains("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b5"))
						{
							Async finishAsync2 = context.async();
							project.finishStep(participant3,taskData.getJsonObject(1))
							.onSuccess(v2 -> {
								//this now has to be the other task of this experiment.
								context.assertTrue(resultOptions.contains(participant3.getProjectPosition()));
								finishAsync2.complete();
							})
							.onFailure(err -> context.fail(err));	
						}
						else
						{								
							Async finishAsync2 = context.async();
							project.finishStep(participant3,taskData.getJsonObject(3))
							.onSuccess(v2 -> {
								//this now has to be the other task of this experiment.
								context.assertTrue(resultOptions.contains(participant3.getProjectPosition()));
								finishAsync2.complete();
							})
							.onFailure(err -> context.fail(err));								
						}

						finishAsync.complete();							
					})
					.onFailure(err -> context.fail(err));
					p3Async.complete();
				})
				.onFailure(err -> context.fail(err));					
				projAsync.complete();

			})
			.onFailure(fail -> {
				context.fail(fail);
			});
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}

}