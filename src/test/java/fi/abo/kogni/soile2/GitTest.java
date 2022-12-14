package fi.abo.kogni.soile2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.utils.VerticleInitialiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public abstract class GitTest extends MongoTest {

	protected String gitDir;
	protected GitManager gitManager;
	protected String dataLakeDir;
	@Override
	public void runBeforeTests(TestContext context)
	{		
		super.runBeforeTests(context);
		Async gitInit = context.async();
		VerticleInitialiser.startGitVerticle(vertx, Level.ERROR).onSuccess( dir -> {
			gitDir = dir;
			gitInit.complete();
		})
		.onFailure(err -> {
			context.fail(err);
			gitInit.complete();
		});
		gitManager = new GitManager(vertx.eventBus());
		try {
			dataLakeDir = Files.createTempDirectory("dataLakeDir").toFile().getAbsolutePath();
		}
		catch(IOException e)
		{
			context.fail(e);
		}
		
		
	}
	
	public void tearDown(TestContext context)
	{
		super.tearDown(context);
		try
		{
			FileUtils.deleteDirectory(new File(dataLakeDir));
			//FileUtils.deleteDirectory(new File(gitDir));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
}
