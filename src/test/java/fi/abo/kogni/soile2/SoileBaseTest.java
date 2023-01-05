package fi.abo.kogni.soile2;


import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public abstract class SoileBaseTest {

	public Vertx vertx;

	private SoileHashing hashStrat;	
	private String hashingAlgo;
	private SecureRandom random = new SecureRandom();
	public String tmpDir;
	@Before
	public void setUp(TestContext context)
	{
		try
		{
			tmpDir = Files.createTempDirectory("tmpDirPrefix").toFile().getAbsolutePath();
		}
		catch(IOException e)
		{
			context.fail(e);
		}
		// Spin up vertx and load the Soile config. 
		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
		getInputArguments().toString().indexOf("jdwp") >= 0;
		if(isDebug)
		{
			System.out.println("Creating new Vertx instance");
			VertxOptions opts = new VertxOptions().setBlockedThreadCheckInterval(1000*60*60);
			vertx = Vertx.vertx(opts);
		}
		else
		{
			System.out.println("Creating new Vertx instance");
			vertx = Vertx.vertx();
		}
		
		final Async CFGAsync = context.async();
		SoileConfigLoader.setupConfig(vertx)
		.onSuccess(cfg_loaded -> 
		{
			
			hashStrat = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
			hashingAlgo = SoileConfigLoader.getUserProperty("hashingAlgorithm");
			setupTestConfig(context);
			runBeforeTests(context);
			CFGAsync.complete();	
		});

	}

	// start 
	public void runBeforeTests(TestContext context)
	{	
		System.out.println("Starting new test");
	}

	// start 
	public void setupTestConfig(TestContext context)
	{	
		System.out.println("Starting new test");
	}


	/**
	 * This method, called after our test, just cleanup everything by closing the vert.x instance
	 *
	 * @param context the test context
	 */
	@After
	public void stopVertx(TestContext context) {
		System.out.println("Shutting down Vertx");
		Async vertxClosed = context.async();
		vertx.close().onComplete(res ->
		{
			System.out.println("Close completed.");
			vertxClosed.complete();
			context.asyncAssertSuccess();
		});		

	}

	public String createHash(String password)
	{
		final byte[] salt = new byte[32];
		random.nextBytes(salt);

		return hashStrat.hash(hashingAlgo,
				null,
				base64Encode(salt),
				password);

	}

	public MultiMap createFormFromJson(JsonObject json)
	{
		MultiMap result = MultiMap.caseInsensitiveMultiMap();
		for(String key : json.fieldNames())
		{
			result.set(key,json.getString(key));
		}
		return result;
	}

	public void startGitVerticle(TestContext context)
	{
		Async gitVerticleAsync = context.async();
		Path gitPath = Path.of(tmpDir, "gitRepo");
		try {
			Files.createDirectories(gitPath);
		}
		catch(IOException e)
		{
			context.fail(e);
			gitVerticleAsync.complete();
			return;
		}
		vertx.deployVerticle(new gitProviderVerticle(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitPath.toFile().getAbsolutePath()))
		.onSuccess(Void -> {
			gitVerticleAsync.complete();
		})
		.onFailure(fail -> {
			context.fail(fail);
			gitVerticleAsync.complete();
		});
	}
	
	public void failContext(Throwable err, TestContext context)
	{
		System.out.println("Test failed due to: " +  err.getMessage());
		context.fail(err);
	}
	
	public void tearDown(TestContext context)
	{				
		try
		{
			FileUtils.deleteDirectory(new File(tmpDir));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
}



