package fi.abo.kogni.soile2.project;

import org.junit.Test;

import fi.abo.kogni.soile2.VertxTest;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.utils.TestProjectFactory;
import io.vertx.ext.unit.TestContext;

public class ProjectTest extends VertxTest {

	@Test
	public void testProjectCreation(TestContext context)
	{
		TestProjectFactory fac = new TestProjectFactory();
		ProjectInstance project = fac.createInstance();
	}
}
