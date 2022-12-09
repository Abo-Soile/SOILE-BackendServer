package fi.abo.kogni.soile2.utils;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class MongoDBTests extends MongoTest{


	@Test
	public void testDataUpdate(TestContext context)
	{
		Async async = context.async();
		JsonObject Element1 = new JsonObject().put("class", "c1").put("value", "v1");
		JsonObject Element2 = new JsonObject().put("class", "c2").put("value", "v2").put("err", 2);
		JsonObject Element3 = new JsonObject().put("class", "c3").put("value", "v3");
		JsonObject Element4 = new JsonObject().put("class", "c3").put("value", "v4").put("err", 1);
		JsonObject QueryObject = new JsonObject().put("name", "test"); 
		JsonObject basicData = new JsonObject().put("name", "test").put("data", new JsonArray().add(Element1).add(Element2));
		JsonObject basicData2 = new JsonObject().put("name", "test").put("data", new JsonArray().add(Element3).add(Element4));
		mongo_client.save("TestCollection", basicData)
		.onSuccess(id -> {
			mongo_client.save("TestCollection", basicData2)
			.onSuccess(id2 -> {
			System.out.println("Basic data added");
								mongo_client.findWithOptions("TestCollection", QueryObject, new FindOptions().setFields(new JsonObject().put("_id", 1).put("data", new JsonObject().put("err", 1).put("class", 1))))
					.onSuccess(list -> {
						for(JsonObject o : list)
						{
							System.out.println(o.encodePrettily());
						}
						async.complete();
					});
			});
		});
		
		
	}
}
