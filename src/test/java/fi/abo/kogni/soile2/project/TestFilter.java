
package fi.abo.kogni.soile2.project;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.project.utils.TestParticipant;
import fi.abo.kogni.soile2.project.utils.TestProjectFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestFilter {

	@Test
	public void testFilterCheck(TestContext context) {
		JsonObject filterData;
		JsonObject projectData;
		try
		{			
			filterData = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("FilterData.json").getPath())));
			projectData = TestProjectFactory.loadProjectData();
		}
		catch(Exception e)
		{			
			context.fail(e.getMessage());
			return ;
		}
		String FilterString = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2.smoker = 1 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";		
		context.assertEquals("Success", Filter.testFilterExpression(FilterString, filterData));
		context.assertNotEquals("Success", Filter.testFilterExpression(FilterString, new JsonObject()));
		String WrongFilterString = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2.smoker = sdasd1 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";
		context.assertNotEquals("Success", Filter.testFilterExpression(WrongFilterString, filterData));		
		Async partAsync = context.async();
		TestParticipant.getTestParticipant(context,3,projectData).onSuccess(p -> {
			context.assertTrue(Filter.userMatchesFilter(FilterString, p));
			String FilterNoSmoker = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b2.smoker = 0 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";		
			context.assertFalse(Filter.userMatchesFilter(FilterNoSmoker, p));
			partAsync.complete();
		}).onFailure(fail ->{
			context.fail(fail);
			partAsync.complete();
		});
	}

	
}

