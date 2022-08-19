package fi.abo.kogni.soile2.utils;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.VertxTest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TimeStampDataTest extends VertxTest{

	private String coll = "testCollection";
	
	@Before
	public void initDB(TestContext context)
	{
		final Async oasync = context.async();
		mongo_client.save(coll, new JsonObject().put("id", 12)).onSuccess(res 
				-> {
					oasync.complete();
				}).onFailure(res -> 
				{
					fail(res.getCause().getMessage());
				});
	
	}
	
	
	@Test
	public void TestTTL() {
		
	}
	
	
	
}
