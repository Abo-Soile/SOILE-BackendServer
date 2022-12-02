
package fi.abo.kogni.soile2.projecthandling;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.Filter;
import fi.abo.kogni.soile2.projecthandling.utils.ParticipantImplForTesting;
import fi.abo.kogni.soile2.projecthandling.utils.ProjectFactoryImplForTesting;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestFilter extends ProjectBaseTest{

	@Test
	public void testFilterCheck(TestContext context) {
		JsonObject filterData;
		JsonObject projectData;
		try
		{			
			filterData = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("FilterData.json").getPath())));
			projectData = ProjectFactoryImplForTesting.loadProjectData();
		}
		catch(Exception e)
		{			
			context.fail(e.getMessage());
			return ;
		}
		String FilterString = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1.smoker = 1 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";		
		context.assertEquals("Success", Filter.testFilterExpression(FilterString, filterData));
		context.assertNotEquals("Success", Filter.testFilterExpression(FilterString, new JsonObject()));
		String WrongFilterString = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1.smoker = sdasd1 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";
		context.assertNotEquals("Success", Filter.testFilterExpression(WrongFilterString, filterData));		
		Async partAsync = context.async();
		ParticipantImplForTesting.getTestParticipant(context,3,getPos(0)).onSuccess(p -> {
			context.assertTrue(Filter.userMatchesFilter(FilterString, p));
			String FilterNoSmoker = "t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b1.smoker = 0 & t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b3.output1 > 0 | t83297d7785fd249bdb6543a850680e812ce11873df2d48467cb9612dbd0482b4.output4 < 10";		
			context.assertFalse(Filter.userMatchesFilter(FilterNoSmoker, p));
			partAsync.complete();
		}).onFailure(fail ->{
			context.fail(fail);
			partAsync.complete();
		});
	}

	
}

