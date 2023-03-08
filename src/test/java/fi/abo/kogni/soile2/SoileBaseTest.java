package fi.abo.kogni.soile2;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.SecureRandom;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public abstract class SoileBaseTest {

	public Vertx vertx;
	protected EventBus eb;
	protected SoileHashing hashStrat;	
	protected String hashingAlgo;
	protected SecureRandom random = new SecureRandom();
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
			VertxOptions opts = new VertxOptions().setBlockedThreadCheckInterval(1000*60*60);
			vertx = Vertx.vertx(opts);
		}
		else
		{
			vertx = Vertx.vertx();
		}
		eb = vertx.eventBus();
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
	public abstract void runBeforeTests(TestContext context);

	// start 
	public void setupTestConfig(TestContext context)
	{
		
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
	
	/**
	 * This method, called after our test, just cleanup everything by closing the vert.x instance
	 *
	 * @param context the test context
	 */
	@After
	public void finishUp(TestContext context) {
		System.out.println("Shutting down Vertx");
		Async vertxClosed = context.async();
		vertx.close().onComplete(res ->
		{
			System.out.println("Close completed.");
			vertxClosed.complete();
		});		
	}

	@After 
	public void clearTmpDir(TestContext context)
	{				
		try
		{
			System.out.println("Cleaning directory");
			FileUtils.deleteDirectory(new File(tmpDir));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
	
	protected boolean areFilesEqual(File f1, File f2) throws IOException
	{		
		InputStream inputStream1 = new FileInputStream(f1);
		InputStream inputStream2 = new FileInputStream(f2);
		return IOUtils.contentEquals(inputStream1, inputStream2);				
	}
}



