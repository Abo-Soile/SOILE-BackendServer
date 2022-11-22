package fi.abo.kogni.soile2.utils;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class MongoDBTests extends MongoTest{


	@Test
	public void testDataUpdate(TestContext context)
	{
		Async async = context.async();
		JsonObject Element1 = new JsonObject().put("class", "c1").put("value", "v1");
		JsonObject Element2 = new JsonObject().put("class", "c2").put("value", "v2");
		JsonObject Element3 = new JsonObject().put("class", "c3").put("value", "v3");
		JsonObject Element4 = new JsonObject().put("class", "c3").put("value", "v4");
		JsonObject QueryObject = new JsonObject().put("name", "test"); 
		JsonObject basicData = new JsonObject().put("name", "test").put("data", new JsonArray().add(Element1).add(Element2));
		mongo_client.save("TestCollection", basicData).onSuccess(id -> {
			mongo_client.updateCollection("TestCollection", QueryObject, new JsonObject().put("$addToSet", new JsonObject().put("data", new JsonObject().put("$each", new JsonArray().add(Element3)))))
			.onSuccess(res -> {
				mongo_client.updateCollection("TestCollection", QueryObject, new JsonObject().put("$addToSet", new JsonObject().put("data",new JsonObject().put("$each", new JsonArray().add(Element3).add(Element4)))))
				.onSuccess( res2 -> {
					mongo_client.findOne("TestCollection", QueryObject, null)
					.onSuccess(res3 -> {
						System.out.println(res3.encodePrettily());
						async.complete();
					});
				});
			});
		});
		
		
	}
}
