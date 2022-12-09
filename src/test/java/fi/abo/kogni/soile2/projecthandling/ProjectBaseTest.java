package fi.abo.kogni.soile2.projecthandling;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.projecthandling.utils.ProjectFactoryImplForTesting;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ProjectBaseTest {
	protected JsonObject projectData;
	protected JsonArray taskData;
	
	@Before
	public void setUp(TestContext context)
	{
		try
		{			
			projectData = ProjectFactoryImplForTesting.loadProjectData();			
			taskData = new JsonObject(Files.readString(Paths.get(getClass().getClassLoader().getResource("TaskData.json").getPath()))).getJsonArray("data");						 
		}
		catch(Exception e)
		{			
			e.printStackTrace(System.out);
			fail(e.getMessage());
			return;
		}
	}
	
	
	public static JsonObject getPos(int i)
	{
		return new JsonObject().put("pos", i);
	}
}

