package fi.abo.kogni.soile2;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoTest extends SoileTest {

	
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

}
