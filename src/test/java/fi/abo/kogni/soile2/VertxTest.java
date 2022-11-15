package fi.abo.kogni.soile2;

import static io.vertx.ext.auth.impl.Codec.base64Encode;

import java.io.IOException;
import java.security.SecureRandom;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthenticationOptions;
import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
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
public abstract class VertxTest extends MongoTest{

	public Vertx vertx;
	public MongoClient mongo_client;
	public JsonObject cfg;
	public JsonObject uCfg;
	public JsonObject commCfg;
	public JsonObject serverCfg;
	public JsonObject sessionCfg;

	private SoileHashing hashStrat;	
	private String hashinAlgo;
	private SecureRandom random = new SecureRandom();


	@Before
	public void setUp(TestContext context)
	{	
		
		System.out.println("Starting new test");
		//TODO: remove these options again before deploying. Evebn in tests they should not be in, but for debugging they avoid loads of 
		// error messages.
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
		
		
		super.setUp(context);
		final Async oasync = context.async();				
		cfg = new JsonObject(vertx.fileSystem().readFileBlocking("soile_config.json"));
		hashStrat = new SoileHashing(cfg.getJsonObject(SoileConfigLoader.USERMGR_CFG)
				.getString("serverSalt"));
		hashinAlgo = cfg.getJsonObject(SoileConfigLoader.USERMGR_CFG)
				.getString("hashingAlgorithm");
		uCfg = cfg.getJsonObject(SoileConfigLoader.USERMGR_CFG);
		commCfg = cfg.getJsonObject(SoileConfigLoader.COMMUNICATION_CFG);
		serverCfg = cfg.getJsonObject(SoileConfigLoader.HTTP_SERVER_CFG);
		sessionCfg = cfg.getJsonObject(SoileConfigLoader.SESSION_CFG);
		//authOptions = new SoileAuthenticationOptions(cfg);
				
		mongo_client = MongoClient.createShared(vertx, cfg.getJsonObject("db"));
		//Clean the database of existing entries for tests to be working properly.
		mongo_client.getCollections(cols ->{
			if(cols.succeeded())
			{
				for(String col : cols.result())
				{
					final Async async = context.async();
					mongo_client.dropCollection(col).onComplete(res ->
					{
						async.complete();
					});

				}			
			}
			else
			{
				cols.cause().printStackTrace(System.out);
			}
			oasync.complete();
		});
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

		return hashStrat.hash(hashinAlgo,
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
	
	public SoileUserManager createManager()
	{
		return new SoileUserManager(MongoClient.create(vertx, cfg.getJsonObject("db")));
	}
	
	
}
