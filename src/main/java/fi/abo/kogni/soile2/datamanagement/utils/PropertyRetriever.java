package fi.abo.kogni.soile2.datamanagement.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * A Retriever that obtains individual Objects from a given collection.
 * @author Thomas Pfau
 *
 */
public class PropertyRetriever implements DataRetriever<String, JsonObject> {
	
	MongoClient client;
	String collection;
	String property;
	
	public PropertyRetriever(MongoClient client, String collection, String targetProperty)
	{
		this.client = client;
		this.collection = collection;
		this.property = targetProperty;
	}
	
	@Override
	public Future<JsonObject> getElement(String UUID)
	{
		return client.findOne(collection, new JsonObject().put(property, UUID),null);
	}

	public Future<JsonObject> getElementFields(String UUID, JsonObject Fields)
	{
		return client.findOne(collection, new JsonObject().put(property, UUID),Fields);
	}

	@Override
	public void getElement(String UUID, Handler<AsyncResult<JsonObject>> handler)
	{
		client.findOne(collection, new JsonObject().put(property, UUID),null, handler);
	}

	public void getElementFields(String UUID, JsonObject Fields, Handler<AsyncResult<JsonObject>> handler)
	{
		client.findOne(collection, new JsonObject().put(property, UUID), Fields, handler);
	}

}
