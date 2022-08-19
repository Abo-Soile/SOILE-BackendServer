package fi.abo.kogni.soile2;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


/**
 * This class pre-loads the config in order to allow tests to use the 
 * config before the vertx instance is actually et up. 
 * @author Thomas Pfau
 *
 */
@RunWith(VertxUnitRunner.class)
public abstract class SoileTest {

	@BeforeClass
	public static void init(TestContext context)
	{
				
		Vertx vertx = Vertx.vertx();
		ConfigRetriever retriever = SoileConfigLoader.getRetriever(vertx);
		final Async CFGAsync = context.async();
		retriever.getConfig().onComplete(res -> 
		{
			if(res.succeeded())
			{
				SoileTest.storeConfig(res.result());
			}
			else {
				System.out.println("Error loading Config");
				System.out.println(res.cause());
			}
			CFGAsync.complete();	
		});
	}
	
	static Future<Void> storeConfig(JsonObject configLoadResult)
	{	
		return SoileConfigLoader.setConfigs(configLoadResult);		
	}	
	
}
