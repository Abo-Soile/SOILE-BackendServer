package fi.abo.kogni.soile2.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.project.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.impl.DBParticipant;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class TestProjectProgression extends MongoTest{
	
	@Test
	public void testProgression(TestContext context) {
		JsonObject projectData;
		JsonArray participantData;
		JsonArray taskData;
		try
		{			
			projectData = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("ProjectDefinition.json").getPath())));
			participantData = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("Participant.json").getPath()))).getJsonArray("data");
			taskData = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("TaskData.json").getPath()))).getJsonArray("data");						 
		}
		catch(Exception e)
		{			
			fail(e.getMessage());
			return;
		}
		//TODO: Fix Managers!!
		ProjectInstance p = new ProjectInstance(projectData, null);		
		DBParticipant p1 = new DBParticipant(participantData.getJsonObject(0),null);
		p.startProject(p1);
		assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", p1.getProjectPosition());
		try
		{
			p.finishStep(p1,taskData.getJsonObject(0));
		}
		catch(InvalidPositionException e)
		{
			context.fail(e);
		}
		assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1", p1.getProjectPosition());
		assertTrue(p1.getFinishedTasks().contains("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1"));
		p.setNextStep(p1);
		assertEquals("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2", p1.getProjectPosition());		
		
		DBParticipant p2 = new DBParticipant(participantData.getJsonObject(1),null);
		assertEquals(Double.valueOf(1.0),p2.getOutputs().get("t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2.smoker"));
		assertEquals(p2.getProjectPosition(), "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4");
		try
		{
			p.finishStep(p2,taskData.getJsonObject(0));
		}
		catch(InvalidPositionException e)
		{
			context.fail(e);
		}
		p.setNextStep(p2);
		assertEquals(p2.getProjectPosition(), "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b6");
	}
		
}