package fi.abo.kogni.soile2.utils;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
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
		JsonObject Element5 = new JsonObject().put("class", "c4").put("value", "v3");
		JsonObject Element6 = new JsonObject().put("class", "c5").put("value", "v4").put("err", 1);
		JsonObject QueryObject = new JsonObject().put("data.value", new JsonObject().put("$eq", "v4")); 
		JsonObject basicData = new JsonObject().put("name", "test").put("data", new JsonArray().add(Element1).add(Element2));
		JsonObject basicData2 = new JsonObject().put("name", "test").put("data", new JsonArray().add(Element3).add(Element4));		
		JsonObject basicData3 = new JsonObject().put("name", "test").put("data", new JsonArray().add(Element5).add(Element6));		
		mongo_client.save("TestCollection", basicData)
		.onSuccess(id -> {
			mongo_client.save("TestCollection", basicData2)
			.onSuccess(id2 -> {
				mongo_client.save("TestCollection", basicData3)
				.onSuccess(id3 -> {
					System.out.println(createInFilter("data","value",new JsonArray().add("v1")
							.add("v3")).encodePrettily());
					JsonObject dataAddReArr = new JsonObject().put("$set", new JsonObject().put("data", new JsonObject().put("id", "$_id")));
					JsonObject dataUnset = new JsonObject().put("$unset","data.err");
					
					JsonObject dataProj = new JsonObject().put("$project", new JsonObject().put("newData", "$data")
																						   .put("_id", 0));					
					MongoAggregationHandler.aggregate(mongo_client, "TestCollection", new JsonArray().add(new JsonObject().put("$match", QueryObject))
																									 .add(createInFilter("data","value",new JsonArray().add("v1").add("v4")))
																									 .add(dataAddReArr)
																									 .add(dataUnset)
																									 .add(dataProj))
					.onSuccess(res -> {
						for(JsonObject o : res)
						{
							System.out.println(o.encodePrettily());
						}
						JsonObject pullUpdate = new JsonObject().put("$pull", new JsonObject()
								  												  .put("data", new JsonObject()
								  												  .put("value", "v3")))
								  								.put("$set", new JsonObject().put("name", "blue"));
						mongo_client.updateCollectionWithOptions("TestCollection", QueryObject, pullUpdate, new UpdateOptions().setMulti(true))
						.onSuccess(suc -> {
							mongo_client.find("TestCollection", QueryObject).
							onSuccess(list -> {
								for(JsonObject o : list)
								{
									System.out.println(o.encodePrettily());
								}
								async.complete();	
							})
							.onFailure(err -> context.fail(err));
							
						})
						.onFailure(err -> context.fail(err));
						
						
					});
				});
			});
		});


	}


	private JsonObject createFilter()
	{
		return new JsonObject().put("$project", 
				new JsonObject().put("data", new JsonObject()
						.put("$filter", new JsonObject().put("input", "$data")
								.put("as", "cdata")
								.put("cond", new JsonObject()
										.put("$in", new JsonArray().add("$$cdata.value")
												.add(new JsonArray().add("v1")
														.add("v3")))))));
	}


	private JsonObject createInFilter(String field, String subField, JsonArray subFieldIn)
	{
		return new JsonObject().put("$project", 
				new JsonObject().put(field, new JsonObject()
						.put("$filter", new JsonObject().put("input", "$" + field)
								.put("as", "currentField")
								.put("cond", new JsonObject()
										.put("$in", new JsonArray().add("$$currentField." + subField)
												.add(subFieldIn))))));
	}
}

