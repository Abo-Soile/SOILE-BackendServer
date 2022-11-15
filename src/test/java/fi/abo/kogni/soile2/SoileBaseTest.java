package fi.abo.kogni.soile2;


import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.security.SecureRandom;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.authentication.SoileAuthenticationOptions;
import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class SoileBaseTest {

	public Vertx vertx;

	private SoileHashing hashStrat;	
	private String hashingAlgo;
	private SecureRandom random = new SecureRandom();

	@Before
	public void setUp(TestContext context)
	{
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
		
		ConfigRetriever retriever = SoileConfigLoader.getRetriever(vertx);
		final Async CFGAsync = context.async();
		retriever.getConfig().onComplete(res -> 
		{
			if(res.succeeded())
			{
				SoileBaseTest.storeConfig(res.result());
			}
			else {
				System.out.println("Error loading Config");
				System.out.println(res.cause());
			}
			hashStrat = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
			hashingAlgo = SoileConfigLoader.getUserProperty("hashingAlgorithm");
			
			runBeforeTests(context);

			CFGAsync.complete();	
		});

	}

	// start 
	public void runBeforeTests(TestContext context)
	{	
		System.out.println("Starting new test");
	}


	/**
	 * This method, called after our test, just cleanup everything by closing the vert.x instance
	 *
	 * @param context the test context
	 */
	@After
	public void tearDown(TestContext context) {
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

	static Future<Void> storeConfig(JsonObject configLoadResult)
	{	
		return SoileConfigLoader.setConfigs(configLoadResult);		
	}	
}



