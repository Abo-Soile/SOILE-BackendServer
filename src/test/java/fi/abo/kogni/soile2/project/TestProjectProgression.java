package fi.abo.kogni.soile2.project;

import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.project.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.utils.ParticipantImplForTesting;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class TestProjectProgression extends ProjectBaseTest{
	
	@Test
	public void testProgression(TestContext context) {		
		Async p1Async = context.async();
		ParticipantImplForTesting.getTestParticipant(context,0,projectData).onSuccess(participant1 -> {
			ProjectInstance project = participant1.getProject();
			
			project.startProject(participant1);
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", participant1.getProjectPosition());
			try
			{
				project.finishStep(participant1,taskData.getJsonObject(0));
			}
			catch(InvalidPositionException e)
			{
				context.fail(e);
				p1Async.complete();
			}
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", participant1.getProjectPosition());
			project.setNextStep(participant1);
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2", participant1.getProjectPosition());					
			p1Async.complete();
		}).onFailure(fail -> {
			context.fail(fail);
			p1Async.complete();
		});		
		Async p2Async = context.async();
		ParticipantImplForTesting.getTestParticipant(context,3,projectData).onSuccess(participant2 -> {
			context.assertEquals(Double.valueOf(1.0),participant2.getOutputs().get("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1.smoker"));
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4",participant2.getProjectPosition());
			ProjectInstance proj = participant2.getProject();
			try
			{
				proj.finishStep(participant2,taskData.getJsonObject(1));
			}
			catch(InvalidPositionException e)
			{
				context.fail(e);
				p2Async.complete();
			}
			System.out.println("Current Participant: " + participant2);
			System.out.println("Current Position: " + participant2.getProjectPosition());
			System.out.println("Current Element: " + proj.getElement(participant2.getProjectPosition()));
			proj.setNextStep(participant2);
			// The first task in the second experiment, since we skipped the second task in the first (participant doesn't pass filter)
			System.out.println("New Position: " + participant2.getProjectPosition());
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b7",participant2.getProjectPosition());
			p2Async.complete();
		}).onFailure(fail -> {
			context.fail(fail);
			p2Async.complete();
		});
		Async p3Async = context.async();
		ParticipantImplForTesting.getTestParticipant(context,1,projectData).onSuccess(participant3 -> {
			context.assertEquals(Double.valueOf(0.0),participant3.getOutputs().get("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1.smoker"));
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2",participant3.getProjectPosition());
			ProjectInstance proj = participant3.getProject();
			try
			{
				proj.finishStep(participant3,taskData.getJsonObject(2));
			}
			catch(InvalidPositionException e)
			{
				context.fail(e);
				p3Async.complete();
			}
			proj.setNextStep(participant3);
			// Can be now either at the first or the second task.
			LinkedList<String> resultOptions = new LinkedList<String>();
			resultOptions.add("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4");
			resultOptions.add("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b5");
			context.assertTrue(resultOptions.contains(participant3.getProjectPosition()));
			resultOptions.remove(participant3.getProjectPosition());
			try
			{
				//this depends on which step we are at now... 
				if(resultOptions.contains("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b5"))
				{
					proj.finishStep(participant3,taskData.getJsonObject(1));
				}
				else
				{
					proj.finishStep(participant3,taskData.getJsonObject(3));	
				}
			}
			catch(InvalidPositionException e)
			{
				context.fail(e);
				p3Async.complete();
			}
			proj.setNextStep(participant3);
			// this now has to be the other task of this experiment.
			context.assertTrue(resultOptions.contains(participant3.getProjectPosition()));
			p3Async.complete();
		}).onFailure(fail -> {
			context.fail(fail);
			p3Async.complete();
		});
	}
		
}