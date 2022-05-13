package fi.abo.kogni.soile2.http_server;

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
import fi.abo.kogni.soile2.SoileTest;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthenticationOptions;
import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public abstract class MongoTestBase extends SoileTest{

	public Vertx vertx;
	public MongoClient mongo_client;
	public JsonObject cfg;
	public JsonObject uCfg;
	public JsonObject commCfg;
	public JsonObject serverCfg;
	public JsonObject sessionCfg;

	static MongodProcess MONGO;
	static int MONGO_PORT = 27022;
	private SoileHashing hashStrat;	
	private String hashinAlgo;
	private SecureRandom random = new SecureRandom();
	private SoileUserManager uManager;
	public SoileAuthenticationOptions authOptions;		

	@BeforeClass
	public static void initialize() throws IOException {
		MongodStarter starter = MongodStarter.getDefaultInstance();		
		ImmutableMongodConfig mongodConfig = ImmutableMongodConfig.builder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
				.build();
		MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
		MONGO = mongodExecutable.start();		
	}

	@AfterClass
	public static void shutdown() {
		MONGO.stop();
	}

	@Before
	public void setUp(TestContext context)
	{			
		vertx = Vertx.vertx();
		super.init(context);
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
		uManager = createManager();
	}
	/**
	 * This method, called after our test, just cleanup everything by closing the vert.x instance
	 *
	 * @param context the test context
	 */
	@After
	public void tearDown(TestContext context) {
		System.out.println("Shutting down Vertx");
		vertx.close(context.asyncAssertSuccess());

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

	public Future<String> createUser(JsonObject userdata, TestContext context)
	{			
		String username = userdata.getString(cfg.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("usernameField"));
		String password = userdata.getString(cfg.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("passwordField"));
		System.out.println("Creating user with name " + username + " and password " + password);
		return uManager.createUser(username, password);
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
