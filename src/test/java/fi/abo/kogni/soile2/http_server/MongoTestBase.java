package fi.abo.kogni.soile2.http_server;

import java.io.IOException;

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
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public abstract class MongoTestBase {

	public Vertx vertx;
	public MongoClient mongo_client;
	public JsonObject config;
	public JsonObject uconfig;	
	static MongodProcess MONGO;
	static int MONGO_PORT = 27022;

	
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
		final Async oasync = context.async();
		vertx = Vertx.vertx();
		config = new JsonObject(vertx.fileSystem().readFileBlocking("soile_config.json"));
		uconfig = config.getJsonObject(SoileConfigLoader.USERMANAGEMENTFIELDS);
		mongo_client = MongoClient.createShared(vertx, config.getJsonObject("db"));
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
		vertx.close(context.asyncAssertSuccess());
		
	}
	

}
