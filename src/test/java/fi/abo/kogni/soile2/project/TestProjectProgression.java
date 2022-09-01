package fi.abo.kogni.soile2.project;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.project.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.utils.TestParticipant;
import fi.abo.kogni.soile2.project.utils.TestProjectFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class TestProjectProgression{
	
	@Test
	public void testProgression(TestContext context) {
		JsonObject projectData;
		JsonArray participantData;
		JsonArray taskData;
		try
		{			
			projectData = TestProjectFactory.loadProjectData();			
			taskData = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("TaskData.json").getPath()))).getJsonArray("data");						 
		}
		catch(Exception e)
		{			
			fail(e.getMessage());
			return;
		}

		Async p1Async = context.async();
		TestParticipant.getTestParticipant(context,0,projectData).onSuccess(participant1 -> {
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
			context.assertTrue(participant1.getFinishedTasks().contains("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1"));
			project.setNextStep(participant1);
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2", participant1.getProjectPosition());					
			p1Async.complete();
		}).onFailure(fail -> {
			context.fail(fail);
			p1Async.complete();
		});		
		Async p2Async = context.async();
		TestParticipant.getTestParticipant(context,3,projectData).onSuccess(participant2 -> {
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
			proj.setNextStep(participant2);
			// The first task in the second experiment
			context.assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b7",participant2.getProjectPosition());
			p2Async.complete();
		}).onFailure(fail -> {
			context.fail(fail);
			p2Async.complete();
		});
		Async p3Async = context.async();
		TestParticipant.getTestParticipant(context,1,projectData).onSuccess(participant3 -> {
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