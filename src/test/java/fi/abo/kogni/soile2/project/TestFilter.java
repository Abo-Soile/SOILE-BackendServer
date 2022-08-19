
package fi.abo.kogni.soile2.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.project.participant.Participant;
import fi.abo.kogni.soile2.project.participant.impl.GitParticipant;
import fi.abo.kogni.soile2.project.utils.TestParticipant;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestFilter {

	@Test
	public void testFilterCheck(TestContext context) {
		JsonObject Filter_Data;
		try
		{			
			Filter_Data = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("FilterData.json").getPath())));
		}
		catch(Exception e)
		{			
			fail(e.getMessage());
			return ;
		}
		String FilterString = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2.smoker = 1 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";		
		assertEquals("Success", Filter.testFilterExpression(FilterString, Filter_Data));
		assertNotEquals("Success", Filter.testFilterExpression(FilterString, new JsonObject()));
		String WrongFilterString = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2.smoker = sdasd1 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";
		assertNotEquals("Success", Filter.testFilterExpression(WrongFilterString, Filter_Data));
		
	}

	
	
		
	
	private Participant getTestParticipant(int i)
	{
		JsonArray Participant_data;
		try
		{			
			Participant_data = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("Participant.json").getPath()))).getJsonArray("data");
		}
		catch(Exception e)
		{			
			fail(e.getMessage());
			return null;
		}
		return new TestParticipant(Participant_data.getJsonObject(i));		
	}
	
	
}

