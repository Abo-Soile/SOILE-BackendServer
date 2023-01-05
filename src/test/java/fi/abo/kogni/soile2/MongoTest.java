package fi.abo.kogni.soile2;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.reverse.TransitionWalker.ReachedState;
import de.flapdoodle.reverse.transitions.Start;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public abstract class MongoTest extends SoileBaseTest {

	
	static Mongod MONGO;
	static ReachedState<RunningMongodProcess> state;
	static int MONGO_PORT = 27022;
	public MongoClient mongo_client;
	
	@BeforeClass
	public static void initialize() throws IOException {
		//MONGO = Mongod.builder();
		Net net = Net.of(Network.getLocalHost().getHostAddress(), MONGO_PORT, Network.localhostIsIPv6());				
		MONGO = Mongod.builder()				
				.net(Start.to(Net.class).initializedWith(net))
				.processOutput(Start.to(ProcessOutput.class).initializedWith(ProcessOutput.silent()).withTransitionLabel("no output"))
				.build();		
		/*MongodStarter starter = MongodStarter.getDefaultInstance();	
		
		ImmutableMongodConfig mongodConfig = ImmutableMongodConfig.builder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(MONGO_PORT, Network.localhostIsIPv6()))				
				.build();
		MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
		MONGO = mongodExecutable.start();*/
		state = MONGO.start(Version.Main.V5_0);		
	}	

	@Override
	public void runBeforeTests(TestContext context)
	{	
		super.runBeforeTests(context);
		mongo_client = MongoClient.createShared(vertx, SoileConfigLoader.getDbCfg());
		System.out.println("initialized mongo Client as : " + mongo_client);
	}
	
	@After
	public void tearDown(TestContext context)
	{		
		super.tearDown(context);
		final Async oasync = context.async();
		mongo_client.getCollections(cols ->{
			if(cols.succeeded())
			{
				HashMap<String, Async> asyncMap = new HashMap<>(); 
				for(String col : cols.result())
				{
					final Async async = context.async();
					asyncMap.put(col, async);					
				}			
				for(String col : cols.result())
				{
					mongo_client.dropCollection(col).onComplete(res ->
				
					{
						asyncMap.get(col).complete();
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
		state.close();
	}

}
