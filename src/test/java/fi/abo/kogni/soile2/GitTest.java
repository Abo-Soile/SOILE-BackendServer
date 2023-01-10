package fi.abo.kogni.soile2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.junit.After;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import fi.abo.kogni.soile2.utils.VerticleInitialiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public abstract class GitTest extends MongoTest {

	protected String gitDir;
	protected GitManager gitManager;
	protected String gitDataLakeDir;
	protected String resultDataLakeDir;
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
	}
	
	@Override
	public void setupTestConfig(TestContext context)
	{
		try {
			gitDataLakeDir = Files.createTempDirectory("gitdataLakeDir").toFile().getAbsolutePath();
			resultDataLakeDir = Files.createTempDirectory("resultdataLakeDir").toFile().getAbsolutePath();
			// we need to Update the config for some verticles, which derive their pathes from the config.
			SoileConfigLoader.getConfig(SoileConfigLoader.HTTP_SERVER_CFG)
			.put("soileGitFolder", gitDir)
			.put("soileGitDataLakeFolder", gitDataLakeDir)
			.put("soileResultDirectory", resultDataLakeDir);
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}

	@After
	public void clearGitFolders(TestContext context)
	{
		try
		{
			FileUtils.deleteDirectory(new File(gitDataLakeDir));
			FileUtils.deleteDirectory(new File(resultDataLakeDir));
			FileUtils.deleteDirectory(new File(gitDir));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
}
