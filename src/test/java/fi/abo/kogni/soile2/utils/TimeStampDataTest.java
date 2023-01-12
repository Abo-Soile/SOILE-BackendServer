package fi.abo.kogni.soile2.utils;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TimeStampDataTest {
	
	@Test
	public void TestTTL(TestContext context) {
		TimeStampedMap<String, Integer> testMap = new TimeStampedMap<>(new DataRetriever<String, Integer>() {
			private int itemID = 0;
			@Override
			public Future<Integer> getElement(String key) {
				// TODO Auto-generated method stub
				itemID += 1;
				return Future.<Integer>succeededFuture(itemID);
			}

			@Override
			public void getElement(String key, Handler<AsyncResult<Integer>> handler) {
				// TODO Auto-generated method stub
				
			}
		}, 50);
		testMap.getData("current")
		.onSuccess(element1 -> {
			testMap.cleanup();
			testMap.getData("current")
			.onSuccess(element2 -> {
				try {
				TimeUnit.MILLISECONDS.sleep(51);
				testMap.cleanup();
				testMap.getData("current")
				.onSuccess(element3 -> {
					context.assertEquals(1,element1);
					context.assertEquals(1,element2);
					context.assertEquals(2,element3);
					context.assertTrue(element1 == element2);
				})
				.onFailure(err -> context.fail(err));
				}
				catch(InterruptedException e)
				{
					context.fail(e);
				}
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
		
	}
	
	
	
}
