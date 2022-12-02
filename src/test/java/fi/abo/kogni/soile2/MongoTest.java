package fi.abo.kogni.soile2;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class MongoTest extends SoileBaseTest {

	
	static MongodProcess MONGO;
	static int MONGO_PORT = 27022;
	public MongoClient mongo_client;
	
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

	@Override
	public void runBeforeTests(TestContext context)
	{	
		super.runBeforeTests(context);
		mongo_client = MongoClient.createShared(vertx, SoileConfigLoader.getDbCfg());
		System.out.println("initialized mongo Client as : " + mongo_client);
	}
	
	@After
	public void cleanDB(TestContext context)
	{
		final Async oasync = context.async();

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
	
	@AfterClass
	public static void shutdown() {
		MONGO.stop();
	}

}
