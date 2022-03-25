package fi.abo.kogni.soile2;

import org.junit.Before;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public abstract class SoileTest {

	public Vertx vertx;
	
	@Before
	public void buildConfig(TestContext context)
	{		
		final Async oasync = context.async();
		vertx = Vertx.vertx();
		ConfigRetriever ret = SoileConfigLoader.getRetriever(vertx);
		ret.getConfig().onComplete(res -> {
			SoileConfigLoader.setConfigs(res.result());
			oasync.complete();
		});
	}

}
